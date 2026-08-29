package com.gayadi.android.ui.screens

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.gayadi.android.domain.model.InvitationStatus
import com.gayadi.android.domain.model.ExpenseSettlementSummary
import com.gayadi.android.domain.model.LOCAL_CURRENT_USER_ID
import com.gayadi.android.domain.model.ScheduleType
import com.gayadi.android.domain.model.TravelExpense
import com.gayadi.android.domain.model.TravelInvitation
import com.gayadi.android.domain.model.TravelParticipant
import com.gayadi.android.domain.model.TravelSchedule
import com.gayadi.android.domain.model.TravelState
import com.gayadi.android.domain.model.TravelTrip
import com.gayadi.android.domain.model.TripStatus
import com.gayadi.android.domain.usecase.GetTravelStateUseCase
import com.gayadi.android.domain.usecase.SaveTravelStateUseCase
import com.gayadi.android.domain.usecase.CalculateExpenseSettlementUseCase
import com.gayadi.android.domain.usecase.UpdateTravelStateUseCase
import com.gayadi.android.domain.usecase.PublishTripInviteUseCase
import com.gayadi.android.domain.usecase.ObserveSharedTripInviteUseCase
import com.gayadi.android.domain.usecase.RemoveSharedTripParticipantUseCase
import com.gayadi.android.domain.usecase.SubmitSharedTripAvailabilityUseCase
import com.gayadi.android.domain.usecase.FinalizeSharedTripDatesUseCase
import com.gayadi.android.domain.model.SharedTripInvite
import com.gayadi.android.domain.repository.AuthRepository
import com.gayadi.android.domain.repository.CreateTripCommand
import com.gayadi.android.domain.repository.DateCoordinationSnapshot
import com.gayadi.android.domain.repository.SchedulePatch
import com.gayadi.android.domain.repository.TravelGateway
import com.gayadi.android.domain.repository.UpdateTripCommand
import com.gayadi.android.domain.usecase.ValidateTravelExpenseUseCase
import java.util.UUID
import java.nio.charset.StandardCharsets
import java.util.Base64
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray

data class TravelUiState(
    val travelState: TravelState = TravelState(),
    val isLoading: Boolean = true,
    val hasLoadedTravelState: Boolean = false,
    val isSavingExpense: Boolean = false,
    val expenseErrorMessage: String? = null,
    val errorMessage: String? = null,
    val message: String? = null,
    val savedExpenseId: String? = null,
)

class TripViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val getTravelState: GetTravelStateUseCase,
    private val saveTravelState: SaveTravelStateUseCase,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val inviteCodeGenerator: () -> String = ::randomInviteCode,
    private val updateTravelState: UpdateTravelStateUseCase? = null,
    private val publishTripInvite: PublishTripInviteUseCase? = null,
    private val observeSharedTripInvite: ObserveSharedTripInviteUseCase? = null,
    private val removeSharedTripParticipant: RemoveSharedTripParticipantUseCase? = null,
    private val submitSharedTripAvailability: SubmitSharedTripAvailabilityUseCase? = null,
    private val finalizeSharedTripDates: FinalizeSharedTripDatesUseCase? = null,
    private val travelGateway: TravelGateway? = null,
    private val authRepository: AuthRepository? = null,
) : ViewModel() {
    private val persistenceMutex = Mutex()
    private val reservedInviteCodes = mutableSetOf<String>()
    private val inviteObserverJobs = mutableMapOf<String, Job>()
    private val legacyTrips = decodeLegacyTrips(savedStateHandle[LEGACY_TRIPS_KEY])
    private val _uiState = MutableStateFlow(TravelUiState())
    val uiState = _uiState.asStateFlow()
    val trips = uiState.map { state -> state.travelState.trips.map(TravelTrip::toSummary) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val selectedTripId = uiState.map { it.travelState.selectedTripId }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val localDemoParticipants = listOf(
        TravelParticipant(LOCAL_CURRENT_USER_ID, "나"),
        TravelParticipant("user-101", "여행곰", "character_pca"),
        TravelParticipant("user-102", "바다별", "character_sca"),
        TravelParticipant("user-103", "산책러", "character_snr"),
    )
    val availableParticipants: List<TravelParticipant>
        get() = if (travelGateway == null) localDemoParticipants else _uiState.value.travelState.participants

    init {
        loadState()
    }

    fun retry() = loadState()

    suspend fun publishInvite(trip: TripSummary): Result<Unit> {
        if (travelGateway != null) {
            return if (trip.inviteCode.isNotBlank()) Result.success(Unit)
            else Result.failure(IllegalStateException("서버에서 초대 코드를 발급하지 못했어요"))
        }
        val publisher = publishTripInvite
            ?: return Result.failure(IllegalStateException("여행 초대 서버를 사용할 수 없어요"))
        val state = _uiState.value.travelState
        val owner = state.participants.find { it.id == state.currentUserId }
            ?: TravelParticipant(state.currentUserId, "나")
        return publisher(trip.toDomain().copy(participantIds = listOf(owner.id), ownerId = owner.id), owner)
            .onSuccess { observeInvite(trip.inviteCode) }
    }

    suspend fun publishInvite(tripId: String): Result<Unit> {
        val trip = _uiState.value.travelState.trips.find { it.id == tripId }
            ?: return Result.failure(IllegalArgumentException("여행을 찾을 수 없어요"))
        if (travelGateway != null) {
            return if (trip.inviteCode.isNotBlank()) Result.success(Unit)
            else Result.failure(IllegalStateException("서버에서 초대 코드를 발급하지 못했어요"))
        }
        val publisher = publishTripInvite
            ?: return Result.failure(IllegalStateException("여행 초대 서버를 사용할 수 없어요"))
        val state = _uiState.value.travelState
        return publisher(trip, state.localCurrentUser()).onSuccess { observeInvite(trip.inviteCode) }
    }

    fun consumeMessage() = _uiState.update { it.copy(message = null) }

    fun consumeSavedExpense() = _uiState.update { it.copy(savedExpenseId = null) }

    fun clearExpenseError() = _uiState.update { it.copy(expenseErrorMessage = null) }

    /** Removes every locally persisted trip artifact and clears the in-memory state. */
    suspend fun clearAllTravelData(): Result<Unit> = withContext(ioDispatcher) {
        persistenceMutex.withLock {
            saveTravelState(TravelState()).onSuccess {
                reservedInviteCodes.clear()
                savedStateHandle[SELECTED_TRIP_ID_KEY] = null
                savedStateHandle[LEGACY_TRIPS_KEY] = null
                _uiState.value = TravelUiState(
                    travelState = TravelState(),
                    isLoading = false,
                    hasLoadedTravelState = true,
                )
            }
        }
    }

    suspend fun addTrip(trip: TripSummary): Result<TripSummary> {
        val gateway = travelGateway
        if (gateway != null) {
            return runCatching {
                gateway.createTrip(
                    CreateTripCommand(
                        name = trip.name,
                        startDate = trip.startDate,
                        endDate = trip.endDate,
                        cities = trip.cities,
                    ),
                ).copy(
                    coverImageResList = trip.coverImageResList,
                    isGroupTrip = trip.isGroupTrip,
                )
            }.mapCatching { created ->
                persistRemoteMutation("여행을 만들었어요") { state ->
                    state.copy(
                        trips = listOf(created) + state.trips.filterNot { it.id == created.id },
                        selectedTripId = created.id,
                    )
                }.getOrThrow()
                created.toSummary()
            }
        }
        return allocateInviteCode().map { inviteCode ->
        val savedTrip = trip.copy(inviteCode = inviteCode)
        mutate("여행을 만들었어요") { state ->
            val currentUser = state.localCurrentUser()
            state.copy(
                trips = listOf(
                    savedTrip.toDomain().copy(participantIds = listOf(state.currentUserId), ownerId = state.currentUserId),
                ) + state.trips,
                participants = (state.participants + currentUser).distinctBy(TravelParticipant::id),
            )
        }
        savedTrip
        }
    }

    /** Keeps the device-local user selectable as payer and split participant for every trip. */
    fun syncCurrentUser(nickname: String?, characterKey: String?) = mutate(null) { state ->
        val currentUser = TravelParticipant(
            id = state.currentUserId,
            nickname = nickname?.trim()?.takeIf { it.isNotBlank() } ?: "나",
            characterKey = characterKey,
        )
        state.copy(
            participants = (state.participants.filterNot { it.id == state.currentUserId } + currentUser),
            trips = state.trips.map { trip ->
                trip.copy(participantIds = (listOf(state.currentUserId) + trip.participantIds).distinct())
            },
        )
    }

    suspend fun updateTrip(trip: TripSummary): Result<TripSummary> {
        val gateway = travelGateway
        if (gateway != null) {
            val current = domainTripById(trip.id)
                ?: return Result.failure(IllegalArgumentException("여행을 찾을 수 없어요"))
            return runCatching {
                gateway.updateTrip(
                    trip.id,
                    UpdateTripCommand(
                        name = trip.name,
                        startDate = trip.startDate,
                        endDate = trip.endDate,
                        cities = trip.cities,
                        version = current.version,
                    ),
                ).copy(
                    coverImageResList = trip.coverImageResList,
                    isGroupTrip = trip.isGroupTrip,
                    dateAvailability = current.dateAvailability,
                )
            }.mapCatching { updated ->
                persistRemoteMutation("여행 정보를 수정했어요") { state ->
                    state.copy(trips = state.trips.map { if (it.id == updated.id) updated else it })
                }.getOrThrow()
                updated.toSummary()
            }
        }
        mutate("여행 정보를 수정했어요") { state ->
            state.copy(trips = state.trips.map { if (it.id == trip.id) trip.toDomain(it) else it })
        }
        return Result.success(trip)
    }

    fun deleteTrip(tripId: String) {
        val remove: (TravelState) -> TravelState = { state ->
            state.copy(
                trips = state.trips.filterNot { it.id == tripId },
                invitations = state.invitations.filterNot { it.tripId == tripId },
                schedules = state.schedules.filterNot { it.tripId == tripId },
                expenses = state.expenses.filterNot { it.tripId == tripId },
                sharedFundAmounts = state.sharedFundAmounts - tripId,
                appliedRouteIds = state.appliedRouteIds.filterKeys { !it.startsWith("$tripId:") },
                selectedTripId = state.selectedTripId.takeUnless { it == tripId },
            )
        }
        val gateway = travelGateway
        if (gateway == null) {
            mutate("여행을 삭제했어요", remove)
        } else {
            viewModelScope.launch(ioDispatcher) {
                runCatching { gateway.deleteTrip(tripId) }.fold(
                    onSuccess = { persistRemoteMutation("여행을 삭제했어요", remove) },
                    onFailure = ::showTravelError,
                )
            }
        }
    }

    fun selectTrip(tripId: String) = mutate(null) { state ->
        if (state.trips.none { it.id == tripId }) state else state.copy(selectedTripId = tripId)
    }

    fun startTrip(tripId: String) = updateTripStatus(tripId, TripStatus.ONGOING, "여행을 시작했어요")

    fun finishTrip(tripId: String) = updateTripStatus(tripId, TripStatus.COMPLETED, "여행을 종료했어요")

    fun addParticipant(tripId: String, participantId: String) {
        val gateway = travelGateway
        if (gateway != null) {
            viewModelScope.launch(ioDispatcher) {
                runCatching { gateway.addParticipant(tripId, participantId) }.fold(
                    onSuccess = { participant ->
                        persistRemoteMutation("참여자를 추가했어요") { state ->
                            state.copy(
                                participants = (state.participants + participant).distinctBy(TravelParticipant::id),
                                trips = state.trips.map { trip ->
                                    if (trip.id == tripId) {
                                        trip.copy(participantIds = (trip.participantIds + participant.id).distinct())
                                    } else trip
                                },
                            )
                        }
                    },
                    onFailure = ::showTravelError,
                )
            }
            return
        }
        mutate("참여자를 추가했어요") { state ->
            val participant = availableParticipants.find { it.id == participantId } ?: return@mutate state
            state.copy(
                participants = (state.participants + participant).distinctBy(TravelParticipant::id),
                trips = state.trips.map { trip ->
                    if (trip.id == tripId) trip.copy(participantIds = (trip.participantIds + participantId).distinct())
                    else trip
                },
            )
        }
    }

    fun removeParticipant(tripId: String, participantId: String) {
        val state = _uiState.value.travelState
        val trip = state.trips.find { it.id == tripId }
        when {
            trip == null -> {
                _uiState.update { it.copy(message = "여행을 찾을 수 없어요") }
            }
            trip.ownerId.isNotBlank() && trip.ownerId != state.currentUserId -> {
                _uiState.update { it.copy(message = "방장만 참여자를 내보낼 수 있어요") }
            }
            participantId == state.currentUserId -> {
                _uiState.update { it.copy(message = "본인은 여행에서 제외할 수 없어요") }
            }
            state.expenses.any { expense ->
                expense.tripId == tripId &&
                    (expense.payerId == participantId || participantId in expense.participantIds)
            } -> {
                _uiState.update { it.copy(message = "비용 내역에 포함된 참여자는 내보낼 수 없어요") }
            }
            else -> {
                val gateway = travelGateway
                if (gateway != null) {
                    viewModelScope.launch(ioDispatcher) {
                        runCatching { gateway.removeParticipant(tripId, participantId) }.fold(
                            onSuccess = {
                                persistRemoteMutation("참여자를 내보냈어요") { current ->
                                    current.updateTrip(tripId) {
                                        it.copy(
                                            participantIds = it.participantIds - participantId,
                                            dateAvailability = it.dateAvailability - participantId,
                                        )
                                    }.copy(
                                        participants = current.participants.filterNot { participant ->
                                            participant.id == participantId && current.trips.none { other ->
                                                other.id != tripId && participant.id in other.participantIds
                                            }
                                        },
                                    )
                                }
                            },
                            onFailure = ::showTravelError,
                        )
                    }
                    return
                }
                val remoteRemover = removeSharedTripParticipant
                if (remoteRemover != null && trip.inviteCode.length == INVITE_CODE_LENGTH) {
                    viewModelScope.launch(ioDispatcher) {
                        remoteRemover(trip.inviteCode, participantId).fold(
                            onSuccess = { removeParticipantLocally(tripId, participantId) },
                            onFailure = ::showInviteError,
                        )
                    }
                } else {
                    removeParticipantLocally(tripId, participantId)
                }
            }
        }
    }

    private fun removeParticipantLocally(tripId: String, participantId: String) {
        mutate("참여자를 내보냈어요") { current ->
            require(participantId != current.currentUserId) { "본인은 여행에서 제외할 수 없어요" }
            require(
                current.expenses.none { expense ->
                    expense.tripId == tripId &&
                        (expense.payerId == participantId || participantId in expense.participantIds)
                },
            ) { "비용 내역에 포함된 참여자는 내보낼 수 없어요" }
            current.updateTrip(tripId) {
                it.copy(
                    participantIds = it.participantIds - participantId,
                    dateAvailability = it.dateAvailability - participantId,
                )
            }
        }
    }

    fun submitDateAvailability(tripId: String, participantId: String, dates: List<String>) {
        val normalizedDates = dates.distinct().sorted()
        val gateway = travelGateway
        if (gateway != null) {
            if (participantId != _uiState.value.travelState.currentUserId) {
                showTravelError(IllegalArgumentException("다른 참여자의 가능 날짜는 대신 저장할 수 없어요"))
                return
            }
            viewModelScope.launch(ioDispatcher) {
                runCatching { gateway.submitDateAvailability(tripId, normalizedDates) }.fold(
                    onSuccess = { applyDateCoordination(it, "가능한 날짜를 저장했어요") },
                    onFailure = ::showTravelError,
                )
            }
            return
        }
        val trip = _uiState.value.travelState.trips.find { it.id == tripId }
        mutate("가능한 날짜를 저장했어요") { state ->
            state.updateTrip(tripId) { trip ->
                trip.copy(dateAvailability = trip.dateAvailability + (participantId to normalizedDates))
            }
        }
        if (participantId == _uiState.value.travelState.currentUserId && trip?.inviteCode?.length == INVITE_CODE_LENGTH) {
            viewModelScope.launch(ioDispatcher) {
                submitSharedTripAvailability?.invoke(trip.inviteCode, normalizedDates)?.onFailure(::showInviteError)
            }
        }
    }

    fun finalizeGroupTripDates(tripId: String, startDate: String, endDate: String) {
        val gateway = travelGateway
        if (gateway != null) {
            viewModelScope.launch(ioDispatcher) {
                runCatching { gateway.finalizeTripDates(tripId, startDate, endDate) }.fold(
                    onSuccess = { applyDateCoordination(it, "여행 날짜를 확정했어요") },
                    onFailure = ::showTravelError,
                )
            }
            return
        }
        val trip = _uiState.value.travelState.trips.find { it.id == tripId }
        mutate("여행 날짜를 확정했어요") { state ->
            state.updateTrip(tripId) { it.copy(startDate = startDate, endDate = endDate) }
        }
        if (trip?.inviteCode?.length == INVITE_CODE_LENGTH) {
            viewModelScope.launch(ioDispatcher) {
                finalizeSharedTripDates?.invoke(trip.inviteCode, startDate, endDate)?.onFailure(::showInviteError)
            }
        }
    }

    fun createInvitation(tripId: String, inviteeId: String): String {
        if (travelGateway != null) {
            return domainTripById(tripId)?.inviteCode.orEmpty()
        }
        val invitation = TravelInvitation(
            id = UUID.randomUUID().toString(),
            tripId = tripId,
            code = UUID.randomUUID().toString().replace("-", "").take(8).uppercase(),
            inviteeId = inviteeId,
        )
        mutate("초대 코드를 만들었어요") { state ->
            state.copy(invitations = state.invitations.filterNot { it.tripId == tripId } + invitation)
        }
        return invitation.code
    }

    fun acceptInvitation(invitationId: String) = decideInvitation(invitationId, InvitationStatus.ACCEPTED)

    fun declineInvitation(invitationId: String) = decideInvitation(invitationId, InvitationStatus.DECLINED)

    fun cancelInvitation(invitationId: String) = decideInvitation(invitationId, InvitationStatus.CANCELLED)

    fun joinByCode(code: String) {
        val gateway = travelGateway
        if (gateway != null) {
            viewModelScope.launch(ioDispatcher) {
                runCatching { gateway.joinTrip(code) }.fold(
                    onSuccess = { membership ->
                        persistRemoteMutation("${membership.trip.name} 여행에 참여했어요") { state ->
                            state.copy(
                                trips = state.trips.filterNot { it.id == membership.trip.id } + membership.trip,
                                participants = (state.participants + membership.participant)
                                    .distinctBy(TravelParticipant::id),
                                selectedTripId = membership.trip.id,
                            )
                        }
                        loadState()
                    },
                    onFailure = ::showTravelError,
                )
            }
            return
        }
        val invitation = _uiState.value.travelState.invitations.find {
            it.code.equals(code.trim(), ignoreCase = true) && it.status == InvitationStatus.PENDING
        }
        if (invitation == null) {
            _uiState.update { it.copy(message = "유효한 초대 코드를 찾지 못했어요") }
        } else {
            acceptInvitation(invitation.id)
        }
    }

    fun upsertSchedule(schedule: TravelSchedule) {
        val gateway = travelGateway
        if (gateway != null) {
            viewModelScope.launch(ioDispatcher) {
                val request = runCatching {
                    if (schedule.id.toLongOrNull() == null) {
                        gateway.createSchedule(schedule.tripId, schedule)
                    } else {
                        gateway.updateSchedule(
                            schedule.tripId,
                            schedule.id,
                            SchedulePatch(
                                title = schedule.title,
                                date = schedule.date,
                                time = schedule.time,
                                endTime = schedule.endTime,
                                clearEndTime = schedule.endTime == null,
                                memo = schedule.memo,
                                type = schedule.type,
                                placeId = schedule.placeId,
                                clearPlaceId = schedule.placeId == null,
                                isVisited = schedule.isVisited,
                            ),
                        )
                    }
                }
                request.fold(
                    onSuccess = { saved ->
                        persistRemoteMutation("일정을 저장했어요") { state ->
                            val withoutDraft = state.schedules.filterNot {
                                it.id == schedule.id || it.id == saved.id
                            }
                            state.copy(schedules = (withoutDraft + saved).normalizeOrders(saved.tripId))
                        }
                    },
                    onFailure = ::showTravelError,
                )
            }
            return
        }
        mutate("일정을 저장했어요") { state ->
            val current = state.schedules.filterNot { it.id == schedule.id }
            state.copy(schedules = (current + schedule).normalizeOrders(schedule.tripId))
        }
    }

    fun addPlaceSchedule(tripId: String, placeId: String, title: String, time: String, memo: String) {
        val trip = _uiState.value.travelState.trips.find { it.id == tripId } ?: return
        val order = schedulesForTrip(tripId).size
        upsertSchedule(
            TravelSchedule(
                id = UUID.randomUUID().toString(),
                tripId = tripId,
                title = title,
                placeId = placeId,
                date = trip.startDate,
                time = time,
                order = order,
                memo = memo,
            ),
        )
    }

    fun deleteSchedule(scheduleId: String) {
        val gateway = travelGateway
        val tripId = _uiState.value.travelState.schedules.find { it.id == scheduleId }?.tripId ?: return
        if (gateway == null) {
            mutate("일정을 삭제했어요") { state ->
                state.copy(
                    schedules = state.schedules.filterNot { it.id == scheduleId }.normalizeOrders(tripId),
                    expenses = state.expenses.filterNot { it.scheduleId == scheduleId },
                )
            }
        } else {
            viewModelScope.launch(ioDispatcher) {
                runCatching { gateway.deleteSchedule(tripId, scheduleId) }.fold(
                    onSuccess = {
                        persistRemoteMutation("일정을 삭제했어요") { state ->
                            state.copy(
                                schedules = state.schedules.filterNot { it.id == scheduleId }.normalizeOrders(tripId),
                                expenses = state.expenses.filterNot { it.scheduleId == scheduleId },
                            )
                        }
                    },
                    onFailure = ::showTravelError,
                )
            }
        }
    }

    fun saveExpense(expense: TravelExpense) {
        if (_uiState.value.isSavingExpense) return
        _uiState.update { it.copy(isSavingExpense = true, expenseErrorMessage = null) }
        viewModelScope.launch(ioDispatcher) { persistExpense(expense) }
    }

    internal suspend fun upsertExpense(expense: TravelExpense): Result<Unit> = persistExpense(expense)

    private suspend fun persistExpense(expense: TravelExpense): Result<Unit> {
        if (!_uiState.value.isSavingExpense) {
            _uiState.update { it.copy(isSavingExpense = true, expenseErrorMessage = null) }
        }
        travelGateway?.let { gateway -> return persistRemoteExpense(gateway, expense) }
        val persistedResult = try {
            persistenceMutex.withLock {
                val transform: (TravelState) -> TravelState = { state -> state.withValidatedExpense(expense) }
                updateTravelState?.invoke(transform) ?: run {
                    runCatching { transform(_uiState.value.travelState) }.fold(
                        onSuccess = { candidate -> saveTravelState(candidate).map { candidate } },
                        onFailure = { Result.failure(it) },
                    )
                }
            }
        } catch (cancellation: kotlinx.coroutines.CancellationException) {
            _uiState.update { it.copy(isSavingExpense = false) }
            throw cancellation
        } catch (error: Throwable) {
            Result.failure(error)
        }
        persistedResult.fold(
            onSuccess = { persisted ->
                savedStateHandle[SELECTED_TRIP_ID_KEY] = persisted.selectedTripId
                _uiState.update {
                    it.copy(
                        travelState = persisted,
                        isSavingExpense = false,
                        message = "비용을 저장했어요",
                        savedExpenseId = expense.id,
                        expenseErrorMessage = null,
                    )
                }
            },
            onFailure = { error ->
                _uiState.update {
                    it.copy(
                        isSavingExpense = false,
                        expenseErrorMessage = error.message ?: "비용 정보를 저장하지 못했어요",
                    )
                }
            },
        )
        return persistedResult.map { Unit }
    }

    fun deleteExpense(expenseId: String) {
        val gateway = travelGateway
        if (gateway == null) {
            mutate("비용을 삭제했어요") { state ->
                state.copy(expenses = state.expenses.filterNot { it.id == expenseId })
            }
            return
        }
        val expense = expenseById(expenseId) ?: return
        viewModelScope.launch(ioDispatcher) {
            runCatching { gateway.deleteExpense(expense.tripId, expenseId) }.fold(
                onSuccess = {
                    persistRemoteMutation("비용을 삭제했어요") { state ->
                        state.copy(expenses = state.expenses.filterNot { it.id == expenseId })
                    }
                },
                onFailure = ::showTravelError,
            )
        }
    }

    fun addSharedFund(tripId: String, amount: Long) {
        val gateway = travelGateway
        if (gateway == null) {
            mutate("공동 경비를 설정했어요") { state ->
                require(amount > 0L) { "공동 경비 금액을 입력해 주세요" }
                state.copy(sharedFundAmounts = state.sharedFundAmounts + (tripId to amount))
            }
            return
        }
        viewModelScope.launch(ioDispatcher) {
            runCatching { gateway.contributeSharedFund(tripId, amount) }.fold(
                onSuccess = { fund ->
                    persistRemoteMutation("공동 경비를 충전했어요") { state ->
                        state.copy(sharedFundAmounts = state.sharedFundAmounts + (tripId to fund.contributedAmount))
                    }
                },
                onFailure = ::showTravelError,
            )
        }
    }

    fun sharedFundBalanceForTrip(tripId: String): Long {
        val deposited = _uiState.value.travelState.sharedFundAmounts[tripId] ?: 0L
        val spent = expensesForTrip(tripId)
            .filter { it.paymentSource == com.gayadi.android.domain.model.ExpensePaymentSource.SHARED_FUND }
            .sumOf(TravelExpense::amount)
        return deposited - spent
    }

    fun expenseById(expenseId: String): TravelExpense? =
        _uiState.value.travelState.expenses.find { it.id == expenseId }

    fun expensesForTrip(tripId: String): List<TravelExpense> =
        _uiState.value.travelState.expenses.filter { it.tripId == tripId }
            .sortedWith(compareBy(TravelExpense::date, TravelExpense::time, TravelExpense::id))

    fun settlementForTrip(tripId: String): Result<ExpenseSettlementSummary> =
        CalculateExpenseSettlementUseCase()(
            expensesForTrip(tripId),
            _uiState.value.travelState.participantIdsForTrip(tripId),
        )

    fun moveSchedule(scheduleId: String, direction: Int) {
        val gateway = travelGateway
        if (gateway == null) {
            mutate(null) { current ->
                val schedule = current.schedules.find { it.id == scheduleId } ?: return@mutate current
                val tripSchedules = current.schedules.filter { it.tripId == schedule.tripId }
                    .sortedBy { it.order }.toMutableList()
                val from = tripSchedules.indexOfFirst { it.id == scheduleId }
                val to = (from + direction).coerceIn(0, tripSchedules.lastIndex)
                if (from == to) return@mutate current
                val moved = tripSchedules.removeAt(from)
                tripSchedules.add(to, moved)
                val reordered = tripSchedules.mapIndexed { index, item -> item.copy(order = index) }
                current.copy(schedules = current.schedules.filterNot { it.tripId == schedule.tripId } + reordered)
            }
        } else {
            val state = _uiState.value.travelState
            val schedule = state.schedules.find { it.id == scheduleId } ?: return
            val tripSchedules = state.schedules.filter { it.tripId == schedule.tripId }
                .sortedBy { it.order }.toMutableList()
            val from = tripSchedules.indexOfFirst { it.id == scheduleId }
            val to = (from + direction).coerceIn(0, tripSchedules.lastIndex)
            if (from == to) return
            val moved = tripSchedules.removeAt(from)
            tripSchedules.add(to, moved)
            val reordered = tripSchedules.mapIndexed { index, item -> item.copy(order = index) }
            viewModelScope.launch(ioDispatcher) {
                runCatching { gateway.reorderSchedules(schedule.tripId, reordered.map(TravelSchedule::id)) }.fold(
                    onSuccess = { saved ->
                        persistRemoteMutation(null) { current ->
                            current.copy(schedules = current.schedules.filterNot { it.tripId == schedule.tripId } + saved)
                        }
                    },
                    onFailure = ::showTravelError,
                )
            }
        }
    }

    fun toggleVisited(scheduleId: String) {
        val gateway = travelGateway
        if (gateway == null) {
            mutate(null) { state ->
                state.copy(schedules = state.schedules.map {
                    if (it.id == scheduleId) it.copy(isVisited = !it.isVisited) else it
                })
            }
        } else {
            val schedule = _uiState.value.travelState.schedules.find { it.id == scheduleId } ?: return
            viewModelScope.launch(ioDispatcher) {
                runCatching {
                    gateway.updateSchedule(
                        schedule.tripId,
                        schedule.id,
                        SchedulePatch(isVisited = !schedule.isVisited),
                    )
                }.fold(
                    onSuccess = { saved ->
                        persistRemoteMutation(null) { state ->
                            state.copy(schedules = state.schedules.map { if (it.id == saved.id) saved else it })
                        }
                    },
                    onFailure = ::showTravelError,
                )
            }
        }
    }

    fun toggleFavorite(placeId: String) {
        val gateway = travelGateway
        if (gateway == null) {
            mutate(null) { state ->
                val favorites = state.favoritePlaceIds
                state.copy(favoritePlaceIds = if (placeId in favorites) favorites - placeId else favorites + placeId)
            }
            return
        }
        val shouldSave = placeId !in _uiState.value.travelState.favoritePlaceIds
        viewModelScope.launch(ioDispatcher) {
            runCatching {
                if (shouldSave) gateway.saveFavoritePlace(placeId) else gateway.deleteFavoritePlace(placeId)
            }.fold(
                onSuccess = {
                    persistRemoteMutation(null) { state ->
                        state.copy(
                            favoritePlaceIds = if (shouldSave) {
                                state.favoritePlaceIds + placeId
                            } else {
                                state.favoritePlaceIds - placeId
                            },
                        )
                    }
                },
                onFailure = ::showTravelError,
            )
        }
    }

    fun applyRoute(tripId: String, routeType: String, optionId: String) = mutate("추천 경로를 적용했어요") { state ->
        if (state.trips.none { it.id == tripId }) state
        else state.copy(appliedRouteIds = state.appliedRouteIds + (routeKey(tripId, routeType) to optionId))
    }

    fun appliedRouteId(tripId: String, routeType: String): String? =
        _uiState.value.travelState.appliedRouteIds[routeKey(tripId, routeType)]

    fun tripById(tripId: String): TripSummary? =
        _uiState.value.travelState.trips.find { it.id == tripId }?.toSummary()

    fun domainTripById(tripId: String): TravelTrip? =
        _uiState.value.travelState.trips.find { it.id == tripId }

    fun participantsForTrip(tripId: String): List<TravelParticipant> {
        val ids = domainTripById(tripId)?.participantIds.orEmpty()
        return (_uiState.value.travelState.participants + availableParticipants)
            .distinctBy(TravelParticipant::id).filter { it.id in ids }
    }

    fun invitationForTrip(tripId: String): TravelInvitation? =
        _uiState.value.travelState.invitations.find { it.tripId == tripId }

    fun schedulesForTrip(tripId: String): List<TravelSchedule> =
        _uiState.value.travelState.schedules.filter { it.tripId == tripId }.sortedBy { it.order }

    fun isFavorite(placeId: String): Boolean = placeId in _uiState.value.travelState.favoritePlaceIds

    private fun decideInvitation(invitationId: String, decision: InvitationStatus) = mutate(
        when (decision) {
            InvitationStatus.ACCEPTED -> "초대를 수락했어요"
            InvitationStatus.DECLINED -> "초대를 거절했어요"
            InvitationStatus.CANCELLED -> "초대를 취소했어요"
            InvitationStatus.PENDING -> null
        },
    ) { state ->
        val invitation = state.invitations.find { it.id == invitationId } ?: return@mutate state
        if (invitation.status != InvitationStatus.PENDING) return@mutate state
        val updated = state.copy(invitations = state.invitations.map {
            if (it.id == invitationId) it.copy(status = decision) else it
        })
        if (decision != InvitationStatus.ACCEPTED) updated else {
            val participant = availableParticipants.find { it.id == invitation.inviteeId }
                ?: TravelParticipant(invitation.inviteeId, invitation.inviteeId)
            updated.copy(
                participants = (updated.participants + participant).distinctBy(TravelParticipant::id),
                trips = updated.trips.map { trip ->
                    if (trip.id == invitation.tripId) {
                        trip.copy(participantIds = (trip.participantIds + invitation.inviteeId).distinct())
                    } else trip
                },
            )
        }
    }

    private suspend fun persistRemoteExpense(
        gateway: TravelGateway,
        expense: TravelExpense,
    ): Result<Unit> {
        val result = try {
            val current = _uiState.value.travelState
            current.withValidatedExpense(expense)
            val saved = if (expense.id.toLongOrNull() != null && current.expenses.any { it.id == expense.id }) {
                gateway.updateExpense(expense.tripId, expense)
            } else {
                gateway.createExpense(expense.tripId, expense)
            }
            persistRemoteMutation("비용을 저장했어요") { state ->
                state.copy(expenses = state.expenses.filterNot { it.id == expense.id || it.id == saved.id } + saved)
            }.getOrThrow()
            Result.success(saved)
        } catch (cancellation: kotlinx.coroutines.CancellationException) {
            _uiState.update { it.copy(isSavingExpense = false) }
            throw cancellation
        } catch (error: Throwable) {
            Result.failure(error)
        }
        result.fold(
            onSuccess = { saved ->
                _uiState.update {
                    it.copy(
                        isSavingExpense = false,
                        savedExpenseId = saved.id,
                        expenseErrorMessage = null,
                    )
                }
            },
            onFailure = { error ->
                _uiState.update {
                    it.copy(
                        isSavingExpense = false,
                        expenseErrorMessage = error.message ?: "비용 정보를 저장하지 못했어요",
                    )
                }
            },
        )
        return result.map { Unit }
    }

    private fun updateTripStatus(tripId: String, status: TripStatus, message: String) {
        val gateway = travelGateway
        if (gateway == null) {
            mutate(message) { state ->
                state.updateTrip(tripId) { trip ->
                    if (
                        status == TripStatus.ONGOING && trip.status == TripStatus.PLANNING ||
                        status == TripStatus.COMPLETED && trip.status == TripStatus.ONGOING
                    ) trip.copy(status = status) else trip
                }
            }
            return
        }
        val current = domainTripById(tripId) ?: return
        if (current.status == status) return
        viewModelScope.launch(ioDispatcher) {
            runCatching { gateway.updateTripStatus(tripId, status) }.fold(
                onSuccess = { updated ->
                    persistRemoteMutation(message) { state ->
                        state.copy(trips = state.trips.map { trip ->
                            if (trip.id == updated.id) {
                                updated.copy(
                                    coverImageResList = trip.coverImageResList,
                                    isGroupTrip = trip.isGroupTrip,
                                    dateAvailability = trip.dateAvailability,
                                )
                            } else trip
                        })
                    }
                },
                onFailure = ::showTravelError,
            )
        }
    }

    private suspend fun applyDateCoordination(snapshot: DateCoordinationSnapshot, message: String) {
        val availability = snapshot.participants
            .filter { it.submitted }
            .associate { it.participant.id to it.dates }
        persistRemoteMutation(message) { state ->
            state.copy(
                trips = state.trips.map { trip ->
                    if (trip.id == snapshot.tripId) {
                        trip.copy(
                            startDate = snapshot.startDate,
                            endDate = snapshot.endDate,
                            dateAvailability = availability,
                            version = snapshot.tripVersion,
                        )
                    } else trip
                },
                participants = (state.participants + snapshot.participants.map { it.participant })
                    .distinctBy(TravelParticipant::id),
            )
        }
    }

    private suspend fun persistRemoteMutation(
        message: String?,
        transform: (TravelState) -> TravelState,
    ): Result<TravelState> = persistenceMutex.withLock {
        val result = runCatching { transform(_uiState.value.travelState) }.fold(
            onSuccess = { candidate -> saveTravelState(candidate).map { candidate } },
            onFailure = { Result.failure(it) },
        )
        result.onSuccess { persisted ->
            savedStateHandle[SELECTED_TRIP_ID_KEY] = persisted.selectedTripId
            _uiState.update {
                it.copy(travelState = persisted, message = message, errorMessage = null)
            }
        }.onFailure(::showTravelError)
        result
    }

    private fun showTravelError(error: Throwable) {
        _uiState.update { it.copy(errorMessage = error.message ?: "여행 정보를 동기화하지 못했어요") }
    }

    private suspend fun loadRemoteState(cached: TravelState): TravelState {
        val gateway = requireNotNull(travelGateway)
        val currentUserId = authRepository?.current()?.getOrThrow()?.id?.toString()
            ?: throw IllegalStateException("로그인 사용자 정보를 확인할 수 없어요")
        val trips = buildList {
            var offset = 0
            do {
                val page = gateway.listTrips(limit = REMOTE_PAGE_SIZE, offset = offset)
                addAll(page)
                offset += page.size
            } while (page.size == REMOTE_PAGE_SIZE)
        }
        val bundles = coroutineScope {
            trips.map { remoteTrip ->
                async {
                    val participants = gateway.listParticipants(remoteTrip.id)
                    val coordination = runCatching { gateway.getDateCoordination(remoteTrip.id) }.getOrNull()
                    val cachedTrip = cached.trips.find { it.id == remoteTrip.id }
                    val trip = remoteTrip.copy(
                        coverImageResList = cachedTrip?.coverImageResList
                            ?: cityCoverImageResources(remoteTrip.cities),
                        isGroupTrip = cachedTrip?.isGroupTrip ?: (participants.size > 1),
                        dateAvailability = coordination?.participants
                            ?.filter { it.submitted }
                            ?.associate { it.participant.id to it.dates }
                            .orEmpty(),
                        version = coordination?.tripVersion ?: remoteTrip.version,
                    )
                    RemoteTripBundle(
                        trip = trip,
                        participants = participants,
                        invitations = runCatching { gateway.listInvitations(remoteTrip.id, limit = 100) }
                            .getOrDefault(emptyList()),
                        schedules = gateway.listSchedules(remoteTrip.id),
                        expenses = gateway.listExpenses(remoteTrip.id),
                        contributedAmount = runCatching { gateway.getSharedFund(remoteTrip.id).contributedAmount }
                            .getOrDefault(0L),
                    )
                }
            }.awaitAll()
        }
        val tripIds = bundles.map { it.trip.id }.toSet()
        return TravelState(
            trips = bundles.map { it.trip },
            participants = bundles.flatMap { it.participants }.distinctBy(TravelParticipant::id),
            invitations = bundles.flatMap { it.invitations },
            schedules = bundles.flatMap { it.schedules },
            favoritePlaceIds = gateway.listFavoritePlaceIds(),
            appliedRouteIds = cached.appliedRouteIds.filterKeys { key -> tripIds.any { key.startsWith("$it:") } },
            selectedTripId = cached.selectedTripId?.takeIf { it in tripIds },
            expenses = bundles.flatMap { it.expenses },
            sharedFundAmounts = bundles.associate { it.trip.id to it.contributedAmount },
            currentUserId = currentUserId,
        )
    }

    private fun loadState() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch(ioDispatcher) {
            getTravelState().fold(
                onSuccess = { state ->
                    val restoredResult = if (travelGateway == null) {
                        Result.success(
                            if (state.trips.isEmpty() && legacyTrips.isNotEmpty()) {
                                state.copy(trips = legacyTrips.map(TripSummary::toDomain))
                            } else state,
                        )
                    } else {
                        runCatching { loadRemoteState(state) }
                    }
                    restoredResult.fold(onSuccess = { restored ->
                    savedStateHandle[SELECTED_TRIP_ID_KEY] = restored.selectedTripId
                    _uiState.value = TravelUiState(
                        travelState = restored,
                        isLoading = false,
                        hasLoadedTravelState = true,
                    )
                    if (travelGateway == null) publishTripInvite?.let { publisher ->
                        val owner = restored.localCurrentUser()
                        restored.trips.filter {
                            it.inviteCode.length == INVITE_CODE_LENGTH &&
                                (it.ownerId.isBlank() || it.ownerId == restored.currentUserId)
                        }.forEach { trip ->
                            publisher(trip, owner)
                        }
                    }
                    restartInviteObservers(restored)
                    if (restored !== state) persistLatest()
                    }, onFailure = { error ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                hasLoadedTravelState = false,
                                errorMessage = error.message ?: "여행 정보를 불러오지 못했어요",
                            )
                        }
                    })
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = error.message ?: "여행 정보를 불러오지 못했어요")
                    }
                },
            )
        }
    }

    private fun mutate(message: String?, transform: (TravelState) -> TravelState) {
        viewModelScope.launch(ioDispatcher) {
            persistenceMutex.withLock {
                val result = updateTravelState?.invoke(transform) ?: run {
                    runCatching { transform(_uiState.value.travelState) }.fold(
                        onSuccess = { candidate -> saveTravelState(candidate).map { candidate } },
                        onFailure = { Result.failure(it) },
                    )
                }
                result.fold(
                    onSuccess = { persisted ->
                        val candidate = persisted
                        savedStateHandle[SELECTED_TRIP_ID_KEY] = candidate.selectedTripId
                        _uiState.update {
                            it.copy(travelState = candidate, message = message, errorMessage = null)
                        }
                    },
                    onFailure = { error ->
                        _uiState.update {
                            it.copy(errorMessage = error.message ?: "여행 정보를 저장하지 못했어요")
                        }
                    },
                )
            }
        }
    }

    private suspend fun persistLatest() = persistenceMutex.withLock {
        saveTravelState(_uiState.value.travelState).onFailure { error ->
            _uiState.update { it.copy(errorMessage = error.message ?: "여행 정보를 저장하지 못했어요") }
        }
    }

    private fun restartInviteObservers(state: TravelState) {
        inviteObserverJobs.values.forEach(Job::cancel)
        inviteObserverJobs.clear()
        if (travelGateway != null) return
        state.trips.map(TravelTrip::inviteCode).filter { it.length == INVITE_CODE_LENGTH }.forEach(::observeInvite)
    }

    private fun observeInvite(inviteCode: String) {
        val observer = observeSharedTripInvite ?: return
        if (inviteCode.length != INVITE_CODE_LENGTH || inviteObserverJobs[inviteCode]?.isActive == true) return
        inviteObserverJobs[inviteCode] = viewModelScope.launch(ioDispatcher) {
            observer(inviteCode).collect { result ->
                result.fold(onSuccess = { mergeSharedInvite(it) }, onFailure = ::showInviteError)
            }
        }
    }

    private suspend fun mergeSharedInvite(shared: SharedTripInvite) {
        persistenceMutex.withLock {
            val transform: (TravelState) -> TravelState = { state ->
                val existing = state.trips.find { it.id == shared.trip.id }
                val localMockIds = existing?.participantIds.orEmpty().filter { participantId ->
                    participantId != state.currentUserId && availableParticipants.any { it.id == participantId }
                }
                val localMockAvailability = existing?.dateAvailability.orEmpty().filterKeys { it in localMockIds }
                val mergedTrip = shared.trip.copy(
                    coverImageResList = existing?.coverImageResList.orEmpty(),
                    participantIds = (shared.trip.participantIds + localMockIds).distinct(),
                    dateAvailability = localMockAvailability + shared.trip.dateAvailability,
                )
                state.copy(
                    trips = state.trips.filterNot { it.id == mergedTrip.id } + mergedTrip,
                    participants = (state.participants + shared.participants).distinctBy(TravelParticipant::id),
                )
            }
            val result = updateTravelState?.invoke(transform) ?: run {
                val candidate = transform(_uiState.value.travelState)
                saveTravelState(candidate).map { candidate }
            }
            result.onSuccess { persisted ->
                _uiState.update { it.copy(travelState = persisted, errorMessage = null) }
            }.onFailure(::showInviteError)
        }
    }

    private fun showInviteError(error: Throwable) {
        _uiState.update { it.copy(errorMessage = error.message ?: "여행 초대 동기화에 실패했어요") }
    }

    private data class RemoteTripBundle(
        val trip: TravelTrip,
        val participants: List<TravelParticipant>,
        val invitations: List<TravelInvitation>,
        val schedules: List<TravelSchedule>,
        val expenses: List<TravelExpense>,
        val contributedAmount: Long,
    )

    private fun allocateInviteCode(): Result<String> = synchronized(reservedInviteCodes) {
        _uiState.value.travelState.trips.mapTo(reservedInviteCodes) { it.inviteCode }
        repeat(MAX_INVITE_CODE_ATTEMPTS) {
            val candidate = normalizeInviteCode(inviteCodeGenerator())
            if (candidate.length == INVITE_CODE_LENGTH && reservedInviteCodes.add(candidate)) {
                return@synchronized Result.success(candidate)
            }
        }
        Result.failure(IllegalStateException("초대 코드를 만들지 못했어요. 다시 시도해 주세요"))
    }

    private fun decodeLegacyTrips(value: String?): List<TripSummary> {
        if (value.isNullOrBlank()) return emptyList()
        return runCatching {
            if (value.trimStart().startsWith("[")) {
                val array = JSONArray(value)
                List(array.length()) { index ->
                    val item = array.getJSONObject(index)
                    TripSummary(
                        id = item.optString("id").ifBlank { UUID.randomUUID().toString() },
                        name = item.getString("name"),
                        startDate = item.getString("startDate"),
                        endDate = item.getString("endDate"),
                        cities = item.getJSONArray("cities").let { cities -> List(cities.length(), cities::getString) },
                        coverImageResList = item.getJSONArray("coverImageResList")
                            .let { images -> List(images.length(), images::getInt) },
                    )
                }
            } else {
                value.lineSequence().filter(String::isNotBlank).map { record ->
                    val fields = record.split('|').map { encoded ->
                        String(Base64.getUrlDecoder().decode(encoded), StandardCharsets.UTF_8)
                    }
                    require(fields.size == 6)
                    TripSummary(
                        id = fields[0],
                        name = fields[1],
                        startDate = fields[2],
                        endDate = fields[3],
                        cities = fields[4].split("\u001F").filter(String::isNotBlank),
                        coverImageResList = fields[5].split("\u001F").mapNotNull(String::toIntOrNull),
                    )
                }.toList()
            }
        }.getOrDefault(emptyList())
    }

    private fun TravelState.updateTrip(tripId: String, transform: (TravelTrip) -> TravelTrip) =
        copy(trips = trips.map { if (it.id == tripId) transform(it) else it })

    private fun List<TravelSchedule>.normalizeOrders(tripId: String): List<TravelSchedule> {
        val normalized = filter { it.tripId == tripId }.sortedBy { it.order }
            .mapIndexed { index, item -> item.copy(order = index) }
        return filterNot { it.tripId == tripId } + normalized
    }

    companion object {
        private const val SELECTED_TRIP_ID_KEY = "selected_trip_id"
        private const val LEGACY_TRIPS_KEY = "saved_trips"
        private const val REMOTE_PAGE_SIZE = 100

        fun factory(
            getTravelState: GetTravelStateUseCase,
            saveTravelState: SaveTravelStateUseCase,
            updateTravelState: UpdateTravelStateUseCase? = null,
            publishTripInvite: PublishTripInviteUseCase? = null,
            observeSharedTripInvite: ObserveSharedTripInviteUseCase? = null,
            removeSharedTripParticipant: RemoveSharedTripParticipantUseCase? = null,
            submitSharedTripAvailability: SubmitSharedTripAvailabilityUseCase? = null,
            finalizeSharedTripDates: FinalizeSharedTripDatesUseCase? = null,
            travelGateway: TravelGateway? = null,
            authRepository: AuthRepository? = null,
        ) = viewModelFactory {
            initializer {
                TripViewModel(
                    createSavedStateHandle(),
                    getTravelState,
                    saveTravelState,
                    updateTravelState = updateTravelState,
                    publishTripInvite = publishTripInvite,
                    observeSharedTripInvite = observeSharedTripInvite,
                    removeSharedTripParticipant = removeSharedTripParticipant,
                    submitSharedTripAvailability = submitSharedTripAvailability,
                    finalizeSharedTripDates = finalizeSharedTripDates,
                    travelGateway = travelGateway,
                    authRepository = authRepository,
                )
            }
        }
    }
}

private fun TravelState.localCurrentUser(): TravelParticipant =
    participants.find { it.id == currentUserId } ?: TravelParticipant(currentUserId, "나")

private fun TravelState.participantIdsForTrip(tripId: String): Set<String> =
    trips.find { it.id == tripId }?.participantIds.orEmpty().toSet() + currentUserId

private fun TravelState.withValidatedExpense(expense: TravelExpense): TravelState {
    ValidateTravelExpenseUseCase()(expense).getOrThrow()
    require(trips.any { it.id == expense.tripId }) { "여행을 찾을 수 없어요." }
    require(expense.scheduleId.isBlank() || schedules.any { it.id == expense.scheduleId && it.tripId == expense.tripId }) {
        "일정을 찾을 수 없어요."
    }
    val tripParticipantIds = participantIdsForTrip(expense.tripId)
    require(
        expense.paymentSource == com.gayadi.android.domain.model.ExpensePaymentSource.SHARED_FUND ||
            expense.payerId in tripParticipantIds,
    ) { "결제자를 여행 참여자 중에서 선택해 주세요." }
    require(expense.participantIds.all { it in tripParticipantIds }) {
        "분담 참여자를 여행 참여자 중에서 선택해 주세요."
    }
    val existing = expenses.find { it.id == expense.id }
    require(existing == null || existing.tripId == expense.tripId) {
        "비용을 다른 여행으로 옮길 수 없어요."
    }
    try {
        expenses.asSequence()
            .filter { it.tripId == expense.tripId && it.id != expense.id }
            .fold(expense.amount) { total, item -> Math.addExact(total, item.amount) }
    } catch (error: ArithmeticException) {
        throw IllegalArgumentException("여행 총액이 너무 커요. 비용 금액을 줄여 주세요.", error)
    }
    return copy(expenses = expenses.filterNot { it.id == expense.id } + expense)
}

private fun routeKey(tripId: String, routeType: String) = "$tripId:$routeType"

private fun TravelTrip.toSummary() = TripSummary(
    id = id,
    name = name,
    startDate = startDate,
    endDate = endDate,
    cities = cities,
    coverImageResList = cityCoverImageResources(cities).ifEmpty { coverImageResList },
    status = status,
    participantIds = participantIds,
    inviteCode = inviteCode,
    isGroupTrip = isGroupTrip,
    dateAvailability = dateAvailability,
    ownerId = ownerId,
)

private fun TripSummary.toDomain(existing: TravelTrip? = null) = TravelTrip(
    id = id,
    name = name,
    startDate = startDate,
    endDate = endDate,
    cities = cities,
    coverImageResList = coverImageResList,
    status = existing?.status ?: status,
    participantIds = existing?.participantIds ?: participantIds,
    inviteCode = existing?.inviteCode?.ifBlank { inviteCode } ?: inviteCode,
    isGroupTrip = existing?.isGroupTrip ?: isGroupTrip,
    dateAvailability = existing?.dateAvailability ?: dateAvailability,
    ownerId = existing?.ownerId?.ifBlank { ownerId } ?: ownerId,
    version = existing?.version ?: 0,
)

private const val INVITE_CODE_LENGTH = 6
private const val MAX_INVITE_CODE_ATTEMPTS = 100
private fun randomInviteCode(): String = UUID.randomUUID().toString().replace("-", "").take(INVITE_CODE_LENGTH).uppercase()
private fun normalizeInviteCode(code: String): String = code.uppercase().filter { it in 'A'..'Z' || it in '0'..'9' }.take(INVITE_CODE_LENGTH)

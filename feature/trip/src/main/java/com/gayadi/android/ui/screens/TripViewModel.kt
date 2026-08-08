package com.gayadi.android.ui.screens

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.gayadi.android.domain.model.DepartureMode
import com.gayadi.android.domain.model.InvitationStatus
import com.gayadi.android.domain.model.ScheduleType
import com.gayadi.android.domain.model.TravelInvitation
import com.gayadi.android.domain.model.TravelParticipant
import com.gayadi.android.domain.model.TravelSchedule
import com.gayadi.android.domain.model.TravelState
import com.gayadi.android.domain.model.TravelTrip
import com.gayadi.android.domain.model.TripStatus
import com.gayadi.android.domain.usecase.GetTravelStateUseCase
import com.gayadi.android.domain.usecase.SaveTravelStateUseCase
import java.util.UUID
import java.nio.charset.StandardCharsets
import java.util.Base64
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray

data class TravelUiState(
    val travelState: TravelState = TravelState(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val message: String? = null,
)

class TripViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val getTravelState: GetTravelStateUseCase,
    private val saveTravelState: SaveTravelStateUseCase,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {
    private val persistenceMutex = Mutex()
    private val legacyTrips = decodeLegacyTrips(savedStateHandle[LEGACY_TRIPS_KEY])
    private val _uiState = MutableStateFlow(TravelUiState())
    val uiState = _uiState.asStateFlow()
    val trips = uiState.map { state -> state.travelState.trips.map(TravelTrip::toSummary) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val selectedTripId = uiState.map { it.travelState.selectedTripId }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val availableParticipants = listOf(
        TravelParticipant("user-101", "여행곰", "character_pca"),
        TravelParticipant("user-102", "바다별", "character_sca"),
        TravelParticipant("user-103", "산책러", "character_snr"),
    )

    init {
        loadState()
    }

    fun retry() = loadState()

    fun consumeMessage() = _uiState.update { it.copy(message = null) }

    fun addTrip(trip: TripSummary) = mutate("여행을 만들었어요") { state ->
        state.copy(trips = listOf(trip.toDomain()) + state.trips)
    }

    fun updateTrip(trip: TripSummary) = mutate("여행 정보를 수정했어요") { state ->
        state.copy(trips = state.trips.map { if (it.id == trip.id) trip.toDomain(it) else it })
    }

    fun deleteTrip(tripId: String) = mutate("여행을 삭제했어요") { state ->
        state.copy(
            trips = state.trips.filterNot { it.id == tripId },
            invitations = state.invitations.filterNot { it.tripId == tripId },
            schedules = state.schedules.filterNot { it.tripId == tripId },
            selectedTripId = state.selectedTripId.takeUnless { it == tripId },
        )
    }

    fun selectTrip(tripId: String) = mutate(null) { state ->
        if (state.trips.none { it.id == tripId }) state else state.copy(selectedTripId = tripId)
    }

    fun startTrip(tripId: String) = mutate("여행을 시작했어요") { state ->
        state.updateTrip(tripId) { trip ->
            if (trip.status == TripStatus.PLANNING) trip.copy(status = TripStatus.ONGOING) else trip
        }
    }

    fun finishTrip(tripId: String) = mutate("여행을 종료했어요") { state ->
        state.updateTrip(tripId) { trip ->
            if (trip.status == TripStatus.ONGOING) trip.copy(status = TripStatus.COMPLETED) else trip
        }
    }

    fun setDepartureMode(tripId: String, mode: DepartureMode) = mutate("출발 방식을 저장했어요") { state ->
        state.updateTrip(tripId) { it.copy(departureMode = mode) }
    }

    fun addParticipant(tripId: String, participantId: String) = mutate("참여자를 추가했어요") { state ->
        val participant = availableParticipants.find { it.id == participantId } ?: return@mutate state
        state.copy(
            participants = (state.participants + participant).distinctBy(TravelParticipant::id),
            trips = state.trips.map { trip ->
                if (trip.id == tripId) trip.copy(participantIds = (trip.participantIds + participantId).distinct())
                else trip
            },
        )
    }

    fun removeParticipant(tripId: String, participantId: String) = mutate("참여자를 내보냈어요") { state ->
        state.updateTrip(tripId) { it.copy(participantIds = it.participantIds - participantId) }
    }

    fun createInvitation(tripId: String, inviteeId: String): String {
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
        val invitation = _uiState.value.travelState.invitations.find {
            it.code.equals(code.trim(), ignoreCase = true) && it.status == InvitationStatus.PENDING
        }
        if (invitation == null) {
            _uiState.update { it.copy(message = "유효한 초대 코드를 찾지 못했어요") }
        } else {
            acceptInvitation(invitation.id)
        }
    }

    fun upsertSchedule(schedule: TravelSchedule) = mutate("일정을 저장했어요") { state ->
        val current = state.schedules.filterNot { it.id == schedule.id }
        state.copy(schedules = (current + schedule).normalizeOrders(schedule.tripId))
    }

    fun addPlaceSchedule(tripId: String, placeId: String, title: String) {
        val trip = _uiState.value.travelState.trips.find { it.id == tripId } ?: return
        val order = schedulesForTrip(tripId).size
        upsertSchedule(
            TravelSchedule(
                id = UUID.randomUUID().toString(),
                tripId = tripId,
                title = title,
                placeId = placeId,
                date = trip.startDate,
                time = "10:00",
                order = order,
            ),
        )
    }

    fun deleteSchedule(scheduleId: String) = mutate("일정을 삭제했어요") { state ->
        val tripId = state.schedules.find { it.id == scheduleId }?.tripId ?: return@mutate state
        state.copy(schedules = state.schedules.filterNot { it.id == scheduleId }.normalizeOrders(tripId))
    }

    fun moveSchedule(scheduleId: String, direction: Int) = mutate(null) { state ->
        val schedule = state.schedules.find { it.id == scheduleId } ?: return@mutate state
        val tripSchedules = state.schedules.filter { it.tripId == schedule.tripId }.sortedBy { it.order }.toMutableList()
        val from = tripSchedules.indexOfFirst { it.id == scheduleId }
        val to = (from + direction).coerceIn(0, tripSchedules.lastIndex)
        if (from == to) return@mutate state
        val moved = tripSchedules.removeAt(from)
        tripSchedules.add(to, moved)
        val reordered = tripSchedules.mapIndexed { index, item -> item.copy(order = index) }
        state.copy(schedules = state.schedules.filterNot { it.tripId == schedule.tripId } + reordered)
    }

    fun toggleVisited(scheduleId: String) = mutate(null) { state ->
        state.copy(schedules = state.schedules.map {
            if (it.id == scheduleId) it.copy(isVisited = !it.isVisited) else it
        })
    }

    fun toggleFavorite(placeId: String) = mutate(null) { state ->
        val favorites = state.favoritePlaceIds
        state.copy(favoritePlaceIds = if (placeId in favorites) favorites - placeId else favorites + placeId)
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

    private fun loadState() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch(ioDispatcher) {
            getTravelState().fold(
                onSuccess = { state ->
                    val restored = if (state.trips.isEmpty() && legacyTrips.isNotEmpty()) {
                        state.copy(trips = legacyTrips.map(TripSummary::toDomain))
                    } else state
                    savedStateHandle[SELECTED_TRIP_ID_KEY] = restored.selectedTripId
                    _uiState.value = TravelUiState(travelState = restored, isLoading = false)
                    if (restored !== state) persistLatest()
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
        val updated = _uiState.updateAndGet {
            it.copy(travelState = transform(it.travelState), message = message, errorMessage = null)
        }.travelState
        savedStateHandle[SELECTED_TRIP_ID_KEY] = updated.selectedTripId
        viewModelScope.launch(ioDispatcher) {
            persistLatest()
        }
    }

    private suspend fun persistLatest() = persistenceMutex.withLock {
        saveTravelState(_uiState.value.travelState).onFailure { error ->
            _uiState.update { it.copy(errorMessage = error.message ?: "여행 정보를 저장하지 못했어요") }
        }
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

        fun factory(getTravelState: GetTravelStateUseCase, saveTravelState: SaveTravelStateUseCase) = viewModelFactory {
            initializer {
                TripViewModel(createSavedStateHandle(), getTravelState, saveTravelState)
            }
        }
    }
}

private fun routeKey(tripId: String, routeType: String) = "$tripId:$routeType"

private fun TravelTrip.toSummary() = TripSummary(
    id = id,
    name = name,
    startDate = startDate,
    endDate = endDate,
    cities = cities,
    coverImageResList = coverImageResList,
    departureMode = departureMode,
    status = status,
    participantIds = participantIds,
)

private fun TripSummary.toDomain(existing: TravelTrip? = null) = TravelTrip(
    id = id,
    name = name,
    startDate = startDate,
    endDate = endDate,
    cities = cities,
    coverImageResList = coverImageResList,
    departureMode = existing?.departureMode ?: departureMode,
    status = existing?.status ?: status,
    participantIds = existing?.participantIds ?: participantIds,
)

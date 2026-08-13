package com.gayadi.android.ui.screens

import androidx.lifecycle.SavedStateHandle
import com.gayadi.android.domain.model.InvitationStatus
import com.gayadi.android.domain.model.ScheduleType
import com.gayadi.android.domain.model.TravelExpense
import com.gayadi.android.domain.model.TravelParticipant
import com.gayadi.android.domain.model.TravelSchedule
import com.gayadi.android.domain.model.TravelState
import com.gayadi.android.domain.model.TravelTrip
import com.gayadi.android.domain.model.TripStatus
import com.gayadi.android.domain.repository.TravelRepository
import com.gayadi.android.domain.usecase.GetTravelStateUseCase
import com.gayadi.android.domain.usecase.SaveTravelStateUseCase
import com.gayadi.android.domain.usecase.UpdateTravelStateUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.async
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TripViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun tripCrudStatusParticipantsAndCascadeDelete() = runTest(dispatcher) {
        val repository = MemoryTravelRepository()
        val viewModel = viewModel(repository)
        advanceUntilIdle()
        viewModel.addTrip(sampleTrip())
        viewModel.selectTrip("trip-28")
        viewModel.addParticipant("trip-28", "user-101")
        viewModel.startTrip("trip-28")
        viewModel.finishTrip("trip-28")
        viewModel.createInvitation("trip-28", "user-102")
        viewModel.upsertSchedule(sampleSchedule("schedule-1"))
        advanceUntilIdle()

        val trip = viewModel.domainTripById("trip-28")!!
        assertEquals(TripStatus.COMPLETED, trip.status)
        assertEquals(listOf("local-user", "user-101"), trip.participantIds)
        assertEquals("trip-28", viewModel.selectedTripId.value)

        viewModel.deleteTrip("trip-28")
        advanceUntilIdle()
        assertTrue(viewModel.trips.value.isEmpty())
        assertTrue(repository.state.invitations.isEmpty())
        assertTrue(repository.state.schedules.isEmpty())
        assertNull(repository.state.selectedTripId)
    }

    @Test
    fun invitationDecisionIsOneWayAndCodeJoinAddsParticipant() = runTest(dispatcher) {
        val repository = MemoryTravelRepository()
        val viewModel = viewModel(repository)
        advanceUntilIdle()
        viewModel.addTrip(sampleTrip())
        val code = viewModel.createInvitation("trip-28", "user-102")
        advanceUntilIdle()

        viewModel.joinByCode(code.lowercase())
        advanceUntilIdle()
        val invitation = viewModel.invitationForTrip("trip-28")!!
        assertEquals(InvitationStatus.ACCEPTED, invitation.status)
        assertTrue("user-102" in viewModel.domainTripById("trip-28")!!.participantIds)

        viewModel.declineInvitation(invitation.id)
        advanceUntilIdle()
        assertEquals(InvitationStatus.ACCEPTED, viewModel.invitationForTrip("trip-28")!!.status)
    }

    @Test
    fun schedulesSupportCrudReorderAlternativeAndVisited() = runTest(dispatcher) {
        val repository = MemoryTravelRepository()
        val viewModel = viewModel(repository)
        advanceUntilIdle()
        viewModel.addTrip(sampleTrip())
        viewModel.upsertSchedule(sampleSchedule("main", order = 0))
        viewModel.upsertSchedule(sampleSchedule("alternative", order = 1, type = ScheduleType.ALTERNATIVE))
        viewModel.moveSchedule("alternative", -1)
        viewModel.toggleVisited("alternative")
        advanceUntilIdle()

        val schedules = viewModel.schedulesForTrip("trip-28")
        assertEquals(listOf("alternative", "main"), schedules.map { it.id })
        assertEquals(ScheduleType.ALTERNATIVE, schedules.first().type)
        assertTrue(schedules.first().isVisited)

        viewModel.deleteSchedule("main")
        advanceUntilIdle()
        assertEquals(listOf(0), viewModel.schedulesForTrip("trip-28").map { it.order })
    }

    @Test
    fun expenseCrudSettlementPersistenceAndScheduleCascadeDelete() = runTest(dispatcher) {
        val repository = MemoryTravelRepository()
        val viewModel = viewModel(repository)
        advanceUntilIdle()
        viewModel.addTrip(sampleTrip())
        viewModel.addParticipant("trip-28", "user-101")
        viewModel.upsertSchedule(sampleSchedule("schedule-1"))
        advanceUntilIdle()

        assertTrue(viewModel.upsertExpense(sampleExpense(amount = 1_001L)).isSuccess)
        advanceUntilIdle()

        val summary = viewModel.settlementForTrip("trip-28").getOrThrow()
        assertEquals(1_001L, summary.totalAmount)
        assertEquals(1_001L, summary.balances.sumOf { it.paidAmount })
        assertEquals(1_001L, summary.balances.sumOf { it.owedAmount })
        assertEquals(500L, summary.transfers.single().amount)
        assertEquals("user-101", summary.transfers.single().fromParticipantId)
        assertEquals("local-user", summary.transfers.single().toParticipantId)

        val recreated = viewModel(repository)
        advanceUntilIdle()
        assertEquals(1_001L, recreated.expensesForTrip("trip-28").single().amount)

        assertTrue(recreated.upsertExpense(sampleExpense(amount = 2_000L).copy(title = "수정 비용")).isSuccess)
        advanceUntilIdle()
        assertEquals(2_000L, repository.state.expenses.single().amount)
        assertEquals("수정 비용", repository.state.expenses.single().title)

        recreated.deleteSchedule("schedule-1")
        advanceUntilIdle()
        assertTrue(repository.state.expenses.isEmpty())
    }

    @Test
    fun invalidSavedExpenseShowsSettlementErrorWithoutThrowing() = runTest(dispatcher) {
        val trip = sampleTrip().toExistingDomain().copy(
            participantIds = listOf("local-user", "user-101"),
        )
        val invalidExpense = sampleExpense().copy(payerId = "")
        val repository = MemoryTravelRepository(
            TravelState(
                trips = listOf(trip),
                participants = listOf(
                    TravelParticipant("local-user", "나"),
                    TravelParticipant("user-101", "여행곰"),
                ),
                schedules = listOf(sampleSchedule("schedule-1")),
                expenses = listOf(invalidExpense),
            ),
        )
        val viewModel = viewModel(repository)
        advanceUntilIdle()

        val summary = viewModel.settlementForTrip("trip-28")

        assertTrue(summary.isFailure)
        assertEquals("결제자를 선택해 주세요.", summary.exceptionOrNull()?.message)
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun invalidExpenseIsRejectedAndReferencedParticipantCannotBeRemoved() = runTest(dispatcher) {
        val repository = MemoryTravelRepository()
        val viewModel = viewModel(repository)
        advanceUntilIdle()
        viewModel.addTrip(sampleTrip())
        viewModel.addParticipant("trip-28", "user-101")
        viewModel.upsertSchedule(sampleSchedule("schedule-1"))
        advanceUntilIdle()

        assertTrue(viewModel.upsertExpense(sampleExpense(amount = 0L)).isFailure)
        assertTrue(viewModel.upsertExpense(sampleExpense().copy(payerId = "outsider")).isFailure)
        assertTrue(repository.state.expenses.isEmpty())

        assertTrue(viewModel.upsertExpense(sampleExpense()).isSuccess)
        advanceUntilIdle()
        viewModel.removeParticipant("trip-28", "user-101")
        advanceUntilIdle()

        assertTrue("user-101" in repository.state.trips.single().participantIds)
        assertEquals("비용 내역에 포함된 참여자는 내보낼 수 없어요", viewModel.uiState.value.message)
    }

    @Test
    fun concurrentExpenseSaveAndParticipantRemovalPreserveReferences() = runTest(dispatcher) {
        val repository = MemoryTravelRepository()
        val viewModel = viewModel(repository)
        advanceUntilIdle()
        viewModel.addTrip(sampleTrip())
        viewModel.addParticipant("trip-28", "user-101")
        viewModel.upsertSchedule(sampleSchedule("schedule-1"))
        advanceUntilIdle()

        val save = async { viewModel.upsertExpense(sampleExpense()) }
        viewModel.removeParticipant("trip-28", "user-101")
        advanceUntilIdle()
        save.await()

        val participantExists = "user-101" in repository.state.trips.single().participantIds
        val expenseExists = repository.state.expenses.any { "user-101" in it.participantIds }
        assertFalse(expenseExists && !participantExists)
    }

    @Test
    fun fallbackMutationCapturesTransformFailure() = runTest(dispatcher) {
        val repository = MemoryTravelRepository()
        val viewModel = viewModelWithoutAtomicUpdate(repository)
        advanceUntilIdle()
        viewModel.addTrip(sampleTrip())
        viewModel.addParticipant("trip-28", "user-101")
        viewModel.upsertSchedule(sampleSchedule("schedule-1"))
        advanceUntilIdle()

        viewModel.saveExpense(sampleExpense())
        viewModel.removeParticipant("trip-28", "user-101")
        advanceUntilIdle()

        assertTrue(repository.state.expenses.any { "user-101" in it.participantIds })
        assertTrue("user-101" in repository.state.trips.single().participantIds)
        assertEquals("비용 내역에 포함된 참여자는 내보낼 수 없어요", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun expenseSaveFailureKeepsEditorStateAndOverflowIsRejected() = runTest(dispatcher) {
        val repository = MemoryTravelRepository()
        val viewModel = viewModel(repository)
        advanceUntilIdle()
        viewModel.addTrip(sampleTrip())
        viewModel.addParticipant("trip-28", "user-101")
        viewModel.upsertSchedule(sampleSchedule("schedule-1"))
        advanceUntilIdle()

        repository.failWrites = true
        val failedWrite = viewModel.upsertExpense(sampleExpense())
        assertTrue(failedWrite.isFailure)
        assertFalse(viewModel.uiState.value.isSavingExpense)
        assertNull(viewModel.uiState.value.errorMessage)
        assertEquals("저장 실패", viewModel.uiState.value.expenseErrorMessage)
        assertTrue(viewModel.expensesForTrip("trip-28").isEmpty())

        viewModel.clearExpenseError()
        assertNull(viewModel.uiState.value.expenseErrorMessage)

        viewModel.upsertSchedule(sampleSchedule("schedule-2"))
        advanceUntilIdle()
        assertEquals("저장 실패", viewModel.uiState.value.errorMessage)
        assertNull(viewModel.uiState.value.expenseErrorMessage)

        repository.failWrites = false
        assertTrue(viewModel.upsertExpense(sampleExpense(Long.MAX_VALUE)).isSuccess)
        val overflow = viewModel.upsertExpense(
            sampleExpense(1L).copy(id = "expense-2"),
        )
        assertTrue(overflow.isFailure)
        assertEquals(1, repository.state.expenses.size)
    }

    @Test
    fun viewModelOwnedExpenseSaveSurvivesCallerRecreationAndIgnoresDuplicateSubmit() = runTest(dispatcher) {
        val repository = MemoryTravelRepository()
        val viewModel = viewModel(repository)
        advanceUntilIdle()
        viewModel.addTrip(sampleTrip())
        viewModel.addParticipant("trip-28", "user-101")
        viewModel.upsertSchedule(sampleSchedule("schedule-1"))
        advanceUntilIdle()

        repository.updateStarted = CompletableDeferred()
        repository.releaseUpdate = CompletableDeferred()
        viewModel.saveExpense(sampleExpense())
        runCurrent()

        assertTrue(viewModel.uiState.value.isSavingExpense)
        repository.updateStarted?.await()
        viewModel.saveExpense(sampleExpense().copy(id = "duplicate-expense"))
        repository.releaseUpdate?.complete(Unit)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isSavingExpense)
        assertEquals("expense-1", viewModel.uiState.value.savedExpenseId)
        assertEquals(listOf("expense-1"), repository.state.expenses.map(TravelExpense::id))
        viewModel.consumeSavedExpense()
        assertNull(viewModel.uiState.value.savedExpenseId)
    }

    @Test
    fun currentUserFallbackIsPersistedIntoExistingTripsWithProfileContext() = runTest(dispatcher) {
        val existing = sampleTrip().toExistingDomain()
        val repository = MemoryTravelRepository(TravelState(trips = listOf(existing)))
        val viewModel = viewModel(repository)
        advanceUntilIdle()

        viewModel.syncCurrentUser("미르", "character_pca")
        advanceUntilIdle()

        assertTrue("local-user" in repository.state.trips.single().participantIds)
        assertEquals(
            TravelParticipant("local-user", "미르", "character_pca"),
            repository.state.participants.single { it.id == "local-user" },
        )
    }

    @Test
    fun favoritesAndFileLikeRepositoryStateSurviveViewModelRecreation() = runTest(dispatcher) {
        val repository = MemoryTravelRepository()
        val first = viewModel(repository)
        advanceUntilIdle()
        first.addTrip(sampleTrip())
        first.toggleFavorite("place-3")
        first.applyRoute("trip-28", "ITINERARY", "balanced")
        advanceUntilIdle()

        val recreated = viewModel(repository)
        advanceUntilIdle()
        assertEquals("제주 여행", recreated.tripById("trip-28")?.name)
        assertTrue(recreated.isFavorite("place-3"))
        assertEquals("balanced", recreated.appliedRouteId("trip-28", "ITINERARY"))
        recreated.toggleFavorite("place-3")
        advanceUntilIdle()
        assertFalse(recreated.isFavorite("place-3"))
    }

    @Test
    fun generatedInviteCodeSkipsCollisionAndPersistsWithTrip() = runTest(dispatcher) {
        val existingTrip = TravelTrip(
            id = "existing",
            name = "기존 여행",
            startDate = "2026.08.01",
            endDate = "2026.08.02",
            cities = listOf("서울"),
            inviteCode = "ABC123",
        )
        val repository = MemoryTravelRepository(TravelState(trips = listOf(existingTrip)))
        val candidates = listOf("ABC123", "XYZ789").iterator()
        val viewModel = viewModel(repository, inviteCodeGenerator = candidates::next)
        advanceUntilIdle()

        val savedTrip = viewModel.addTrip(sampleTrip()).getOrThrow()
        advanceUntilIdle()

        assertEquals("XYZ789", savedTrip.inviteCode)
        assertEquals("XYZ789", repository.state.trips.single { it.id == "trip-28" }.inviteCode)
        assertEquals(2, repository.state.trips.map(TravelTrip::inviteCode).distinct().size)
    }

    @Test
    fun clearAllTravelData_removesPersistedAndInMemoryState() = runTest(dispatcher) {
        val repository = MemoryTravelRepository()
        val viewModel = viewModel(repository)
        advanceUntilIdle()
        viewModel.addTrip(sampleTrip())
        viewModel.toggleFavorite("place-3")
        advanceUntilIdle()

        viewModel.clearAllTravelData().getOrThrow()

        assertEquals(TravelState(), repository.state)
        assertEquals(TravelState(), viewModel.uiState.value.travelState)
        assertFalse(viewModel.uiState.value.isLoading)
        assertTrue(viewModel.uiState.value.hasLoadedTravelState)
    }

    @Test
    fun failedInitialLoadIsNotExposedAsAValidEmptyState() = runTest(dispatcher) {
        val repository = FailingTravelRepository()
        val viewModel = TripViewModel(
            SavedStateHandle(),
            GetTravelStateUseCase(repository),
            SaveTravelStateUseCase(repository),
            dispatcher,
            updateTravelState = UpdateTravelStateUseCase(repository),
        )

        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertFalse(viewModel.uiState.value.hasLoadedTravelState)
        assertTrue(viewModel.uiState.value.errorMessage?.contains("손상") == true)
    }

    @Test
    fun inviteCodeGenerationStopsAfterMaximumAttempts() = runTest(dispatcher) {
        val existingTrip = TravelTrip(
            id = "existing",
            name = "기존 여행",
            startDate = "2026.08.01",
            endDate = "2026.08.02",
            cities = listOf("서울"),
            inviteCode = "ABC123",
        )
        val repository = MemoryTravelRepository(TravelState(trips = listOf(existingTrip)))
        val viewModel = viewModel(repository, inviteCodeGenerator = { "ABC123" })
        advanceUntilIdle()

        val result = viewModel.addTrip(sampleTrip())
        advanceUntilIdle()

        assertTrue(result.isFailure)
        assertEquals(listOf("existing"), repository.state.trips.map(TravelTrip::id))
    }

    @Test
    fun migratesLegacySavedTripsIntoRepository() = runTest(dispatcher) {
        val legacyJson = """[{"id":"legacy-1","name":"제주 여행","startDate":"2026.08.08","endDate":"2026.08.10","cities":["제주"],"coverImageResList":[1,2]}]"""
        val repository = MemoryTravelRepository()
        val viewModel = viewModel(repository, SavedStateHandle(mapOf("saved_trips" to legacyJson)))
        advanceUntilIdle()

        assertEquals("legacy-1", viewModel.trips.value.single().id)
        assertEquals("legacy-1", repository.state.trips.single().id)
    }

    private fun viewModel(
        repository: MemoryTravelRepository,
        savedStateHandle: SavedStateHandle = SavedStateHandle(),
        inviteCodeGenerator: () -> String = { "GAYADI" },
    ) = TripViewModel(
        savedStateHandle,
        GetTravelStateUseCase(repository),
        SaveTravelStateUseCase(repository),
        dispatcher,
        inviteCodeGenerator,
        UpdateTravelStateUseCase(repository),
    )

    private fun viewModelWithoutAtomicUpdate(
        repository: MemoryTravelRepository,
    ) = TripViewModel(
        SavedStateHandle(),
        GetTravelStateUseCase(repository),
        SaveTravelStateUseCase(repository),
        dispatcher,
        updateTravelState = null,
    )

    private fun sampleTrip() = TripSummary(
        id = "trip-28",
        name = "제주 여행",
        startDate = "2026.08.08",
        endDate = "2026.08.10",
        cities = listOf("제주"),
        coverImageResList = emptyList(),
    )

    private fun sampleSchedule(
        id: String,
        order: Int = 0,
        type: ScheduleType = ScheduleType.MAIN,
    ) = TravelSchedule(id, "trip-28", id, null, "2026.08.08", "10:00", type, order)

    private fun sampleExpense(amount: Long = 1_001L) = TravelExpense(
        id = "expense-1",
        tripId = "trip-28",
        scheduleId = "schedule-1",
        title = "점심",
        amount = amount,
        payerId = "local-user",
        participantIds = listOf("local-user", "user-101"),
        date = "2026.08.08",
        time = "12:00",
    )

    private fun TripSummary.toExistingDomain() = TravelTrip(
        id = id,
        name = name,
        startDate = startDate,
        endDate = endDate,
        cities = cities,
        coverImageResList = coverImageResList,
    )
}

private class MemoryTravelRepository(initial: TravelState = TravelState()) : TravelRepository {
    var state = initial
    var failWrites = false
    var updateStarted: CompletableDeferred<Unit>? = null
    var releaseUpdate: CompletableDeferred<Unit>? = null
    override suspend fun getTravelState(): Result<TravelState> = Result.success(state)
    override suspend fun saveTravelState(state: TravelState): Result<Unit> {
        if (failWrites) return Result.failure(IllegalStateException("저장 실패"))
        this.state = state
        return Result.success(Unit)
    }

    override suspend fun updateTravelState(
        transform: (TravelState) -> TravelState,
    ): Result<TravelState> {
        updateStarted?.complete(Unit)
        releaseUpdate?.await()
        return super<TravelRepository>.updateTravelState(transform)
    }
}

private class FailingTravelRepository : TravelRepository {
    private val error = IllegalStateException("여행 데이터 손상")

    override suspend fun getTravelState(): Result<TravelState> = Result.failure(error)

    override suspend fun saveTravelState(state: TravelState): Result<Unit> = Result.failure(error)
}

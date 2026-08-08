package com.gayadi.android.ui.screens

import androidx.lifecycle.SavedStateHandle
import com.gayadi.android.domain.model.DepartureMode
import com.gayadi.android.domain.model.InvitationStatus
import com.gayadi.android.domain.model.ScheduleType
import com.gayadi.android.domain.model.TravelSchedule
import com.gayadi.android.domain.model.TravelState
import com.gayadi.android.domain.model.TripStatus
import com.gayadi.android.domain.repository.TravelRepository
import com.gayadi.android.domain.usecase.GetTravelStateUseCase
import com.gayadi.android.domain.usecase.SaveTravelStateUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
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
    fun tripCrudStatusDepartureParticipantsAndCascadeDelete() = runTest(dispatcher) {
        val repository = MemoryTravelRepository()
        val viewModel = viewModel(repository)
        advanceUntilIdle()
        viewModel.addTrip(sampleTrip())
        viewModel.selectTrip("trip-28")
        viewModel.setDepartureMode("trip-28", DepartureMode.TOGETHER)
        viewModel.addParticipant("trip-28", "user-101")
        viewModel.startTrip("trip-28")
        viewModel.finishTrip("trip-28")
        viewModel.createInvitation("trip-28", "user-102")
        viewModel.upsertSchedule(sampleSchedule("schedule-1"))
        advanceUntilIdle()

        val trip = viewModel.domainTripById("trip-28")!!
        assertEquals(DepartureMode.TOGETHER, trip.departureMode)
        assertEquals(TripStatus.COMPLETED, trip.status)
        assertEquals(listOf("user-101"), trip.participantIds)
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
    ) = TripViewModel(
        savedStateHandle,
        GetTravelStateUseCase(repository),
        SaveTravelStateUseCase(repository),
        dispatcher,
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
}

private class MemoryTravelRepository(initial: TravelState = TravelState()) : TravelRepository {
    var state = initial
    override suspend fun getTravelState(): Result<TravelState> = Result.success(state)
    override suspend fun saveTravelState(state: TravelState): Result<Unit> {
        this.state = state
        return Result.success(Unit)
    }
}

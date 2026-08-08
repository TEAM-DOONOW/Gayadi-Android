package com.gayadi.android.data

import com.gayadi.android.data.repository.FileTravelRepository
import com.gayadi.android.domain.model.InvitationStatus
import com.gayadi.android.domain.model.ScheduleType
import com.gayadi.android.domain.model.TravelInvitation
import com.gayadi.android.domain.model.TravelParticipant
import com.gayadi.android.domain.model.TravelSchedule
import com.gayadi.android.domain.model.TravelState
import com.gayadi.android.domain.model.TravelTrip
import com.gayadi.android.domain.model.TripStatus
import java.nio.file.Files
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.Dispatchers
import java.util.concurrent.CyclicBarrier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FileTravelRepositoryTest {
    @Test
    fun fullStateRoundTripsAcrossRepositoryRecreation() = runTest {
        val directory = Files.createTempDirectory("travel-repository-test").toFile()
        try {
            val file = directory.resolve("travel-state.json")
            val dispatcher = StandardTestDispatcher(testScheduler)
            val state = fullState()
            assertTrue(FileTravelRepository(file, dispatcher).saveTravelState(state).isSuccess)

            val restored = FileTravelRepository(file, dispatcher).getTravelState().getOrThrow()

            assertEquals(state, restored)
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test
    fun missingFileReturnsEmptyStateAndMalformedFileReturnsFailure() = runTest {
        val directory = Files.createTempDirectory("travel-repository-empty-test").toFile()
        try {
            val file = directory.resolve("travel-state.json")
            val dispatcher = StandardTestDispatcher(testScheduler)
            val repository = FileTravelRepository(file, dispatcher)
            assertEquals(TravelState(), repository.getTravelState().getOrThrow())
            file.writeText("not-json")
            assertTrue(repository.getTravelState().isFailure)
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test
    fun concurrentRepositoryInstancesAlwaysLeaveOneCompleteState() = runTest {
        val directory = Files.createTempDirectory("travel-repository-concurrent-test").toFile()
        try {
            val file = directory.resolve("travel-state.json")
            val states = (1..20).map { index ->
                fullState().copy(trips = fullState().trips.map { it.copy(id = "trip-$index", name = "여행 $index") })
            }
            val barrier = CyclicBarrier(states.size)
            states.map { state ->
                async(Dispatchers.IO) {
                    barrier.await()
                    FileTravelRepository(file, Dispatchers.IO).saveTravelState(state).getOrThrow()
                }
            }.awaitAll()

            val restored = FileTravelRepository(file, Dispatchers.IO).getTravelState().getOrThrow()
            assertTrue(restored in states)
        } finally {
            directory.deleteRecursively()
        }
    }

    private fun fullState() = TravelState(
        trips = listOf(
            TravelTrip(
                id = "trip-28",
                name = "제주 여행",
                startDate = "2026.08.08",
                endDate = "2026.08.10",
                cities = listOf("제주"),
                coverImageResList = listOf(1, 2),
                status = TripStatus.ONGOING,
                participantIds = listOf("user-101"),
            ),
        ),
        participants = listOf(TravelParticipant("user-101", "여행곰", "character_pca")),
        invitations = listOf(
            TravelInvitation("invite-1", "trip-28", "ABC12345", "user-101", InvitationStatus.ACCEPTED),
        ),
        schedules = listOf(
            TravelSchedule(
                id = "schedule-1",
                tripId = "trip-28",
                title = "섭지코지",
                placeId = "place-3",
                date = "2026.08.08",
                time = "10:00",
                type = ScheduleType.ALTERNATIVE,
                order = 0,
                isVisited = true,
            ),
        ),
        favoritePlaceIds = setOf("place-3"),
        appliedRouteIds = mapOf("trip-28:ITINERARY" to "balanced"),
        selectedTripId = "trip-28",
    )
}

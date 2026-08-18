package com.gayadi.android.data

import com.gayadi.android.data.repository.FileTravelRepository
import com.gayadi.android.domain.model.InvitationStatus
import com.gayadi.android.domain.model.ExpenseCategory
import com.gayadi.android.domain.model.ExpensePaymentSource
import com.gayadi.android.domain.model.LOCAL_CURRENT_USER_ID
import com.gayadi.android.domain.model.ScheduleType
import com.gayadi.android.domain.model.TravelExpense
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
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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

    @Test
    fun legacyJsonDefaultsNewFieldsAndNextSaveWritesSchemaVersionTwo() = runTest {
        val directory = Files.createTempDirectory("travel-repository-legacy-test").toFile()
        try {
            val file = directory.resolve("travel-state.json")
            file.writeText(
                """{"trips":[{"id":"trip-1","name":"제주 여행","startDate":"2026.08.08","endDate":"2026.08.10","cities":["제주"]}],"schedules":[{"id":"schedule-1","tripId":"trip-1","title":"점심","date":"2026.08.08","time":"12:00","order":0}]}""",
            )
            val repository = FileTravelRepository(file, StandardTestDispatcher(testScheduler))

            val restored = repository.getTravelState().getOrThrow()

            assertEquals(null, restored.schedules.single().endTime)
            assertTrue(restored.expenses.isEmpty())
            assertEquals(LOCAL_CURRENT_USER_ID, restored.currentUserId)

            repository.saveTravelState(restored).getOrThrow()
            assertEquals(2, JSONObject(file.readText()).getInt("schemaVersion"))
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test
    fun concurrentAtomicUpdatesDoNotLoseExpenses() = runTest {
        val directory = Files.createTempDirectory("travel-repository-update-test").toFile()
        try {
            val file = directory.resolve("travel-state.json")
            val updateCount = 20
            val barrier = CyclicBarrier(updateCount)
            (1..updateCount).map { index ->
                async(Dispatchers.IO) {
                    barrier.await()
                    FileTravelRepository(file, Dispatchers.IO).updateTravelState { state ->
                        state.copy(expenses = state.expenses + expense("expense-$index"))
                    }.getOrThrow()
                }
            }.awaitAll()

            val restored = FileTravelRepository(file, Dispatchers.IO).getTravelState().getOrThrow()
            assertEquals(updateCount, restored.expenses.size)
            assertEquals(updateCount, restored.expenses.map(TravelExpense::id).distinct().size)
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test
    fun rejectsFutureFractionalAndTextSchemaVersions() = runTest {
        val directory = Files.createTempDirectory("travel-repository-schema-test").toFile()
        try {
            val file = directory.resolve("travel-state.json")
            val repository = FileTravelRepository(file, StandardTestDispatcher(testScheduler))
            listOf("3", "2.5", "\"future\"").forEach { schemaVersion ->
                file.writeText("""{"schemaVersion":$schemaVersion}""")
                assertTrue("schemaVersion=$schemaVersion", repository.getTravelState().isFailure)
            }
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test
    fun failedTransformAndOverflowLeavePreviousFileIntact() = runTest {
        val directory = Files.createTempDirectory("travel-repository-failure-test").toFile()
        try {
            val file = directory.resolve("travel-state.json")
            val repository = FileTravelRepository(file, StandardTestDispatcher(testScheduler))
            val original = fullState().copy(expenses = listOf(expense("expense-1").copy(amount = 10L)))
            repository.saveTravelState(original).getOrThrow()

            val transformFailure = repository.updateTravelState {
                throw IllegalStateException("변환 실패")
            }
            assertTrue(transformFailure.isFailure)
            assertEquals(original, repository.getTravelState().getOrThrow())

            val overflow = repository.updateTravelState { state ->
                state.copy(
                    expenses = listOf(
                        expense("expense-max").copy(amount = Long.MAX_VALUE),
                        expense("expense-over").copy(amount = 1L),
                    ),
                )
            }
            assertTrue(overflow.isFailure)
            assertFalse(directory.resolve("${file.name}.tmp").exists())
            assertEquals(original, repository.getTravelState().getOrThrow())
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
                inviteCode = "JEJU28",
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
                endTime = "11:30",
            ),
        ),
        favoritePlaceIds = setOf("place-3"),
        appliedRouteIds = mapOf("trip-28:ITINERARY" to "balanced"),
        selectedTripId = "trip-28",
        expenses = listOf(expense("expense-1")),
        currentUserId = "device-user-42",
    )

    private fun expense(id: String) = TravelExpense(
        id = id,
        tripId = "trip-28",
        scheduleId = "schedule-1",
        title = "점심",
        memo = "흑돼지",
        amount = 45_001L,
        payerId = "user-101",
        participantIds = listOf("user-101", "user-102"),
        date = "2026.08.08",
        time = "11:00",
        category = ExpenseCategory.FOOD,
        paymentSource = ExpensePaymentSource.SHARED_FUND,
        receiptImageUri = "content://receipt/expense-1",
    )
}

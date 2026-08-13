package com.gayadi.android.notification

import com.gayadi.android.domain.model.TravelSchedule
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class ExpenseReminderPlanTest {
    private val seoul = ZoneId.of("Asia/Seoul")
    private val beforeTrigger = Clock.fixed(Instant.parse("2026-08-13T01:00:00Z"), seoul)
    private val afterTrigger = Clock.fixed(Instant.parse("2026-08-13T02:00:00Z"), seoul)

    @Test
    fun unchangedFutureScheduleKeepsExistingWorkWithoutReplacementOrCancellation() {
        val schedule = schedule()
        val initial = initialPlan(schedule)

        val plan = planExpenseReminderSync(
            schedules = listOf(schedule),
            previouslyScheduledSignatures = initial.desiredSignatures,
            clock = beforeTrigger,
        )

        assertTrue(plan.remindersToEnqueue.isEmpty())
        assertTrue(plan.workNamesToCancel.isEmpty())
        assertEquals(initial.desiredSignatures, plan.desiredSignatures)
    }

    @Test
    fun updatedFutureScheduleReplacesSameUniqueWorkWithUpdatedDelayAndTitle() {
        val original = schedule()
        val initial = initialPlan(original)
        val workName = expenseReminderWorkName(original.tripId, original.id)
        val updated = original.copy(title = "늦은 점심", endTime = "11:15")

        val plan = planExpenseReminderSync(
            schedules = listOf(updated),
            previouslyScheduledSignatures = initial.desiredSignatures,
            clock = beforeTrigger,
        )

        assertTrue(plan.workNamesToCancel.isEmpty())
        assertEquals(workName, plan.remindersToEnqueue.single().workName)
        assertEquals("늦은 점심", plan.remindersToEnqueue.single().scheduleTitle)
        assertEquals(75 * 60 * 1_000L, plan.remindersToEnqueue.single().delayMillis)
        assertEquals(setOf(workName), plan.desiredSignatures.keys)
    }

    @Test
    fun deletedScheduleCancelsOnlyItsPreviouslyScheduledWork() {
        val deleted = schedule(id = "schedule-deleted")
        val retained = schedule(id = "schedule-retained", title = "저녁", endTime = "12:00")
        val initial = initialPlan(deleted, retained)
        val deletedName = expenseReminderWorkName(deleted.tripId, deleted.id)
        val retainedName = expenseReminderWorkName(retained.tripId, retained.id)

        val plan = planExpenseReminderSync(
            schedules = listOf(retained),
            previouslyScheduledSignatures = initial.desiredSignatures,
            clock = beforeTrigger,
        )

        assertEquals(setOf(deletedName), plan.workNamesToCancel)
        assertEquals(setOf(retainedName), plan.desiredSignatures.keys)
        assertTrue(plan.remindersToEnqueue.isEmpty())
    }

    @Test
    fun deletedTripCancelsItsWorkAndKeepsAnotherTripsWork() {
        val deletedFirst = schedule(id = "schedule-1", tripId = "trip-deleted")
        val deletedSecond = schedule(
            id = "schedule-2",
            tripId = "trip-deleted",
            title = "저녁",
            endTime = "12:00",
        )
        val retained = schedule(
            id = "schedule-retained",
            tripId = "trip-retained",
            title = "숙소",
            endTime = "13:00",
        )
        val initial = initialPlan(deletedFirst, deletedSecond, retained)
        val firstName = expenseReminderWorkName(deletedFirst.tripId, deletedFirst.id)
        val secondName = expenseReminderWorkName(deletedSecond.tripId, deletedSecond.id)
        val retainedName = expenseReminderWorkName(retained.tripId, retained.id)

        val plan = planExpenseReminderSync(
            schedules = listOf(retained),
            previouslyScheduledSignatures = initial.desiredSignatures,
            clock = beforeTrigger,
        )

        assertEquals(setOf(firstName, secondName), plan.workNamesToCancel)
        assertEquals(setOf(retainedName), plan.desiredSignatures.keys)
        assertTrue(plan.remindersToEnqueue.isEmpty())
    }

    @Test
    fun unchangedOverdueSchedulePreservesDeferredWorkSoItCanRunOnce() {
        val schedule = schedule(endTime = "10:30")
        val initial = initialPlan(schedule)

        val plan = planExpenseReminderSync(
            schedules = listOf(schedule),
            previouslyScheduledSignatures = initial.desiredSignatures,
            clock = afterTrigger,
        )

        assertTrue(plan.remindersToEnqueue.isEmpty())
        assertTrue(plan.workNamesToCancel.isEmpty())
        assertEquals(initial.desiredSignatures, plan.desiredSignatures)
    }

    @Test
    fun changedOverdueScheduleCancelsOldWorkAndDoesNotEnqueuePastReplacement() {
        val original = schedule(endTime = "10:30")
        val initial = initialPlan(original)
        val workName = expenseReminderWorkName(original.tripId, original.id)
        val changed = original.copy(title = "변경된 점심", endTime = "10:45")

        val plan = planExpenseReminderSync(
            schedules = listOf(changed),
            previouslyScheduledSignatures = initial.desiredSignatures,
            clock = afterTrigger,
        )

        assertEquals(setOf(workName), plan.workNamesToCancel)
        assertTrue(plan.remindersToEnqueue.isEmpty())
        assertTrue(plan.desiredSignatures.isEmpty())
    }

    @Test
    fun newlyDiscoveredPastScheduleIsNotEnqueued() {
        val plan = planExpenseReminderSync(
            schedules = listOf(schedule(endTime = "09:59")),
            previouslyScheduledSignatures = emptyMap(),
            clock = beforeTrigger,
        )

        assertTrue(plan.remindersToEnqueue.isEmpty())
        assertTrue(plan.workNamesToCancel.isEmpty())
        assertTrue(plan.desiredSignatures.isEmpty())
    }

    @Test
    fun removingEndTimeCancelsPreviouslyScheduledWork() {
        val original = schedule()
        val initial = initialPlan(original)
        val workName = expenseReminderWorkName(original.tripId, original.id)

        val plan = planExpenseReminderSync(
            schedules = listOf(original.copy(endTime = null)),
            previouslyScheduledSignatures = initial.desiredSignatures,
            clock = beforeTrigger,
        )

        assertEquals(setOf(workName), plan.workNamesToCancel)
        assertTrue(plan.remindersToEnqueue.isEmpty())
        assertTrue(plan.desiredSignatures.isEmpty())
    }

    @Test
    fun workOperationsCompleteBeforeTheSignatureLedgerIsCommitted() {
        val plan = initialPlan(schedule())
        val events = mutableListOf<String>()

        applyExpenseReminderSyncPlan(
            plan = plan,
            cancelWork = { events += "cancel:$it" },
            enqueueReminder = { events += "enqueue:${it.workName}" },
            commitDesiredSignatures = {
                events += "commit"
                true
            },
        )

        assertEquals("commit", events.last())
        assertTrue(events.first().startsWith("enqueue:"))
    }

    @Test
    fun failedWorkOperationLeavesTheSignatureLedgerUncommittedForRetry() {
        val plan = initialPlan(schedule())
        var ledgerCommitted = false

        try {
            applyExpenseReminderSyncPlan(
                plan = plan,
                cancelWork = {},
                enqueueReminder = { error("WorkManager enqueue failed") },
                commitDesiredSignatures = {
                    ledgerCommitted = true
                    true
                },
            )
            fail("Expected enqueue failure")
        } catch (failure: IllegalStateException) {
            assertEquals("WorkManager enqueue failed", failure.message)
        }

        assertFalse(ledgerCommitted)
    }

    @Test
    fun failedSignatureCommitCancelsNewlyEnqueuedWorkBeforeThrowing() {
        val plan = initialPlan(schedule())
        val workName = plan.remindersToEnqueue.single().workName
        val events = mutableListOf<String>()

        try {
            applyExpenseReminderSyncPlan(
                plan = plan,
                cancelWork = { events += "cancel:$it" },
                enqueueReminder = { events += "enqueue:${it.workName}" },
                commitDesiredSignatures = {
                    events += "commit"
                    false
                },
            )
            fail("Expected signature commit failure")
        } catch (failure: IllegalStateException) {
            assertEquals("비용 알림 예약 상태를 저장하지 못했어요", failure.message)
        }

        assertEquals(
            listOf("enqueue:$workName", "commit", "cancel:$workName"),
            events,
        )
    }

    @Test
    fun cancellationIsRethrownInsteadOfConvertedToFailureResult() = runBlocking {
        val cancellation = CancellationException("cancel sync")

        try {
            runCatchingPreservingCancellation<Unit> { throw cancellation }
            fail("Expected cancellation")
        } catch (caught: CancellationException) {
            assertSame(cancellation, caught)
        }
    }

    @Test
    fun nonCancellationFailureRemainsAResultFailure() = runBlocking {
        val result = runCatchingPreservingCancellation<Unit> { error("sync failed") }

        assertTrue(result.isFailure)
        assertEquals("sync failed", result.exceptionOrNull()?.message)
    }

    @Test
    fun syncGateSerializesAStaleTransactionBeforeTheLatestReconciliation() = runBlocking {
        val gate = ExpenseReminderSyncGate()
        val firstEntered = CompletableDeferred<Unit>()
        val releaseFirst = CompletableDeferred<Unit>()
        val events = mutableListOf<String>()

        val staleSync = async(start = CoroutineStart.UNDISPATCHED) {
            gate.run {
                events += "stale-start"
                firstEntered.complete(Unit)
                releaseFirst.await()
                events += "stale-end"
            }
        }
        firstEntered.await()
        val latestSync = async(start = CoroutineStart.UNDISPATCHED) {
            gate.run { events += "latest-reconcile" }
        }
        yield()

        assertEquals(listOf("stale-start"), events)
        releaseFirst.complete(Unit)
        joinAll(staleSync, latestSync)
        assertEquals(listOf("stale-start", "stale-end", "latest-reconcile"), events)
    }

    private fun initialPlan(vararg schedules: TravelSchedule) = planExpenseReminderSync(
        schedules = schedules.toList(),
        previouslyScheduledSignatures = emptyMap(),
        clock = beforeTrigger,
    )

    private fun schedule(
        id: String = "schedule-1",
        tripId: String = "trip-1",
        title: String = "점심",
        endTime: String? = "10:30",
    ) = TravelSchedule(
        id = id,
        tripId = tripId,
        title = title,
        date = "2026.08.13",
        time = "10:00",
        endTime = endTime,
        order = 0,
    )
}

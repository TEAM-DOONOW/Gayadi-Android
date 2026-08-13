package com.gayadi.android.notification

import com.gayadi.android.domain.model.TravelSchedule
import java.time.Clock

internal data class PlannedExpenseReminder(
    val tripId: String,
    val scheduleId: String,
    val scheduleTitle: String,
    val workName: String,
    val delayMillis: Long,
)

internal data class ExpenseReminderSyncPlan(
    val remindersToEnqueue: List<PlannedExpenseReminder>,
    val workNamesToCancel: Set<String>,
    val desiredSignatures: Map<String, String>,
)

/** Applies idempotent work changes before publishing their matching local ledger. */
internal fun applyExpenseReminderSyncPlan(
    plan: ExpenseReminderSyncPlan,
    cancelWork: (String) -> Unit,
    enqueueReminder: (PlannedExpenseReminder) -> Unit,
    commitDesiredSignatures: (Map<String, String>) -> Boolean,
) {
    plan.workNamesToCancel.forEach(cancelWork)
    plan.remindersToEnqueue.forEach(enqueueReminder)
    if (!commitDesiredSignatures(plan.desiredSignatures)) {
        plan.remindersToEnqueue.forEach { reminder -> cancelWork(reminder.workName) }
        error("비용 알림 예약 상태를 저장하지 못했어요")
    }
}

/**
 * Produces the reminder changes without touching Android storage or WorkManager.
 *
 * New and changed future reminders are later enqueued with a replace policy. An existing
 * reminder with the same signature is preserved, even after its trigger time, because the OS
 * may still be deferring that work.
 */
internal fun planExpenseReminderSync(
    schedules: List<TravelSchedule>,
    previouslyScheduledSignatures: Map<String, String?>,
    clock: Clock = Clock.systemDefaultZone(),
): ExpenseReminderSyncPlan {
    val nowMillis = clock.millis()
    val remindersToEnqueue = mutableListOf<PlannedExpenseReminder>()
    val workNamesToCancel = previouslyScheduledSignatures.keys.toMutableSet()
    val desiredSignatures = linkedMapOf<String, String>()

    schedules.forEach { schedule ->
        val triggerAtMillis = expenseReminderTriggerAtMillis(
            date = schedule.date,
            endTime = schedule.endTime,
            zoneId = clock.zone,
        ) ?: return@forEach
        val workName = expenseReminderWorkName(schedule.tripId, schedule.id)
        val signature = expenseReminderSignature(triggerAtMillis, schedule.title)
        val previousSignature = previouslyScheduledSignatures[workName]
        val isFuture = triggerAtMillis > nowMillis

        when {
            previousSignature == signature -> {
                // Keep the existing WorkManager job, including an overdue job deferred by the OS.
                workNamesToCancel.remove(workName)
                desiredSignatures[workName] = signature
            }

            isFuture -> {
                // A new or changed future reminder replaces any job with the same unique name.
                workNamesToCancel.remove(workName)
                desiredSignatures[workName] = signature
                remindersToEnqueue += PlannedExpenseReminder(
                    tripId = schedule.tripId,
                    scheduleId = schedule.id,
                    scheduleTitle = schedule.title,
                    workName = workName,
                    delayMillis = triggerAtMillis - nowMillis,
                )
            }

            else -> Unit // Never enqueue a newly discovered or changed reminder in the past.
        }
    }

    return ExpenseReminderSyncPlan(
        remindersToEnqueue = remindersToEnqueue,
        workNamesToCancel = workNamesToCancel,
        desiredSignatures = desiredSignatures,
    )
}

private fun expenseReminderSignature(triggerAtMillis: Long, scheduleTitle: String): String =
    "$triggerAtMillis:$scheduleTitle"

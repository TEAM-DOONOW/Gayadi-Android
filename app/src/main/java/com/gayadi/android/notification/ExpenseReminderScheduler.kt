package com.gayadi.android.notification

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.gayadi.android.domain.model.TravelSchedule
import java.time.Clock
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

private val processWideExpenseReminderSyncGate = ExpenseReminderSyncGate()

class ExpenseReminderScheduler(
    context: Context,
    private val workManager: WorkManager = WorkManager.getInstance(context.applicationContext),
    private val clock: Clock = Clock.systemDefaultZone(),
) {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE,
    )

    suspend fun sync(schedules: List<TravelSchedule>): Result<Unit> = runCatchingPreservingCancellation {
        withContext(Dispatchers.IO) {
            processWideExpenseReminderSyncGate.run {
                val previouslyScheduledNames = preferences
                    .getStringSet(SCHEDULED_NAMES_KEY, emptySet())
                    .orEmpty()
                    .toSet()
                val persistedSignatures = previouslyScheduledNames.associateWith { workName ->
                    preferences.getString(signaturePreferenceKey(workName), null)
                }
                val previouslyScheduledSignatures = reconcileExpenseReminderSignatures(
                    persistedSignatures = persistedSignatures,
                    hasActiveWork = { workName ->
                        workManager.getWorkInfosForUniqueWork(workName).get().any { workInfo ->
                            !workInfo.state.isFinished
                        }
                    },
                )
                val plan = planExpenseReminderSync(schedules, previouslyScheduledSignatures, clock)

                applyExpenseReminderSyncPlan(
                    plan = plan,
                    cancelWork = { workName ->
                        workManager.cancelUniqueWork(workName).result.get()
                    },
                    enqueueReminder = { reminder ->
                        workManager.enqueueUniqueWork(
                            reminder.workName,
                            ExistingWorkPolicy.REPLACE,
                            expenseReminderWorkRequest(reminder),
                        ).result.get()
                    },
                    commitDesiredSignatures = { desiredSignatures ->
                        preferences.edit().apply {
                            (previouslyScheduledNames - desiredSignatures.keys).forEach { workName ->
                                remove(signaturePreferenceKey(workName))
                            }
                            desiredSignatures.forEach { (workName, signature) ->
                                putString(signaturePreferenceKey(workName), signature)
                            }
                            putStringSet(SCHEDULED_NAMES_KEY, desiredSignatures.keys.toSet())
                        }.commit()
                    },
                )
            }
        }
    }

    suspend fun cancelAll(): Result<Unit> = runCatchingPreservingCancellation {
        withContext(Dispatchers.IO) {
            processWideExpenseReminderSyncGate.run {
                workManager.cancelAllWorkByTag(EXPENSE_REMINDER_TAG).result.get()
                val scheduledNames = preferences.getStringSet(SCHEDULED_NAMES_KEY, emptySet()).orEmpty()
                val committed = preferences.edit().apply {
                    scheduledNames.forEach { workName -> remove(signaturePreferenceKey(workName)) }
                    remove(SCHEDULED_NAMES_KEY)
                }.commit()
                check(committed) { "비용 알림 예약 상태를 지우지 못했어요" }
            }
        }
    }

    private fun signaturePreferenceKey(workName: String) = "$SIGNATURE_KEY_PREFIX$workName"

    private companion object {
        const val PREFERENCES_NAME = "expense-reminder-scheduler"
        const val SCHEDULED_NAMES_KEY = "scheduled-work-names"
        const val SIGNATURE_KEY_PREFIX = "scheduled-signature:"
    }
}

internal fun expenseReminderWorkRequest(
    reminder: PlannedExpenseReminder,
) = OneTimeWorkRequestBuilder<ExpenseReminderWorker>()
    .setInitialDelay(reminder.delayMillis, TimeUnit.MILLISECONDS)
    .setInputData(
        Data.Builder()
            .putString(KEY_TRIP_ID, reminder.tripId)
            .putString(KEY_SCHEDULE_ID, reminder.scheduleId)
            .putString(KEY_SCHEDULE_TITLE, reminder.scheduleTitle)
            .build(),
    )
    .addTag(EXPENSE_REMINDER_TAG)
    .addTag(reminder.workName)
    .build()

/**
 * Retries transient reminder-store or WorkManager failures while the calling UI effect is alive.
 * A new schedule snapshot cancels that effect (and this delay) before starting its own sync.
 */
internal suspend fun syncExpenseRemindersWithRetry(
    sync: suspend () -> Result<Unit>,
    retryDelaysMillis: Sequence<Long> = generateSequence(1_000L) { previous ->
        (previous * 2L).coerceAtMost(60_000L)
    },
    delayBeforeRetry: suspend (Long) -> Unit = { delay(it) },
) {
    var result = sync()
    val delays = retryDelaysMillis.iterator()
    while (result.isFailure && delays.hasNext()) {
        delayBeforeRetry(delays.next())
        result = sync()
    }
}

internal suspend fun <T> runCatchingPreservingCancellation(
    block: suspend () -> T,
): Result<T> = try {
    Result.success(block())
} catch (failure: CancellationException) {
    throw failure
} catch (failure: Throwable) {
    Result.failure(failure)
}

internal class ExpenseReminderSyncGate {
    private val mutex = Mutex()

    suspend fun <T> run(block: suspend () -> T): T = mutex.withLock { block() }
}

/**
 * Discards persisted signatures whose WorkManager job is no longer active.
 *
 * The signature ledger is only a scheduling optimization, not the source of truth. In particular,
 * [ExpenseReminderScheduler.cancelAll] can successfully cancel WorkManager jobs and then fail to
 * clear this ledger. Reconciling both stores makes the next sync enqueue each still-future reminder
 * again instead of treating a stale signature as proof that its job still exists.
 */
internal fun reconcileExpenseReminderSignatures(
    persistedSignatures: Map<String, String?>,
    hasActiveWork: (String) -> Boolean,
): Map<String, String?> = persistedSignatures.filterKeys(hasActiveWork)

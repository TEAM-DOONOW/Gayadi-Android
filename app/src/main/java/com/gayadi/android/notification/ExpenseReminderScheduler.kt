package com.gayadi.android.notification

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.gayadi.android.domain.model.TravelSchedule
import java.time.Clock
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class ExpenseReminderScheduler(
    context: Context,
    private val workManager: WorkManager = WorkManager.getInstance(context.applicationContext),
    private val clock: Clock = Clock.systemDefaultZone(),
) {
    private val syncGate = ExpenseReminderSyncGate()
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE,
    )

    suspend fun sync(schedules: List<TravelSchedule>): Result<Unit> = runCatching {
        withContext(Dispatchers.IO) {
            syncGate.run {
                val previouslyScheduledNames = preferences
                    .getStringSet(SCHEDULED_NAMES_KEY, emptySet())
                    .orEmpty()
                    .toSet()
                val previouslyScheduledSignatures = previouslyScheduledNames.associateWith { workName ->
                    preferences.getString(signaturePreferenceKey(workName), null)
                }
                val plan = planExpenseReminderSync(schedules, previouslyScheduledSignatures, clock)

                applyExpenseReminderSyncPlan(
                    plan = plan,
                    cancelWork = { workName ->
                        workManager.cancelUniqueWork(workName).result.get()
                    },
                    enqueueReminder = { reminder ->
                        val request = OneTimeWorkRequestBuilder<ExpenseReminderWorker>()
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
                        workManager.enqueueUniqueWork(
                            reminder.workName,
                            ExistingWorkPolicy.REPLACE,
                            request,
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

    suspend fun cancelAll(): Result<Unit> = runCatching {
        withContext(Dispatchers.IO) {
            syncGate.run {
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

internal class ExpenseReminderSyncGate {
    private val mutex = Mutex()

    suspend fun <T> run(block: suspend () -> T): T = mutex.withLock { block() }
}

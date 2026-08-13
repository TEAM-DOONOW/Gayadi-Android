package com.gayadi.android.notification

import android.Manifest
import android.app.Notification
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.ListenableWorker
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import androidx.work.testing.TestListenableWorkerBuilder
import com.gayadi.android.MainActivity
import com.gayadi.android.domain.model.TravelSchedule
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExpenseReminderIntegrationTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val appContext = instrumentation.targetContext
    private val workManager = WorkManager.getInstance(appContext)
    private val workNamesToCleanUp = mutableSetOf<String>()
    private lateinit var isolatedContext: IsolatedPreferencesContext
    private lateinit var scheduler: ExpenseReminderScheduler

    @Before
    fun setUp() {
        isolatedContext = IsolatedPreferencesContext(
            base = appContext,
            prefix = "expense-reminder-test-${UUID.randomUUID()}:",
        )
        scheduler = ExpenseReminderScheduler(
            context = isolatedContext,
            workManager = workManager,
            clock = Clock.fixed(
                Instant.parse("2026-08-13T01:00:00Z"),
                ZoneId.of("Asia/Seoul"),
            ),
        )
    }

    @After
    fun tearDown() {
        workNamesToCleanUp.forEach { workName ->
            workManager.cancelUniqueWork(workName).result.get(10, TimeUnit.SECONDS)
        }
        isolatedContext.clearIsolatedPreferences()
    }

    @Test
    fun schedulerCreatesReplacesCancelsAndRecoversUniqueWork() = runBlocking {
        val token = UUID.randomUUID().toString()
        val original = schedule(
            tripId = "trip-$token",
            id = "schedule-$token",
            title = "점심",
            endTime = "10:30",
        )
        val workName = expenseReminderWorkName(original.tripId, original.id)
        workNamesToCleanUp += workName

        scheduler.sync(listOf(original)).getOrThrow()

        val first = activeWork(workName)
        assertEquals(30 * 60 * 1_000L, first.initialDelayMillis)
        assertTrue(EXPENSE_REMINDER_TAG in first.tags)
        assertTrue(workName in first.tags)

        scheduler.sync(listOf(original.copy(title = "늦은 점심", endTime = "11:15"))).getOrThrow()

        val replaced = activeWork(workName)
        assertNotEquals(first.id, replaced.id)
        assertEquals(75 * 60 * 1_000L, replaced.initialDelayMillis)

        scheduler.sync(emptyList()).getOrThrow()
        assertTrue(activeWorks(workName).isEmpty())

        scheduler.sync(listOf(original)).getOrThrow()
        val beforeExternalCancellation = activeWork(workName)
        workManager.cancelUniqueWork(workName).result.get(10, TimeUnit.SECONDS)
        assertTrue(activeWorks(workName).isEmpty())

        // The scheduler's signature still exists, so this proves reconciliation trusts live work.
        scheduler.sync(listOf(original)).getOrThrow()
        val recovered = activeWork(workName)
        assertNotEquals(beforeExternalCancellation.id, recovered.id)
        assertEquals(30 * 60 * 1_000L, recovered.initialDelayMillis)
    }

    @Test
    @Suppress("RestrictedApi")
    fun workRequestCarriesWorkerInputDelayAndBothLifecycleTags() {
        val reminder = PlannedExpenseReminder(
            tripId = "trip/서울",
            scheduleId = "schedule 1",
            scheduleTitle = "저녁 식사",
            workName = expenseReminderWorkName("trip/서울", "schedule 1"),
            delayMillis = 42_000L,
        )

        val request = expenseReminderWorkRequest(reminder)

        assertEquals(ExpenseReminderWorker::class.java.name, request.workSpec.workerClassName)
        assertEquals(42_000L, request.workSpec.initialDelay)
        assertEquals("trip/서울", request.workSpec.input.getString(KEY_TRIP_ID))
        assertEquals("schedule 1", request.workSpec.input.getString(KEY_SCHEDULE_ID))
        assertEquals("저녁 식사", request.workSpec.input.getString(KEY_SCHEDULE_TITLE))
        assertTrue(EXPENSE_REMINDER_TAG in request.tags)
        assertTrue(reminder.workName in request.tags)
    }

    @Test
    fun workerPostsReminderChannelAndExplicitExpenseDeepLink() = runBlocking {
        grantNotificationPermissionIfNeeded()
        val token = UUID.randomUUID().toString()
        val tripId = "trip/$token"
        val scheduleId = "schedule $token"
        val notificationId = expenseReminderNotificationId(tripId, scheduleId)
        val notificationManager = appContext.getSystemService(NotificationManager::class.java)
        notificationManager.cancel(notificationId)

        try {
            val worker = TestListenableWorkerBuilder<ExpenseReminderWorker>(appContext)
                .setInputData(
                    workDataOf(
                        KEY_TRIP_ID to tripId,
                        KEY_SCHEDULE_ID to scheduleId,
                        KEY_SCHEDULE_TITLE to "한강 산책",
                    ),
                )
                .build()

            val result = worker.doWork()

            assertTrue(result is ListenableWorker.Result.Success)
            val channel = notificationManager.getNotificationChannel(EXPENSE_REMINDER_CHANNEL_ID)
            assertNotNull(channel)
            assertEquals("일정 비용 알림", channel.name.toString())
            assertEquals(NotificationManager.IMPORTANCE_DEFAULT, channel.importance)

            val posted = notificationManager.activeNotifications
                .single { it.id == notificationId }
                .notification
            assertEquals(EXPENSE_REMINDER_CHANNEL_ID, posted.channelId)
            assertEquals("일정이 끝났어요", posted.extras.getString(Notification.EXTRA_TITLE))
            assertEquals(
                "한강 산책 비용을 기록해 보세요",
                posted.extras.getString(Notification.EXTRA_TEXT),
            )
            assertTrue(posted.flags and Notification.FLAG_AUTO_CANCEL != 0)
            assertNotNull(posted.contentIntent)

            val deepLinkIntent = expenseReminderDeepLinkIntent(appContext, tripId, scheduleId)
            assertEquals(Intent.ACTION_VIEW, deepLinkIntent.action)
            assertEquals(expenseReminderUri(tripId, scheduleId), deepLinkIntent.dataString)
            assertEquals(ComponentName(appContext, MainActivity::class.java), deepLinkIntent.component)
            assertTrue(deepLinkIntent.flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0)
            assertTrue(deepLinkIntent.flags and Intent.FLAG_ACTIVITY_CLEAR_TOP != 0)

            val implicitDeepLink = Intent(Intent.ACTION_VIEW, deepLinkIntent.data)
                .setPackage(appContext.packageName)
            val resolved = requireNotNull(
                appContext.packageManager.resolveActivity(
                    implicitDeepLink,
                    PackageManager.MATCH_DEFAULT_ONLY,
                ),
            )
            assertEquals(MainActivity::class.java.name, resolved.activityInfo.name)
            assertTrue(resolved.activityInfo.exported)
        } finally {
            notificationManager.cancel(notificationId)
        }
    }

    private fun activeWork(workName: String): WorkInfo = activeWorks(workName).single()

    private fun activeWorks(workName: String): List<WorkInfo> = workManager
        .getWorkInfosForUniqueWork(workName)
        .get(10, TimeUnit.SECONDS)
        .filterNot { it.state.isFinished }

    private fun grantNotificationPermissionIfNeeded() {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            appContext.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            instrumentation.uiAutomation.grantRuntimePermission(
                appContext.packageName,
                Manifest.permission.POST_NOTIFICATIONS,
            )
        }
    }

    private fun schedule(
        id: String,
        tripId: String,
        title: String,
        endTime: String,
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

private class IsolatedPreferencesContext(
    base: Context,
    private val prefix: String,
) : ContextWrapper(base) {
    override fun getApplicationContext(): Context = this

    override fun getSharedPreferences(name: String, mode: Int) =
        super.getSharedPreferences("$prefix$name", mode)

    fun clearIsolatedPreferences() {
        getSharedPreferences("expense-reminder-scheduler", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }
}

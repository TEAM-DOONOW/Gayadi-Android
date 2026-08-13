package com.gayadi.android.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.gayadi.android.MainActivity
import com.gayadi.android.R

class ExpenseReminderWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        if (!notificationsAllowed(applicationContext)) return Result.success()
        val tripId = inputData.getString(KEY_TRIP_ID) ?: return Result.failure()
        val scheduleId = inputData.getString(KEY_SCHEDULE_ID) ?: return Result.failure()
        val scheduleTitle = inputData.getString(KEY_SCHEDULE_TITLE).orEmpty()

        createNotificationChannel(applicationContext)
        val deepLinkIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse(expenseReminderUri(tripId, scheduleId)),
            applicationContext,
            MainActivity::class.java,
        ).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            expenseReminderNotificationId(tripId, scheduleId),
            deepLinkIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(applicationContext, EXPENSE_REMINDER_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("일정이 끝났어요")
            .setContentText(
                if (scheduleTitle.isBlank()) "비용을 기록해 보세요" else "$scheduleTitle 비용을 기록해 보세요",
            )
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        return try {
            NotificationManagerCompat.from(applicationContext).notify(
                expenseReminderNotificationId(tripId, scheduleId),
                notification,
            )
            Result.success()
        } catch (_: SecurityException) {
            Result.success()
        }
    }
}

private fun notificationsAllowed(context: Context): Boolean {
    val runtimePermissionGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED
    return runtimePermissionGranted && NotificationManagerCompat.from(context).areNotificationsEnabled()
}

private fun createNotificationChannel(context: Context) {
    val channel = NotificationChannel(
        EXPENSE_REMINDER_CHANNEL_ID,
        "일정 비용 알림",
        NotificationManager.IMPORTANCE_DEFAULT,
    ).apply {
        description = "일정이 끝나면 비용 입력을 알려드려요"
    }
    context.getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
}

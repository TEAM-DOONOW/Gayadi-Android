package com.gayadi.android.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.gayadi.android.MainActivity
import com.gayadi.android.R
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.flow.MutableSharedFlow

class GayadiMessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        getSharedPreferences(PREFERENCES, MODE_PRIVATE).edit().putString(REFRESHED_TOKEN, token).apply()
        tokenUpdates.tryEmit(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val title = message.notification?.title ?: message.data["title"] ?: return
        val body = message.notification?.body ?: message.data["body"].orEmpty()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(CHANNEL, "여행 알림", NotificationManager.IMPORTANCE_DEFAULT))
        val intent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        manager.notify(
            message.data["notificationId"]?.toIntOrNull() ?: title.hashCode(),
            NotificationCompat.Builder(this, CHANNEL)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(body)
                .setAutoCancel(true)
                .setContentIntent(intent)
                .build(),
        )
    }

    companion object {
        const val PREFERENCES = "push-notifications"
        const val REFRESHED_TOKEN = "refreshed-token"
        val tokenUpdates = MutableSharedFlow<String>(extraBufferCapacity = 1)
        private const val CHANNEL = "gayadi-events"
    }
}

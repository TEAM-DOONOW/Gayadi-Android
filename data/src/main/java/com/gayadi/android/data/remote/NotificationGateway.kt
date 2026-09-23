package com.gayadi.android.data.remote

import com.gayadi.android.data.datasource.GayadiApiClient
import org.json.JSONArray
import org.json.JSONObject

data class InboxNotification(
    val id: Long,
    val type: String,
    val title: String,
    val content: String,
    val tripId: Long?,
    val isRead: Boolean,
    val createdAt: String,
)

class NotificationGateway(private val api: GayadiApiClient) {
    suspend fun list(): List<InboxNotification> {
        val array = JSONArray(api.request("GET", "/api/v1/notifications?limit=100"))
        return (0 until array.length()).map { index ->
            val item = array.getJSONObject(index)
            InboxNotification(
                id = item.getLong("id"),
                type = item.getString("type"),
                title = item.getString("title"),
                content = item.getString("content"),
                tripId = item.optLong("tripId").takeIf { !item.isNull("tripId") },
                isRead = item.getBoolean("isRead"),
                createdAt = item.optString("createdAt"),
            )
        }
    }

    suspend fun markRead(id: Long) {
        api.request("PUT", "/api/v1/notifications/$id/read")
    }

    suspend fun registerToken(token: String) {
        api.request("PUT", "/api/v1/notifications/device-token", JSONObject().put("token", token).toString())
    }
}

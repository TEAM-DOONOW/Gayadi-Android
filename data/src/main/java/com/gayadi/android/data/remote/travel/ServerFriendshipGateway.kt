package com.gayadi.android.data.remote.travel

import com.gayadi.android.data.datasource.GayadiApiClient
import com.gayadi.android.domain.repository.*
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder

class ServerFriendshipGateway(private val api: GayadiApiClient) : FriendshipGateway {
    override suspend fun list(): List<Friendship> = buildList {
        var offset = 0
        do {
            val page = JSONArray(api.request("GET", "/api/v1/friendships?limit=100&offset=$offset"))
            repeat(page.length()) { i ->
                val row = page.getJSONObject(i)
                add(Friendship(row.getLong("id").toString(), user(row.getJSONObject("user")),
                    row.getString("status"), row.getBoolean("requestedByMe"), row.getBoolean("canDecide"), row.getInt("version")))
            }
            offset += page.length()
        } while (page.length() == 100)
    }
    override suspend fun search(query: String): List<FriendshipUser> {
        require(query.trim().length in 1..100)
        val rows = JSONArray(api.request("GET", "/api/v1/users?query=${URLEncoder.encode(query.trim(), "UTF-8")}&limit=30"))
        return (0 until rows.length()).map { user(rows.getJSONObject(it)) }
    }
    override suspend fun request(userId: String) {
        api.request("POST", "/api/v1/friendships", JSONObject().put("targetUserId", id(userId)).toString())
    }
    override suspend fun decide(friendship: Friendship, accept: Boolean) {
        api.request("PATCH", "/api/v1/friendships/${id(friendship.id)}", JSONObject()
            .put("status", if (accept) "ACCEPTED" else "REJECTED").put("version", friendship.version).toString())
    }
    override suspend fun delete(friendshipId: String) {
        api.request("DELETE", "/api/v1/friendships/${id(friendshipId)}")
    }
    private fun id(value: String) = requireNotNull(value.toLongOrNull()?.takeIf { it > 0 })
    private fun user(row: JSONObject) = FriendshipUser(row.getLong("id").toString(), row.getString("nickname"))
}

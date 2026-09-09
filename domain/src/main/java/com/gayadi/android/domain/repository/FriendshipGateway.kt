package com.gayadi.android.domain.repository

data class FriendshipUser(val id: String, val nickname: String)
data class Friendship(val id: String, val user: FriendshipUser, val status: String,
    val requestedByMe: Boolean, val canDecide: Boolean, val version: Int)
interface FriendshipGateway {
    suspend fun list(): List<Friendship>
    suspend fun search(query: String): List<FriendshipUser>
    suspend fun request(userId: String)
    suspend fun decide(friendship: Friendship, accept: Boolean)
    suspend fun delete(friendshipId: String)
}

package com.gayadi.android.data

import com.gayadi.android.data.datasource.*
import com.gayadi.android.data.remote.travel.ServerFriendshipGateway
import com.gayadi.android.domain.repository.*
import com.gayadi.android.domain.model.*
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.*
import org.json.JSONObject
import org.junit.*
import org.junit.Assert.*

class ServerFriendshipGatewayTest {
    private lateinit var server: MockWebServer
    private lateinit var gateway: ServerFriendshipGateway
    @Before fun setup() {
        server = MockWebServer().apply { start() }
        val auth = object : AuthRepository {
            override fun currentSession(): AuthSession? = null
            override fun clearSession() {}
            override suspend fun validAccessToken() = "test-token"
            override suspend fun refreshSession(): AuthSession = error("Unused")
            override suspend fun signInWithGoogle(idToken: String): AuthSession = error("Unused")
        }
        gateway = ServerFriendshipGateway(GayadiApiClient(server.url("/").toString(), auth))
    }
    @After fun teardown() { server.shutdown() }
    @Test fun searchEncodesInputAndAuthenticates() = runTest {
        server.enqueue(MockResponse().setBody("[{\"id\":2,\"nickname\":\"친구\"}]"))
        assertEquals("2", gateway.search("서울 & 친구").single().id)
        val request = server.takeRequest()
        assertEquals("서울 & 친구", request.requestUrl!!.queryParameter("query"))
        assertEquals("Bearer test-token", request.getHeader("Authorization"))
    }
    @Test fun acceptingRequestSendsVersionAndDoesNotSwallowConflict() = runTest {
        server.enqueue(MockResponse().setResponseCode(409))
        val relation = Friendship("5", FriendshipUser("2", "친구"), "PENDING", false, true, 7)
        assertTrue(runCatching { gateway.decide(relation, true) }.exceptionOrNull() is GayadiApiException)
        val request = server.takeRequest()
        assertEquals("PATCH", request.method)
        assertEquals("/api/v1/friendships/5", request.path)
        val body = JSONObject(request.body.readUtf8())
        assertEquals(7, body.getInt("version"))
        assertEquals("ACCEPTED", body.getString("status"))
    }
}

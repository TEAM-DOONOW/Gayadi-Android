package com.gayadi.android.data

import com.gayadi.android.data.remote.ApiException
import com.gayadi.android.data.remote.GayadiHttpClient
import com.gayadi.android.data.remote.RecordingHttpURLConnection
import com.gayadi.android.data.remote.RecordingTokenStore
import com.gayadi.android.data.repository.RemoteAuthRepository
import java.net.HttpURLConnection
import java.net.URL
import java.util.ArrayDeque
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RemoteAuthRepositoryTest {
    @Test
    fun signupMapsSessionAndPersistsAccessToken() = runTest {
        val tokenStore = RecordingTokenStore()
        val connection = RecordingHttpURLConnection(
            URL("https://example.com"),
            statusCode = HttpURLConnection.HTTP_CREATED,
            responseBody = authResponse("new-token"),
        )
        val repository = RemoteAuthRepository(client(tokenStore, connection), tokenStore)

        val session = repository.signup("USER@example.com", "password1", "여행자").getOrThrow()

        assertEquals("new-token", session.accessToken)
        assertEquals(12L, session.user.id)
        assertEquals("여행자", session.user.nickname)
        assertEquals("new-token", tokenStore.token)
        assertEquals("/api/v1/auth/registrations", connection.url.path)
        val body = JSONObject(connection.writtenBody)
        assertEquals("USER@example.com", body.getString("email"))
        assertEquals("password1", body.getString("password"))
        assertEquals("여행자", body.getString("nickname"))
        assertNull(connection.getRequestProperty("Authorization"))
    }

    @Test
    fun loginParseFailureDoesNotReplaceExistingToken() = runTest {
        val tokenStore = RecordingTokenStore("existing-token")
        val connection = RecordingHttpURLConnection(
            URL("https://example.com"),
            responseBody = """{"tokenType":"Bearer","expiresIn":7200,"user":{}}""",
        )
        val repository = RemoteAuthRepository(client(tokenStore, connection), tokenStore)

        val result = repository.login("user@example.com", "password1")

        assertTrue(result.isFailure)
        assertEquals("existing-token", tokenStore.token)
    }

    @Test
    fun currentMapsProfileFields() = runTest {
        val tokenStore = RecordingTokenStore("token")
        val connection = RecordingHttpURLConnection(
            URL("https://example.com"),
            responseBody = """
                {
                  "id":12,
                  "email":"user@example.com",
                  "nickname":"여행자",
                  "introduction":"느긋한 여행",
                  "profileImageUrl":"https://example.com/profile.png",
                  "resultCode":"PNR",
                  "travelStyleName":"산책가",
                  "characterKey":"character_pnr",
                  "strengths":["계획"],
                  "weaknesses":["지연"]
                }
            """.trimIndent(),
        )
        val repository = RemoteAuthRepository(client(tokenStore, connection), tokenStore)

        val user = repository.current().getOrThrow()

        assertEquals("PNR", user.resultCode)
        assertEquals(listOf("계획"), user.strengths)
        assertEquals("Bearer token", connection.getRequestProperty("Authorization"))
    }

    @Test
    fun withdrawKeepsTokenWhenServerRejectsRequest() = runTest {
        val tokenStore = RecordingTokenStore("token")
        val connection = RecordingHttpURLConnection(
            URL("https://example.com"),
            statusCode = HttpURLConnection.HTTP_CONFLICT,
            responseBody = """{"status":409,"code":"CONFLICT","message":"여행을 먼저 취소해 주세요."}""",
        )
        val repository = RemoteAuthRepository(client(tokenStore, connection), tokenStore)

        val result = repository.withdraw()

        assertTrue(result.exceptionOrNull() is ApiException)
        assertEquals("token", tokenStore.token)
    }

    @Test
    fun logoutClearsOnlyLocalToken() = runTest {
        val tokenStore = RecordingTokenStore("token")
        var connectionOpened = false
        val client = GayadiHttpClient(
            "https://example.com",
            tokenStore,
            connectionFactory = {
                connectionOpened = true
                RecordingHttpURLConnection(it)
            },
            ioDispatcher = Dispatchers.Unconfined,
        )

        RemoteAuthRepository(client, tokenStore).logout().getOrThrow()

        assertNull(tokenStore.token)
        assertEquals(1, tokenStore.clearCount)
        assertTrue(!connectionOpened)
    }

    private fun client(
        tokenStore: RecordingTokenStore,
        vararg connections: RecordingHttpURLConnection,
    ): GayadiHttpClient {
        val queue = ArrayDeque(connections.toList())
        return GayadiHttpClient(
            "https://example.com",
            tokenStore,
            connectionFactory = { requestedUrl ->
                queue.removeFirst().withUrl(requestedUrl)
            },
            ioDispatcher = Dispatchers.Unconfined,
        )
    }

    private fun authResponse(token: String) = """
        {
          "accessToken":"$token",
          "tokenType":"Bearer",
          "expiresIn":7200,
          "user":{
            "id":12,
            "email":"user@example.com",
            "nickname":"여행자",
            "introduction":null,
            "profile_image_url":null,
            "status":"ACTIVE"
          }
        }
    """.trimIndent()
}

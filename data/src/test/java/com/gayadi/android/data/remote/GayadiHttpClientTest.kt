package com.gayadi.android.data.remote

import java.io.File
import java.net.HttpURLConnection
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class GayadiHttpClientTest {
    @Test
    fun getObject_encodesQueryAndAddsBearerToken() = runTest {
        val connection = RecordingHttpURLConnection(
            url = java.net.URL("https://example.com"),
            responseBody = """{"value":"ok"}""",
        )
        val client = client(RecordingTokenStore("jwt-token"), connection)

        val response = client.getObject(
            path = "/api/v1/items",
            query = linkedMapOf("keyword" to "서울 & 제주", "cursor" to null),
        )

        assertEquals("ok", response.getString("value"))
        assertEquals("GET", connection.requestMethod)
        assertEquals("Bearer jwt-token", connection.getRequestProperty("Authorization"))
        assertEquals(mapOf("keyword" to "서울 & 제주"), parseQuery(connection.url.query))
        assertTrue(connection.wasDisconnected)
    }

    @Test
    fun postObject_sendsJsonWithoutTokenForPublicEndpoint() = runTest {
        val connection = RecordingHttpURLConnection(
            url = java.net.URL("https://example.com"),
            statusCode = HttpURLConnection.HTTP_CREATED,
            responseBody = """{"id":7}""",
        )
        val client = client(RecordingTokenStore("must-not-leak"), connection)

        val response = client.postObject(
            path = "/api/v1/auth/registrations",
            body = JSONObject().put("email", "user@example.com"),
            authenticated = false,
        )

        assertEquals(7L, response.getLong("id"))
        assertEquals("POST", connection.requestMethod)
        assertNull(connection.getRequestProperty("Authorization"))
        assertEquals("user@example.com", JSONObject(connection.writtenBody).getString("email"))
        assertEquals("application/json", connection.getRequestProperty("Content-Type"))
    }

    @Test
    fun patchArray_parsesBareArrayResponse() = runTest {
        val connection = RecordingHttpURLConnection(
            url = java.net.URL("https://example.com"),
            responseBody = """[{"id":1},{"id":2}]""",
        )
        val client = client(RecordingTokenStore("jwt-token"), connection)

        val response = client.patchArray(
            path = "/api/v1/trips/3/schedule-orders",
            body = JSONObject().put("orderedIds", org.json.JSONArray().put(2).put(1)),
        )

        assertEquals("PATCH", connection.requestMethod)
        assertEquals(2, response.length())
        assertEquals(2L, response.getJSONObject(1).getLong("id"))
    }

    @Test
    fun structured401_throwsApiExceptionAndClearsStaleToken() = runTest {
        val tokenStore = RecordingTokenStore("expired-token")
        val connection = RecordingHttpURLConnection(
            url = java.net.URL("https://example.com"),
            statusCode = HttpURLConnection.HTTP_UNAUTHORIZED,
            responseBody = """
                {
                  "timestamp":"2026-08-30T01:02:03Z",
                  "status":401,
                  "code":"UNAUTHORIZED",
                  "message":"토큰이 만료되었습니다.",
                  "path":"/api/v1/users/current",
                  "traceId":"trace-1",
                  "details":{"token":"expired"}
                }
            """.trimIndent(),
        )
        val client = client(tokenStore, connection)

        val exception = expectApiException {
            client.getObject("/api/v1/users/current")
        }

        assertEquals(401, exception.statusCode)
        assertEquals("UNAUTHORIZED", exception.error.code)
        assertEquals("토큰이 만료되었습니다.", exception.message)
        assertEquals("expired", exception.error.details["token"])
        assertEquals("trace-1", exception.error.traceId)
        assertNull(tokenStore.token)
        assertEquals(1, tokenStore.clearCount)
    }

    @Test
    fun malformedError_usesFallbackWithoutClearingToken() = runTest {
        val tokenStore = RecordingTokenStore("valid-token")
        val connection = RecordingHttpURLConnection(
            url = java.net.URL("https://example.com"),
            statusCode = HttpURLConnection.HTTP_INTERNAL_ERROR,
            responseBody = "not-json",
        )
        val client = client(tokenStore, connection)

        val exception = expectApiException { client.getObject("/api/v1/failure") }

        assertEquals("HTTP_500", exception.error.code)
        assertEquals("요청을 처리하지 못했습니다. (HTTP 500)", exception.message)
        assertEquals("valid-token", tokenStore.token)
    }

    @Test
    fun delete_accepts204WithNoBody() = runTest {
        val connection = RecordingHttpURLConnection(
            url = java.net.URL("https://example.com"),
            statusCode = HttpURLConnection.HTTP_NO_CONTENT,
            responseBody = "",
        )
        val client = client(RecordingTokenStore("jwt-token"), connection)

        client.delete("/api/v1/users/current")

        assertEquals("DELETE", connection.requestMethod)
        assertTrue(connection.wasDisconnected)
    }

    @Test
    fun authenticatedRequestWithoutTokenDoesNotOpenConnection() = runTest {
        var opened = false
        val client = GayadiHttpClient(
            baseUrl = "https://example.com",
            tokenStore = RecordingTokenStore(),
            connectionFactory = {
                opened = true
                RecordingHttpURLConnection(it)
            },
            ioDispatcher = Dispatchers.Unconfined,
        )

        val exception = expectApiException { client.getObject("/api/v1/users/current") }

        assertEquals(401, exception.statusCode)
        assertEquals("로그인이 필요합니다.", exception.message)
        assertFalse(opened)
    }

    @Test
    fun fileTokenStorePersistsAndClearsToken() {
        val file = File.createTempFile("gayadi-token", ".test").also { it.delete() }
        try {
            val first = FileTokenStore(file)
            first.writeAccessToken(" token-value ")

            assertEquals("token-value", FileTokenStore(file).readAccessToken())

            first.clearAccessToken()
            assertNull(first.readAccessToken())
            assertFalse(file.exists())
        } finally {
            file.delete()
        }
    }

    private fun client(
        tokenStore: TokenStore,
        connection: RecordingHttpURLConnection,
    ) = GayadiHttpClient(
        baseUrl = "https://example.com/",
        tokenStore = tokenStore,
        connectionFactory = { requestedUrl ->
            connection.withUrl(requestedUrl)
        },
        ioDispatcher = Dispatchers.Unconfined,
    )

    private suspend fun expectApiException(block: suspend () -> Unit): ApiException = try {
        block()
        fail("Expected ApiException")
        error("unreachable")
    } catch (exception: ApiException) {
        exception
    }

    private fun parseQuery(rawQuery: String?): Map<String, String> =
        rawQuery.orEmpty()
            .split('&')
            .filter(String::isNotBlank)
            .associate { parameter ->
                val parts = parameter.split('=', limit = 2)
                URLDecoder.decode(parts[0], StandardCharsets.UTF_8.name()) to
                    URLDecoder.decode(parts.getOrElse(1) { "" }, StandardCharsets.UTF_8.name())
            }
}

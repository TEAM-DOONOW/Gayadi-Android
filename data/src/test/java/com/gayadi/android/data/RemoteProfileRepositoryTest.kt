package com.gayadi.android.data

import com.gayadi.android.data.remote.GayadiHttpClient
import com.gayadi.android.data.remote.RecordingHttpURLConnection
import com.gayadi.android.data.remote.RecordingTokenStore
import com.gayadi.android.data.repository.RemoteProfileRepository
import com.gayadi.android.domain.model.BasicInfo
import com.gayadi.android.domain.model.SurveyResult
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

class RemoteProfileRepositoryTest {
    @Test
    fun saveBasicInfoPatchesCurrentProfile() = runTest {
        val connection = RecordingHttpURLConnection(
            URL("https://example.com"),
            responseBody = profileResponse(),
        )
        val repository = repository(RecordingTokenStore("token"), connection)

        repository.saveBasicInfo(BasicInfo("새 닉네임", "새 소개"))

        assertEquals("PATCH", connection.requestMethod)
        assertEquals("/api/v1/users/current", connection.url.path)
        val body = JSONObject(connection.writtenBody)
        assertEquals("새 닉네임", body.getString("nickname"))
        assertEquals("새 소개", body.getString("introduction"))
    }

    @Test
    fun saveSurveyResultSucceedsOnlyAfterServerProfileReflectsResult() = runTest {
        val successConnection = RecordingHttpURLConnection(
            URL("https://example.com"),
            responseBody = profileResponse(resultCode = "PNR"),
        )
        val failureConnection = RecordingHttpURLConnection(
            URL("https://example.com"),
            responseBody = profileResponse(resultCode = null),
        )
        val result = SurveyResult(
            code = "PNR",
            emoji = "",
            name = "산책가",
            summary = "설명",
            hashtags = emptyList(),
            strengths = listOf("계획"),
            weaknesses = listOf("지연"),
            characterKey = "character_pnr",
        )

        assertTrue(repository(RecordingTokenStore("token"), successConnection).saveSurveyResult(result).isSuccess)
        val failure = repository(RecordingTokenStore("token"), failureConnection).saveSurveyResult(result)
        assertEquals(
            "설문 결과가 서버에 반영되지 않았습니다. 설문 제출 API를 먼저 호출해 주세요.",
            failure.exceptionOrNull()?.message,
        )
    }

    @Test
    fun clearProfileClearsTokenAfter204() = runTest {
        val tokenStore = RecordingTokenStore("token")
        val connection = RecordingHttpURLConnection(
            URL("https://example.com"),
            statusCode = HttpURLConnection.HTTP_NO_CONTENT,
            responseBody = "",
        )

        repository(tokenStore, connection).clearProfile().getOrThrow()

        assertEquals("DELETE", connection.requestMethod)
        assertNull(tokenStore.token)
    }

    private fun repository(
        tokenStore: RecordingTokenStore,
        vararg connections: RecordingHttpURLConnection,
    ): RemoteProfileRepository {
        val queue = ArrayDeque(connections.toList())
        val client = GayadiHttpClient(
            "https://example.com",
            tokenStore,
            connectionFactory = { requestedUrl ->
                queue.removeFirst().withUrl(requestedUrl)
            },
            ioDispatcher = Dispatchers.Unconfined,
        )
        return RemoteProfileRepository(client, tokenStore)
    }

    private fun profileResponse(resultCode: String? = "PNR") = JSONObject().apply {
        put("id", 12)
        put("email", "user@example.com")
        put("nickname", "여행자")
        put("introduction", "느긋한 여행")
        put("profileImageUrl", JSONObject.NULL)
        put("resultCode", resultCode ?: JSONObject.NULL)
        put("travelStyleName", "산책가")
        put("characterKey", "character_pnr")
        put("strengths", org.json.JSONArray().put("계획"))
        put("weaknesses", org.json.JSONArray().put("지연"))
    }.toString()
}

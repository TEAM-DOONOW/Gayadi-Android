package com.gayadi.android.data

import com.gayadi.android.data.datasource.*
import com.gayadi.android.data.model.InquiryDto
import com.gayadi.android.data.repository.*
import com.gayadi.android.domain.model.*
import com.gayadi.android.domain.repository.AuthRepository
import com.gayadi.android.domain.usecase.SubmitSurveyUseCase
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class RestAccountSurveyApiTest {
    private lateinit var server: MockWebServer
    private lateinit var auth: TestAuth
    private lateinit var api: GayadiApiClient
    @Before fun setup() {
        server = MockWebServer().apply { start() }
        auth = TestAuth()
        api = GayadiApiClient(server.url("/").toString(), auth)
    }
    @After fun cleanup() { server.shutdown() }
    private fun json(body: String) = MockResponse().setHeader("Content-Type", "application/json").setBody(body)

    @Test fun `401 refreshes once and retries with the new token`() = runTest {
        server.enqueue(MockResponse().setResponseCode(401))
        server.enqueue(json("{}"))
        api.request("GET", "/api/v1/users/current")
        assertEquals("Bearer old", server.takeRequest().getHeader("Authorization"))
        assertEquals("Bearer new", server.takeRequest().getHeader("Authorization"))
        assertEquals(1, auth.refreshes)
    }

    @Test fun `second 401 and 403 do not loop or expose the response body`() = runTest {
        server.enqueue(MockResponse().setResponseCode(401))
        server.enqueue(MockResponse().setResponseCode(401).setBody("private diagnostics"))
        val failure = runCatching { api.request("GET", "/api/v1/users/current") }.exceptionOrNull()
        assertEquals(401, (failure as GayadiApiException).statusCode)
        assertFalse(failure.message.orEmpty().contains("private"))
        assertEquals(2, server.requestCount)
        server.enqueue(MockResponse().setResponseCode(403))
        assertTrue(runCatching { api.request("DELETE", "/api/v1/users/current") }.isFailure)
        assertEquals(1, auth.refreshes)
    }

    @Test fun `public survey maps server fields and never sends a token`() = runTest {
        server.enqueue(json("""{"id":"travel-personality-v1","title":"여행 설문",
            "resultCodeOrder":["preparation"],"questions":[{"id":"q01","order":1,
            "dimension":"preparation","title":"첫 질문","options":[{"id":"a","text":"계획","code":"P"}]}],
            "results":[]} """))
        val deferred = CompletableDeferred<Result<com.gayadi.android.data.model.SurveyDefinitionDto>>()
        RestSurveyDataSource(api, this).loadSurvey { deferred.complete(it) }
        val survey = deferred.await().getOrThrow()
        assertEquals("첫 질문", survey.questions.single().title)
        val request = server.takeRequest()
        assertEquals("/api/v1/surveys/travel-personality-v1", request.path)
        assertNull(request.getHeader("Authorization"))
        assertEquals(0, auth.refreshes)
    }

    @Test fun `submission sends option ids and returns the server result`() = runTest {
        server.enqueue(json("""{"attemptId":1,"resultCode":"PNR"}"""))
        val definition = SurveyDefinition("travel-personality-v1", "설문", listOf("preparation"),
            listOf(SurveyQuestion("q01", 1, "preparation", "질문",
                listOf(SurveyOption("a", "계획", "P"), SurveyOption("b", "즉흥", "S")))), emptyMap())
        val result = SubmitSurveyUseCase(RestSurveySubmissionRepository(api))(definition, mapOf("q01" to "P"))
        assertEquals("PNR", result)
        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/api/v1/surveys/travel-personality-v1/submissions", request.path)
        val answer = JSONObject(request.body.readUtf8()).getJSONArray("answers").getJSONObject(0)
        assertEquals("q01", answer.getString("questionId"))
        assertEquals("a", answer.getString("optionId"))
    }

    @Test fun `inquiry uses authenticated API and propagates server failure`() = runTest {
        server.enqueue(MockResponse().setResponseCode(201).setBody("{}"))
        val source = RestInquiryDataSource(api, this)
        val inquiry = InquiryDto("BUG", "제목", "설명", "test@example.invalid")
        val result = CompletableDeferred<Result<Unit>>()
        source.submit(inquiry) { result.complete(it) }
        result.await().getOrThrow()
        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/api/v1/inquiries", request.path)
        assertEquals("Bearer old", request.getHeader("Authorization"))
        assertEquals("BUG", JSONObject(request.body.readUtf8()).getString("category"))
        server.enqueue(MockResponse().setResponseCode(500))
        val failure = CompletableDeferred<Result<Unit>>()
        source.submit(inquiry) { failure.complete(it) }
        assertTrue(failure.await().isFailure)
    }

    @Test fun `logout sends refresh token body without requiring an access token`() = runTest {
        auth.clearSession()
        server.enqueue(MockResponse().setResponseCode(204))
        RestSessionApiDataSource(api).logout("test-refresh")
        val request = server.takeRequest()
        assertEquals("DELETE", request.method)
        assertEquals("/api/v1/auth/sessions/current", request.path)
        assertNull(request.getHeader("Authorization"))
        assertEquals("test-refresh", JSONObject(request.body.readUtf8()).getString("refreshToken"))
    }

    @Test fun `profile maps nullable fields and delete accepts 204`() = runTest {
        server.enqueue(json("""{"id":12,"nickname":"가야디","introduction":null,"resultCode":null,
            "characterKey":null,"strengths":null,"weaknesses":null}"""))
        val source = HttpProfileApiDataSource(api)
        val profile = source.currentUser()
        assertEquals("가야디", profile.nickname)
        assertEquals("", profile.introduction)
        assertNull(profile.resultCode)
        assertTrue(profile.strengths.isEmpty())
        server.enqueue(MockResponse().setResponseCode(204))
        source.deleteCurrentUser()
        server.takeRequest()
        assertEquals("DELETE", server.takeRequest().method)
    }

    @Test fun `failed deletion keeps session and local profile`() = runTest {
        val local = InMemoryProfileRepository(InMemoryProfileLocalDataSource())
        local.saveBasicInfo(BasicInfo("기존", "소개"))
        val repository = AuthenticatedProfileRepository(local, HttpProfileApiDataSource(api), auth)
        server.enqueue(MockResponse().setResponseCode(500))
        assertTrue(repository.clearProfile().isFailure)
        assertNotNull(auth.currentSession())
        assertEquals("기존", local.getProfile()?.nickname)
        server.enqueue(MockResponse().setResponseCode(204))
        repository.clearProfile().getOrThrow()
        assertNull(auth.currentSession())
        assertNull(local.getProfile())
    }

    private class TestAuth : AuthRepository {
        var refreshes = 0
        private var session: AuthSession? = AuthSession("old", "Bearer", 3600, "refresh", 7200, 0,
            AuthUser(12, "가야디", "test@example.invalid"))
        override suspend fun signInWithGoogle(idToken: String) = requireNotNull(session)
        override suspend fun refreshSession(): AuthSession {
            refreshes++
            return requireNotNull(session).copy(accessToken = "new").also { session = it }
        }
        override suspend fun validAccessToken() = requireNotNull(session).accessToken
        override fun currentSession() = session
        override fun clearSession() { session = null }
    }
}

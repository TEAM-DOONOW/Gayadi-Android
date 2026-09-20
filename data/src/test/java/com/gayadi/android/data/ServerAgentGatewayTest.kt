package com.gayadi.android.data

import com.gayadi.android.data.datasource.GayadiApiClient
import com.gayadi.android.data.remote.agent.ServerAgentGateway
import com.gayadi.android.domain.model.AuthSession
import com.gayadi.android.domain.repository.AuthRepository
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ServerAgentGatewayTest {
    @Test
    fun recommendsPlacesWithTravelContextAndParsesCanonicalIds() = runTest {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setResponseCode(200).setBody(
                """
                {
                  "recommendations":[{
                    "placeId":"42","sourcePlaceId":"126508","name":"국립중앙박물관",
                    "category":"CULTURE","score":0.91,"reason":"여행 성향과 잘 맞아요."
                  }],
                  "reasoning":"문화 체험을 중심으로 골랐어요."
                }
                """.trimIndent(),
            ))
            val gateway = ServerAgentGateway(GayadiApiClient(server.url("/").toString(), TestAuthRepository()))

            val response = gateway.recommendPlaces(
                destination = "서울",
                profile = "문화 체험을 좋아해요.",
                latitude = 37.5,
                longitude = 127.0,
                keywords = listOf("박물관"),
                groupSize = 2,
            )

            assertEquals("42", response.recommendations.single().placeId)
            assertEquals("126508", response.recommendations.single().sourcePlaceId)
            val request = server.takeRequest(1, TimeUnit.SECONDS)!!
            assertEquals("/api/v1/recommendations/places", request.path)
            val body = JSONObject(request.body.readUtf8())
            assertEquals("PLACE_RECOMMENDATION", body.getString("purpose"))
            assertEquals("서울", body.getString("destination"))
            assertEquals(2, body.getInt("groupSize"))
            assertTrue(body.getBoolean("externalProcessingConsent"))
        }
    }

    @Test
    fun analyzesSituationAndParsesRecommendationsAndProposal() = runTest {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setResponseCode(200).setBody(
                """
                {
                  "situationSummary":"비 예보로 실내 장소를 우선해요.",
                  "routeRecalculationRequired":true,
                  "nextAction":"대안을 선택해 주세요.",
                  "placeRecommendations":{
                    "recommendations":[{"placeId":"42","name":"국립중앙박물관","category":"CULTURE","score":0.91,"reason":"실내 관람이 가능해요."}],
                    "reasoning":"비를 피할 수 있는 장소를 골랐어요."
                  },
                  "changeProposal":{
                    "id":7,"reason":"비 예보","status":"PENDING","baseRevisionNo":2,
                    "options":[{"key":"AI_RECOMMENDATION_42","placeId":42,"placeName":"국립중앙박물관","description":"실내 대안","requireIndoor":true}]
                  }
                }
                """.trimIndent(),
            ))
            val gateway = ServerAgentGateway(GayadiApiClient(server.url("/").toString(), TestAuthRepository()))

            val response = gateway.analyzeSituation("3", 37.5, 127.0)

            assertEquals("국립중앙박물관", response.recommendations.single().name)
            assertEquals("7", response.changeProposal?.id)
            assertTrue(response.routeRecalculationRequired)
            val request = server.takeRequest(1, TimeUnit.SECONDS)!!
            assertEquals("/api/v1/trips/3/situation-responses", request.path)
            assertEquals("Bearer access", request.getHeader("Authorization"))
            assertTrue(JSONObject(request.body.readUtf8()).getBoolean("externalProcessingConsent"))
        }
    }

    @Test
    fun listsAndDecidesPendingProposal() = runTest {
        val proposal = """
            {"id":7,"reason":"혼잡","status":"PENDING","baseRevisionNo":2,
             "options":[{"key":"ALT_42","placeId":42,"placeName":"서울도서관","description":"한적한 장소","requireIndoor":true}]}
        """.trimIndent()
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setResponseCode(200).setBody("[$proposal]"))
            server.enqueue(MockResponse().setResponseCode(200).setBody(proposal.replace("PENDING", "APPROVED")))
            val gateway = ServerAgentGateway(GayadiApiClient(server.url("/").toString(), TestAuthRepository()))

            val pending = gateway.listChangeProposals("3").single()
            val approved = gateway.decideChangeProposal("3", pending.id, true, "ALT_42", 2)

            assertEquals("APPROVED", approved.status)
            val listRequest = server.takeRequest(1, TimeUnit.SECONDS)!!
            assertTrue(listRequest.path!!.startsWith("/api/v1/trips/3/change-proposals"))
            val decisionRequest = server.takeRequest(1, TimeUnit.SECONDS)!!
            assertEquals("PATCH", decisionRequest.method)
            assertEquals("ALT_42", JSONObject(decisionRequest.body.readUtf8()).getString("selectedOptionKey"))
        }
    }
}

private class TestAuthRepository : AuthRepository {
    override suspend fun signInWithGoogle(idToken: String): AuthSession = error("unused")
    override suspend fun refreshSession(): AuthSession = error("unused")
    override suspend fun validAccessToken(): String = "access"
    override fun currentSession(): AuthSession? = null
    override fun observeSession(): Flow<AuthSession?> = flowOf(null)
    override fun clearSession() = Unit
}

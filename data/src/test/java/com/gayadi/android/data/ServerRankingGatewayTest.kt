package com.gayadi.android.data

import com.gayadi.android.data.datasource.GayadiApiClient
import com.gayadi.android.data.remote.ranking.ServerRankingGateway
import com.gayadi.android.domain.model.RankingType
import com.gayadi.android.domain.repository.AuthRepository
import com.gayadi.android.domain.model.AuthSession
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ServerRankingGatewayTest {
    private lateinit var server: MockWebServer
    private lateinit var gateway: ServerRankingGateway

    @Before fun setup() {
        server = MockWebServer().apply { start() }
        val auth = object : AuthRepository {
            override fun currentSession(): AuthSession? = null
            override fun clearSession() {}
            override suspend fun validAccessToken() = "test-token"
            override suspend fun refreshSession(): AuthSession = error("Unused")
            override suspend fun signInWithGoogle(idToken: String): AuthSession = error("Unused")
        }
        gateway = ServerRankingGateway(GayadiApiClient(server.url("/").toString(), auth))
    }

    @After fun teardown() { server.shutdown() }

    @Test fun requestsTypeRegionAndParsesItems() = runTest {
        server.enqueue(MockResponse().setBody("""
            {"type":"ATTRACTION","region":"서울","basePeriod":"2026-07","source":"KTO_DATALAB",
             "providerDataAvailable":true,"items":[
               {"rank":1,"title":"경복궁","subtitle":"서울 종로구 · 궁","imageUrl":"https://img/a.jpg",
                "latitude":37.579,"longitude":126.977,"placeId":null,"contentId":"126508","metric":null,
                "metricLabel":"방문 순위 1위"},
               {"rank":2,"title":"  ","subtitle":null},
               {"rank":3,"title":"명동 맛집","placeId":42,"imageUrl":null,"latitude":null,"metricLabel":"찜 3개"}
             ]}
        """.trimIndent()))

        val result = gateway.getRankings(RankingType.ATTRACTION, "서울 ", 10)

        val request = server.takeRequest()
        assertEquals("/api/v1/rankings", request.requestUrl!!.encodedPath)
        assertEquals("ATTRACTION", request.requestUrl!!.queryParameter("type"))
        assertEquals("서울", request.requestUrl!!.queryParameter("region"))
        assertEquals("10", request.requestUrl!!.queryParameter("limit"))
        assertEquals("Bearer test-token", request.getHeader("Authorization"))

        assertTrue(result.providerDataAvailable)
        assertEquals("2026-07", result.basePeriod)
        assertEquals(listOf("경복궁", "명동 맛집"), result.items.map { it.title })
        val first = result.items.first()
        assertEquals("https://img/a.jpg", first.imageUrl)
        assertEquals(37.579, first.latitude!!, 1e-9)
        assertNull(first.placeId)
        assertEquals("126508", first.contentId)
        val restaurant = result.items.last()
        assertEquals("42", restaurant.placeId)
        assertEquals("", restaurant.imageUrl)
        assertNull(restaurant.latitude)
    }

    @Test fun nationwideRequestOmitsRegionAndKeepsFallbackFlag() = runTest {
        server.enqueue(MockResponse().setBody("""{"type":"REGION","region":"","providerDataAvailable":false,"items":[]}"""))

        val result = gateway.getRankings(RankingType.REGION)

        assertNull(server.takeRequest().requestUrl!!.queryParameter("region"))
        assertFalse(result.providerDataAvailable)
        assertTrue(result.items.isEmpty())
    }
}

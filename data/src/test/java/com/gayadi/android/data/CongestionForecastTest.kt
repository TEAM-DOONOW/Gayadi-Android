package com.gayadi.android.data

import com.gayadi.android.data.datasource.GayadiApiClient
import com.gayadi.android.data.remote.travel.ServerTripSupportGateway
import com.gayadi.android.domain.repository.CongestionCommand
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.*
import org.junit.Test

class CongestionForecastTest {
    @Test fun `place detail authenticates and reads nested weather`() = runTest {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setBody("""{
                "weather":{"available":true,"condition":"맑음","temperatureCelsius":23,"precipitationProbability":10}
            }"""))
            val gateway = ServerTripSupportGateway(GayadiApiClient(server.url("/").toString(), TestAuthRepository()))
            val weather = gateway.getPlaceWeather("123")
            val request = server.takeRequest()
            assertEquals("GET", request.method)
            assertEquals("/api/v1/congestion/places/123", request.path)
            assertEquals("Bearer access", request.getHeader("Authorization"))
            assertEquals("맑음", weather?.condition)
            assertEquals("23", weather?.temperature)
            assertEquals(10, weather?.precipitationProbability)
            server.enqueue(MockResponse().setBody("""{"weather":{"available":false}}"""))
            assertEquals(false, gateway.getPlaceWeather("123")?.available)
            server.enqueue(MockResponse().setBody("""{}"""))
            assertNull(gateway.getPlaceWeather("123"))
        }
    }

    @Test fun `forecast authenticates and preserves offset and calendar provenance`() = runTest {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setBody("""{
                "level":"혼잡","concentrationScore":72,"estimated":true,
                "providerDataAvailable":false,"source":"CALENDAR","targetDate":"2026-09-28",
                "confidence":"LOW","message":"달력 기반 추정입니다."
            }"""))
            val gateway = ServerTripSupportGateway(GayadiApiClient(server.url("/").toString(), TestAuthRepository()))
            val result = gateway.getCongestion(CongestionCommand("11", "110", "서울", "경복궁", "2026-09-28T12:00+09:00"))
            val request = server.takeRequest()
            assertEquals("Bearer access", request.getHeader("Authorization"))
            assertEquals("/api/v1/congestion/forecast", request.requestUrl!!.encodedPath)
            assertEquals("2026-09-28T12:00+09:00", request.requestUrl!!.queryParameter("targetAt"))
            assertEquals("경복궁", request.requestUrl!!.queryParameter("placeName"))
            assertEquals("CALENDAR", result.source)
            assertEquals("2026-09-28", result.targetDate)
            assertTrue(result.estimated)
            assertFalse(result.providerDataAvailable)
            assertEquals("달력 기반 추정입니다.", result.message)
        }
    }

}

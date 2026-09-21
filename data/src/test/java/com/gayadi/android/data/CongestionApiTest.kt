package com.gayadi.android.data

import com.gayadi.android.data.datasource.GayadiApiClient
import com.gayadi.android.data.datasource.RestCongestionDataSource
import com.gayadi.android.data.repository.DefaultCongestionRepository
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CongestionApiTest {
    @Test
    fun requestsHourlyForecastAndParsesPoints() = runTest {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setResponseCode(200).setBody(
                """
                {
                  "area":"서울특별시 종로구","placeName":"경복궁","targetDate":"2026-09-01",
                  "baseLevel":"CROWDED","baseScore":75,
                  "source":"KTO_DISTRICT_CONCENTRATION_FORECAST",
                  "estimated":true,"providerDataAvailable":true,"confidence":"LOW",
                  "message":"시간대 분포를 적용한 추정치입니다.",
                  "points":[
                    {"hour":11,"concentrationScore":79,"level":"CROWDED"},
                    {"hour":13,"concentrationScore":86,"level":"CROWDED"}
                  ]
                }
                """.trimIndent(),
            ))
            val repository = DefaultCongestionRepository(
                RestCongestionDataSource(GayadiApiClient(server.url("/").toString(), TestAuthRepository())),
            )

            val forecast = repository.getHourlyForecast(
                areaCode = "11",
                districtCode = "110",
                areaName = "서울",
                placeName = "경복궁",
            ).getOrThrow()

            assertEquals("CROWDED", forecast.baseLevel)
            assertEquals(75, forecast.baseScore)
            assertTrue(forecast.estimated)
            assertTrue(forecast.providerDataAvailable)
            assertEquals(listOf(11, 13), forecast.points.map { it.hour })
            assertEquals(listOf(79, 86), forecast.points.map { it.concentrationScore })
            val request = server.takeRequest(1, TimeUnit.SECONDS)!!
            assertTrue(request.path!!.startsWith("/api/v1/congestion/forecast/hourly"))
            assertTrue(request.path!!.contains("areaCode=11"))
            assertTrue(request.path!!.contains("districtCode=110"))
        }
    }

    @Test
    fun rejectsInvalidAreaCodesBeforeRequest() = runTest {
        val repository = DefaultCongestionRepository(
            RestCongestionDataSource(GayadiApiClient("http://example.com", TestAuthRepository())),
        )

        val result = repository.getHourlyForecast(areaCode = "1", districtCode = "110")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }
}

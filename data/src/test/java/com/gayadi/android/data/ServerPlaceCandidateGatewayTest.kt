package com.gayadi.android.data

import com.gayadi.android.data.datasource.GayadiApiClient
import com.gayadi.android.data.remote.travel.ServerPlaceCandidateGateway
import com.gayadi.android.domain.model.RouteTransportMode
import com.gayadi.android.domain.repository.*
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.*
import org.junit.Test

class ServerPlaceCandidateGatewayTest {
    @Test fun `travel time search is authenticated read only and preserves order and signed additional minutes`() = runTest {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setBody("""{
                "items":[
                  {"id":9,"name":"카페 B","categoryCode":"CAFE","latitude":37.1,"longitude":127.1,
                   "travelTime":{"transportMode":"CAR","durationMinutes":10,"onwardDurationMinutes":7,"additionalDurationMinutes":-5,"configuredProvider":"KAKAO_DIRECTIONS","fallback":false}},
                  {"id":2,"name":"카페 A","categoryCode":"CAFE","latitude":37.2,"longitude":127.2,
                   "travelTime":{"transportMode":"CAR","durationMinutes":15,"onwardDurationMinutes":null,"additionalDurationMinutes":null,"configuredProvider":"LOCAL_ESTIMATE","fallback":true}}
                ],"ranking":{"sort":"TRAVEL_TIME","evaluatedCandidates":20,"limited":true},"nextCursor":null,"hasNext":false
            }"""))
            val gateway = ServerPlaceCandidateGateway(GayadiApiClient(server.url("/").toString(), TestAuthRepository()))
            val page = gateway.search(PlaceCandidateQuery(
                query = "카페", region = "1", category = "CAFE", sort = PlaceSort.TRAVEL_TIME,
                transportMode = RouteTransportMode.CAR, origin = PlaceCoordinate(37.5665,126.978), next = PlaceCoordinate(37.6,127.0),
            ))
            val request = server.takeRequest()
            assertEquals("GET", request.method)
            assertEquals("Bearer access", request.getHeader("Authorization"))
            val url = requireNotNull(request.requestUrl)
            assertEquals("/api/v1/places", url.encodedPath)
            assertEquals("카페", url.queryParameter("query"))
            assertEquals("CAR", url.queryParameter("transportMode"))
            assertEquals("37.5665", url.queryParameter("originLatitude"))
            assertEquals("127.0", url.queryParameter("nextLongitude"))
            assertNull(url.queryParameter("cursor"))
            assertEquals(listOf("9", "2"), page.items.map { it.place.contentId })
            assertEquals(-5, page.items.first().travelTime?.additionalDurationMinutes)
            assertFalse(page.items.first().travelTime!!.isEstimate)
            assertTrue(page.items.last().travelTime!!.isEstimate)
            assertTrue(page.limited)
        }
    }

    @Test fun `walking and cycling modes round trip through place search`() = runTest {
        MockWebServer().use { server ->
            val gateway = ServerPlaceCandidateGateway(GayadiApiClient(server.url("/").toString(), TestAuthRepository()))
            listOf("WALK" to RouteTransportMode.WALK, "BICYCLE" to RouteTransportMode.BICYCLE).forEach { (wireValue, mode) ->
                server.enqueue(MockResponse().setBody("""{
                    "items":[{"id":9,"name":"후보","travelTime":{"transportMode":"$wireValue","durationMinutes":12,"configuredProvider":"LOCAL_ESTIMATE","fallback":false}}],
                    "ranking":{"sort":"TRAVEL_TIME","evaluatedCandidates":1,"limited":false},"hasNext":false
                }"""))
                val page = gateway.search(PlaceCandidateQuery(sort=PlaceSort.TRAVEL_TIME,transportMode=mode,origin=PlaceCoordinate(37.5,127.0)))
                assertEquals(wireValue,server.takeRequest().requestUrl!!.queryParameter("transportMode"))
                assertEquals(mode,page.items.single().travelTime!!.transportMode)
                assertTrue(page.items.single().travelTime!!.isEstimate)
            }
        }
    }

    @Test fun `missing origin accepts server recent fallback and recent search uses cursor`() = runTest {
        MockWebServer().use { server ->
            val body = """{"items":[],"ranking":{"sort":"RECENT"},"nextCursor":"12","hasNext":true}"""
            repeat(2) { server.enqueue(MockResponse().setBody(body)) }
            val gateway = ServerPlaceCandidateGateway(GayadiApiClient(server.url("/").toString(), TestAuthRepository()))
            val page = gateway.search(PlaceCandidateQuery(sort = PlaceSort.TRAVEL_TIME, region = "1"))
            assertEquals(PlaceSort.RECENT, page.sort)
            val request = server.takeRequest().requestUrl!!
            assertNull(request.queryParameter("originLatitude"))
            assertNull(request.queryParameter("nextLatitude"))
            assertEquals("PUBLIC_TRANSIT", request.queryParameter("transportMode"))
            gateway.search(PlaceCandidateQuery(cursor = "12"))
            assertEquals("12", server.takeRequest().requestUrl!!.queryParameter("cursor"))
        }
    }

    @Test fun `missing ranking with valid origin is not treated as missing coordinates`() = runTest {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setBody("""{"items":[],"nextCursor":null,"hasNext":false}"""))
            val gateway = ServerPlaceCandidateGateway(GayadiApiClient(server.url("/").toString(), TestAuthRepository()))
            val failure = runCatching {
                gateway.search(PlaceCandidateQuery(sort=PlaceSort.TRAVEL_TIME, origin=PlaceCoordinate(37.5,127.0)))
            }.exceptionOrNull()
            assertTrue(failure is java.io.IOException)
            assertTrue(failure!!.message!!.contains("정렬 정보를 받지 못했어요"))
            assertEquals("37.5",server.takeRequest().requestUrl!!.queryParameter("originLatitude"))
        }
    }

    @Test fun `invalid cursor and next only requests are rejected before network`() {
        assertThrows(IllegalArgumentException::class.java) { PlaceCandidateQuery(sort = PlaceSort.TRAVEL_TIME, cursor = "1") }
        assertThrows(IllegalArgumentException::class.java) { PlaceCandidateQuery(next = PlaceCoordinate(37.0,127.0)) }
        assertThrows(IllegalArgumentException::class.java) { PlaceCoordinate(Double.NaN,127.0) }
    }
}

package com.gayadi.android.data

import com.gayadi.android.data.datasource.GayadiApiClient
import com.gayadi.android.data.datasource.ServerPlaceApiDataSource
import com.gayadi.android.data.datasource.TourApiDataSource
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ServerPlaceApiDataSourceTest {
    @Test
    fun loadsCanonicalPlaceIdsWithRegionAndCategory() = runTest {
        MockWebServer().use { server ->
            server.enqueue(page(place(101, "서울 명소", "ATTRACTION", 37.5, 127.0)))
            val source: TourApiDataSource =
                ServerPlaceApiDataSource(GayadiApiClient(server.url("/").toString()))

            val places = source.getPlaces(
                pageSize = 20,
                contentTypeId = 12,
                regionName = "서울",
            )

            assertEquals("101", places.single().contentId)
            val request = server.takeRequest().requestUrl!!
            assertEquals("ATTRACTION", request.queryParameter("category"))
            assertEquals("서울", request.queryParameter("region"))
        }
    }

    @Test
    fun fallsBackToAllPlacesWhenRegionHasNoMatches() = runTest {
        MockWebServer().use { server ->
            server.enqueue(page())
            server.enqueue(page(place(101, "서울 명소", "ATTRACTION", 37.5, 127.0)))
            val source: TourApiDataSource =
                ServerPlaceApiDataSource(GayadiApiClient(server.url("/").toString()))

            val places = source.getPlaces(
                pageSize = 20,
                contentTypeId = 12,
                regionName = "제주 성산",
            )

            assertEquals("101", places.single().contentId)
            assertEquals("제주 성산", server.takeRequest().requestUrl!!.queryParameter("region"))
            assertEquals(null, server.takeRequest().requestUrl!!.queryParameter("region"))
        }
    }

    @Test
    fun searchesCanonicalPlacesByKeyword() = runTest {
        MockWebServer().use { server ->
            server.enqueue(page(place(202, "검색 장소", "CAFE", 37.5, 127.0)))
            val source: TourApiDataSource =
                ServerPlaceApiDataSource(GayadiApiClient(server.url("/").toString()))

            val places = source.searchPlaces(pageSize = 10, keyword = "검색 장소")

            assertEquals("202", places.single().contentId)
            assertEquals("검색 장소", server.takeRequest().requestUrl!!.queryParameter("query"))
        }
    }

    @Test
    fun filtersAndSortsCanonicalPlacesByDistance() = runTest {
        MockWebServer().use { server ->
            server.enqueue(
                page(
                    place(1, "먼 장소", "ATTRACTION", 37.509, 127.0),
                    place(2, "가까운 장소", "ATTRACTION", 37.501, 127.0),
                    place(3, "반경 밖 장소", "ATTRACTION", 38.0, 127.0),
                ),
            )
            val source: TourApiDataSource =
                ServerPlaceApiDataSource(GayadiApiClient(server.url("/").toString()))

            val places = source.getNearbyPlaces(
                pageSize = 10,
                mapX = "127.0",
                mapY = "37.5",
                radius = 2_000,
                arrange = "E",
                maxPages = 1,
            )

            assertEquals(listOf("2", "1"), places.map { it.contentId })
            assertTrue(places.all { requireNotNull(it.distanceMeters) <= 2_000 })
        }
    }

    private fun page(vararg items: String) = MockResponse()
        .setResponseCode(200)
        .setHeader("Content-Type", "application/json")
        .setBody("""{"items":[${items.joinToString(",")}],"nextCursor":null,"hasNext":false}""")

    private fun place(id: Long, name: String, category: String, latitude: Double, longitude: Double) =
        """{
            "id":$id,
            "name":"$name",
            "categoryCode":"$category",
            "address":"주소",
            "roadAddress":"도로명 주소",
            "latitude":$latitude,
            "longitude":$longitude,
            "imageUrl":""
        }""".trimIndent()
}

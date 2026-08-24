package com.gayadi.android.data

import com.gayadi.android.data.datasource.HttpTourApiDataSource
import com.gayadi.android.data.datasource.TourApiDataSource
import com.gayadi.android.data.model.TourPlaceDto
import com.gayadi.android.data.repository.DefaultTourRepository
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.ArrayDeque
import kotlinx.coroutines.test.runTest
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TourApiTest {
    @Test
    fun parsesTourAreaResponse() {
        val places = HttpTourApiDataSource("http://example.com").parsePlaces(
            """
            {
              "items": [{
                "contentId": "2783012",
                "title": "영산포철도공원",
                "address": "전남광주통합특별시 나주시 삼영동",
                "addressDetail": "174-1",
                "firstImage": "https://example.com/place.jpg",
                "mapX": "126.7075",
                "mapY": "35.0052"
              }]
            }
            """.trimIndent(),
        )

        assertEquals(1, places.size)
        assertEquals("2783012", places.single().contentId)
        assertEquals("영산포철도공원", places.single().title)
    }

    @Test
    fun followsServerCursorContractUntilLastPage() = runTest {
        val requestedUrls = mutableListOf<URL>()
        val responses = ArrayDeque(
            listOf(
                tourPage("place-1", "첫 번째 장소", "cursor +/="),
                tourPage("place-2", "두 번째 장소", null),
            ),
        )
        val dataSource = stubbedDataSource(requestedUrls, responses)

        val places = dataSource.getPlaces(pageSize = 1, contentTypeId = 12)

        val requests = requestedUrls.map { parseQuery(it.query) }
        assertEquals(listOf("place-1", "place-2"), places.map(TourPlaceDto::contentId))
        assertEquals(2, requests.size)
        assertEquals("1", requests[0]["pageSize"])
        assertEquals("12", requests[0]["contentTypeId"])
        assertFalse(requests[0].containsKey("cursor"))
        assertFalse(requests[0].containsKey("pageNo"))
        assertFalse(requests[0].containsKey("numOfRows"))
        assertEquals("1", requests[1]["pageSize"])
        assertEquals("12", requests[1]["contentTypeId"])
        assertEquals("cursor +/=", requests[1]["cursor"])
    }

    @Test
    fun rejectsRepeatedNextCursor() = runTest {
        val requestedUrls = mutableListOf<URL>()
        val repeatedPage = tourPage("place-1", "반복 장소", "repeated-cursor")
        val dataSource = stubbedDataSource(
            requestedUrls = requestedUrls,
            responses = ArrayDeque(listOf(repeatedPage, repeatedPage)),
        )

        val result = runCatching {
            dataSource.getPlaces(pageSize = 1, contentTypeId = 12)
        }

        assertTrue(result.isFailure)
        assertEquals(
            "관광 API가 동일한 다음 페이지 커서를 반복했습니다.",
            result.exceptionOrNull()?.message,
        )
        assertEquals(2, requestedUrls.size)
    }

    @Test
    fun reusesCachedResponseForSameRequest() = runTest {
        val dataSource = CountingTourApiDataSource()
        val repository = DefaultTourRepository(dataSource)

        repository.getPlaces(pageSize = 100, contentTypeId = 12).getOrThrow()
        repository.getPlaces(pageSize = 100, contentTypeId = 12).getOrThrow()

        assertEquals(1, dataSource.requestCount)
    }
}

private class CountingTourApiDataSource : TourApiDataSource {
    var requestCount = 0

    override suspend fun getPlaces(pageSize: Int, contentTypeId: Int): List<TourPlaceDto> {
        requestCount += 1
        return listOf(
            TourPlaceDto(
                contentId = "1",
                title = "테스트 관광지",
                address = "서울",
                addressDetail = "",
                firstImage = "",
                mapX = "126.0",
                mapY = "37.0",
            ),
        )
    }
}

private fun stubbedDataSource(
    requestedUrls: MutableList<URL>,
    responses: ArrayDeque<String>,
): HttpTourApiDataSource = HttpTourApiDataSource("http://example.com") { url ->
    requestedUrls += url
    StubHttpURLConnection(url, responses.removeFirst())
}

private class StubHttpURLConnection(
    url: URL,
    private val responseBody: String,
) : HttpURLConnection(url) {
    override fun connect() = Unit

    override fun disconnect() = Unit

    override fun usingProxy(): Boolean = false

    override fun getResponseCode(): Int = HTTP_OK

    override fun getInputStream(): InputStream =
        ByteArrayInputStream(responseBody.toByteArray(StandardCharsets.UTF_8))
}

private fun parseQuery(rawQuery: String?): Map<String, String> =
    rawQuery.orEmpty()
        .split('&')
        .filter(String::isNotBlank)
        .associate { parameter ->
            val parts = parameter.split('=', limit = 2)
            val name = URLDecoder.decode(parts[0], StandardCharsets.UTF_8.name())
            val value = URLDecoder.decode(parts.getOrElse(1) { "" }, StandardCharsets.UTF_8.name())
            name to value
        }

private fun tourPage(contentId: String, title: String, nextCursor: String?): String =
    JSONObject().apply {
        put("items", JSONArray().put(JSONObject().apply {
            put("contentId", contentId)
            put("title", title)
        }))
        put("totalCount", 1)
        put("pageSize", 1)
        put("nextCursor", nextCursor ?: JSONObject.NULL)
    }.toString()

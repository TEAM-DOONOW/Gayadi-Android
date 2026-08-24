package com.gayadi.android.data

import com.gayadi.android.data.datasource.HttpTourApiDataSource
import com.gayadi.android.data.datasource.TourApiDataSource
import com.gayadi.android.data.mapper.toDomain
import com.gayadi.android.data.model.TourPlaceDto
import com.gayadi.android.data.repository.DefaultTourRepository
import com.gayadi.android.domain.usecase.GetTourPlacesUseCase
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.ArrayDeque
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class TourApiTest {
    @Test
    fun parsesTourAreaResponseIncludingClassification() {
        val places = HttpTourApiDataSource("http://example.com").parsePlaces(
            """
            {
              "items": [{
                "contentId": "2783012",
                "contentTypeId": "39",
                "title": "영산포철도공원",
                "address": "전남광주통합특별시 나주시 삼영동",
                "addressDetail": "174-1",
                "firstImage": "https://example.com/place.jpg",
                "mapX": "126.7075",
                "mapY": "35.0052",
                "lclsSystm1": "FD",
                "lclsSystm2": "FD05",
                "lclsSystm3": "FD050100"
              }]
            }
            """.trimIndent(),
        )

        assertEquals(1, places.size)
        assertEquals("2783012", places.single().contentId)
        assertEquals("영산포철도공원", places.single().title)
        assertEquals("39", places.single().contentTypeId)
        assertEquals("FD", places.single().lclsSystm1)
        assertEquals("FD05", places.single().lclsSystm2)
        assertEquals("FD050100", places.single().lclsSystm3)
    }

    @Test
    fun tourPlaceMapperPreservesClassification() {
        val place = TourPlaceDto(
            contentId = "1",
            title = "테스트 카페",
            address = "서울",
            addressDetail = "종로구",
            firstImage = "https://example.com/cafe.jpg",
            mapX = "126.0",
            mapY = "37.0",
            contentTypeId = "39",
            lclsSystm1 = "FD",
            lclsSystm2 = "FD05",
            lclsSystm3 = "FD050100",
        ).toDomain()

        assertEquals("39", place.contentTypeId)
        assertEquals("FD", place.lclsSystm1)
        assertEquals("FD05", place.lclsSystm2)
        assertEquals("FD050100", place.lclsSystm3)
    }

    @Test
    fun followsServerCursorContractUntilLastPageAndKeepsFilters() = runTest {
        val requestedUrls = mutableListOf<URL>()
        val responses = ArrayDeque(
            listOf(
                tourPage("place-1", "첫 번째 장소", "cursor +/="),
                tourPage("place-2", "두 번째 장소", null),
            ),
        )
        val dataSource = stubbedDataSource(requestedUrls, responses)

        val places = dataSource.getPlaces(
            pageSize = 1,
            contentTypeId = 39,
            lclsSystm1 = "FD",
            lclsSystm2 = "FD05",
            lclsSystm3 = "FD050100",
        )

        val requests = requestedUrls.map { parseQuery(it.query) }
        assertEquals(listOf("place-1", "place-2"), places.map(TourPlaceDto::contentId))
        assertEquals(2, requests.size)
        requests.forEach { query ->
            assertEquals("1", query["pageSize"])
            assertEquals("39", query["contentTypeId"])
            assertEquals("FD", query["lclsSystm1"])
            assertEquals("FD05", query["lclsSystm2"])
            assertEquals("FD050100", query["lclsSystm3"])
            assertFalse(query.containsKey("pageNo"))
            assertFalse(query.containsKey("numOfRows"))
        }
        assertFalse(requests[0].containsKey("cursor"))
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

    @Test
    fun useCasePropagatesFullQueryAndCacheSeparatesEveryDimension() = runTest {
        val dataSource = RecordingTourApiDataSource()
        val useCase = GetTourPlacesUseCase(DefaultTourRepository(dataSource))
        val baseQuery = RecordedTourQuery(
            pageSize = 50,
            contentTypeId = 39,
            lclsSystm1 = "FD",
            lclsSystm2 = "FD05",
            lclsSystm3 = "FD050100",
            maxPages = 1,
        )
        val distinctQueries = listOf(
            baseQuery.copy(pageSize = 51),
            baseQuery.copy(contentTypeId = 32),
            baseQuery.copy(lclsSystm1 = "FE"),
            baseQuery.copy(lclsSystm2 = "FD06"),
            baseQuery.copy(lclsSystm3 = "FD050200"),
            baseQuery.copy(maxPages = 2),
        )

        useCase.load(baseQuery)
        useCase.load(baseQuery)
        distinctQueries.forEach { useCase.load(it) }

        assertEquals(listOf(baseQuery) + distinctQueries, dataSource.requests)
    }

    @Test
    fun maxPagesCapsCursorRequestsAndClassificationFiltersReachEveryPage() = runTest {
        val requestedUrls = mutableListOf<URL>()
        val dataSource = stubbedDataSource(
            requestedUrls = requestedUrls,
            responses = ArrayDeque(
                listOf(
                    tourPage("cafe-1", "첫 카페", "next-2"),
                    tourPage("cafe-2", "두 번째 카페", "next-3"),
                ),
            ),
        )

        val places = dataSource.getPlaces(
            pageSize = 100,
            contentTypeId = 39,
            lclsSystm1 = "FD",
            lclsSystm2 = "FD05",
            lclsSystm3 = "FD050100",
            maxPages = 2,
        )

        assertEquals(listOf("cafe-1", "cafe-2"), places.map(TourPlaceDto::contentId))
        assertEquals(2, requestedUrls.size)
        requestedUrls.map { parseQuery(it.query) }.forEach { query ->
            assertEquals("100", query["pageSize"])
            assertEquals("39", query["contentTypeId"])
            assertEquals("FD", query["lclsSystm1"])
            assertEquals("FD05", query["lclsSystm2"])
            assertEquals("FD050100", query["lclsSystm3"])
        }
    }

    @Test
    fun omittedMaxPagesPreservesUnlimitedCursorPagination() = runTest {
        val requestedUrls = mutableListOf<URL>()
        val dataSource = stubbedDataSource(
            requestedUrls = requestedUrls,
            responses = ArrayDeque(
                listOf(
                    tourPage("first", "첫 장소", "next"),
                    tourPage("second", "두 번째 장소", null),
                ),
            ),
        )

        val places = dataSource.getPlaces(pageSize = 1, contentTypeId = 12)

        assertEquals(listOf("first", "second"), places.map(TourPlaceDto::contentId))
        assertEquals(listOf(null, "next"), requestedUrls.map { parseQuery(it.query)["cursor"] })
    }

    @Test
    fun rejectsInvalidMaxPagesBeforeOpeningAConnection() = runTest {
        var connectionCreated = false
        val dataSource = HttpTourApiDataSource("http://example.com") { url ->
            connectionCreated = true
            StubHttpURLConnection(url, tourPage("unused", "미사용", null))
        }

        val result = runCatching {
            dataSource.getPlaces(pageSize = 100, contentTypeId = 12, maxPages = 0)
        }

        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
        assertFalse(connectionCreated)
    }

    @Test
    fun defaultRepositoryRethrowsCancellation() = runTest {
        val expected = CancellationException("관광지 조회 취소")
        val repository = DefaultTourRepository(RecordingTourApiDataSource(failure = expected))
        var caught: CancellationException? = null

        try {
            repository.getPlaces(pageSize = 100, contentTypeId = 12)
        } catch (cancellation: CancellationException) {
            caught = cancellation
        }

        assertSame(expected, caught)
    }
}

private data class RecordedTourQuery(
    val pageSize: Int,
    val contentTypeId: Int,
    val lclsSystm1: String?,
    val lclsSystm2: String?,
    val lclsSystm3: String?,
    val maxPages: Int?,
)

private class CountingTourApiDataSource : TourApiDataSource {
    var requestCount = 0

    override suspend fun getPlaces(
        pageSize: Int,
        contentTypeId: Int,
        lclsSystm1: String?,
        lclsSystm2: String?,
        lclsSystm3: String?,
        maxPages: Int?,
    ): List<TourPlaceDto> {
        requestCount += 1
        return tourPlace(contentTypeId)
    }
}

private class RecordingTourApiDataSource(
    private val failure: Throwable? = null,
) : TourApiDataSource {
    val requests = mutableListOf<RecordedTourQuery>()

    override suspend fun getPlaces(
        pageSize: Int,
        contentTypeId: Int,
        lclsSystm1: String?,
        lclsSystm2: String?,
        lclsSystm3: String?,
        maxPages: Int?,
    ): List<TourPlaceDto> {
        requests += RecordedTourQuery(
            pageSize = pageSize,
            contentTypeId = contentTypeId,
            lclsSystm1 = lclsSystm1,
            lclsSystm2 = lclsSystm2,
            lclsSystm3 = lclsSystm3,
            maxPages = maxPages,
        )
        failure?.let { throw it }
        return tourPlace(contentTypeId)
    }
}

private fun tourPlace(contentTypeId: Int): List<TourPlaceDto> = listOf(
    TourPlaceDto(
        contentId = "1",
        title = "테스트 장소",
        address = "서울",
        addressDetail = "",
        firstImage = "",
        mapX = "126.0",
        mapY = "37.0",
        contentTypeId = contentTypeId.toString(),
    ),
)

private suspend fun GetTourPlacesUseCase.load(query: RecordedTourQuery) {
    invoke(
        pageSize = query.pageSize,
        contentTypeId = query.contentTypeId,
        lclsSystm1 = query.lclsSystm1,
        lclsSystm2 = query.lclsSystm2,
        lclsSystm3 = query.lclsSystm3,
        maxPages = query.maxPages,
    ).getOrThrow()
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

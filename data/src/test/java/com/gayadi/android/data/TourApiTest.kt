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
    fun useCasePropagatesFullQueryAndCacheSeparatesEveryDimension() = runTest {
        val dataSource = RecordingTourApiDataSource()
        val useCase = GetTourPlacesUseCase(DefaultTourRepository(dataSource))
        val baseQuery = RecordedTourQuery(
            numOfRows = 50,
            contentTypeId = 39,
            lclsSystm1 = "FD",
            lclsSystm2 = "FD05",
            lclsSystm3 = "FD050100",
            maxPages = 1,
        )
        val distinctQueries = listOf(
            baseQuery.copy(numOfRows = 51),
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
    fun maxPagesLimitsRequestsAndClassificationFiltersReachTheServer() = runTest {
        val requestedUrls = mutableListOf<URL>()
        val dataSource = stubbedDataSource(
            requestedUrls = requestedUrls,
            responses = ArrayDeque(listOf(tourPage("cafe", "테스트 카페", totalCount = 300))),
        )

        val places = dataSource.getPlaces(
            numOfRows = 100,
            contentTypeId = 39,
            lclsSystm1 = "FD",
            lclsSystm2 = "FD05",
            lclsSystm3 = "FD050100",
            maxPages = 1,
        )

        val query = parseQuery(requestedUrls.single().query)
        assertEquals(listOf("cafe"), places.map(TourPlaceDto::contentId))
        assertEquals("100", query["numOfRows"])
        assertEquals("1", query["pageNo"])
        assertEquals("39", query["contentTypeId"])
        assertEquals("FD", query["lclsSystm1"])
        assertEquals("FD05", query["lclsSystm2"])
        assertEquals("FD050100", query["lclsSystm3"])
    }

    @Test
    fun omittedMaxPagesPreservesUnlimitedPagination() = runTest {
        val requestedUrls = mutableListOf<URL>()
        val dataSource = stubbedDataSource(
            requestedUrls = requestedUrls,
            responses = ArrayDeque(
                listOf(
                    tourPage("first", "첫 장소", totalCount = 2),
                    tourPage("second", "두 번째 장소", totalCount = 2),
                ),
            ),
        )

        val places = dataSource.getPlaces(numOfRows = 1, contentTypeId = 12)

        assertEquals(listOf("first", "second"), places.map(TourPlaceDto::contentId))
        assertEquals(listOf("1", "2"), requestedUrls.map { parseQuery(it.query)["pageNo"] })
    }

    @Test
    fun rejectsInvalidMaxPagesBeforeOpeningAConnection() = runTest {
        var connectionCreated = false
        val dataSource = HttpTourApiDataSource("http://example.com") { url ->
            connectionCreated = true
            StubHttpURLConnection(url, tourPage("unused", "미사용", totalCount = 1))
        }

        val result = runCatching {
            dataSource.getPlaces(numOfRows = 100, contentTypeId = 12, maxPages = 0)
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
            repository.getPlaces(numOfRows = 100, contentTypeId = 12)
        } catch (cancellation: CancellationException) {
            caught = cancellation
        }

        assertSame(expected, caught)
    }
}

private data class RecordedTourQuery(
    val numOfRows: Int,
    val contentTypeId: Int,
    val lclsSystm1: String?,
    val lclsSystm2: String?,
    val lclsSystm3: String?,
    val maxPages: Int?,
)

private class RecordingTourApiDataSource(
    private val failure: Throwable? = null,
) : TourApiDataSource {
    val requests = mutableListOf<RecordedTourQuery>()

    override suspend fun getPlaces(
        numOfRows: Int,
        contentTypeId: Int,
        lclsSystm1: String?,
        lclsSystm2: String?,
        lclsSystm3: String?,
        maxPages: Int?,
    ): List<TourPlaceDto> {
        requests += RecordedTourQuery(
            numOfRows = numOfRows,
            contentTypeId = contentTypeId,
            lclsSystm1 = lclsSystm1,
            lclsSystm2 = lclsSystm2,
            lclsSystm3 = lclsSystm3,
            maxPages = maxPages,
        )
        failure?.let { throw it }
        return listOf(
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
    }
}

private suspend fun GetTourPlacesUseCase.load(query: RecordedTourQuery) {
    invoke(
        numOfRows = query.numOfRows,
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

private fun tourPage(
    contentId: String,
    title: String,
    totalCount: Int,
): String = JSONObject().apply {
    put("items", JSONArray().put(JSONObject().apply {
        put("contentId", contentId)
        put("title", title)
    }))
    put("totalCount", totalCount)
}.toString()

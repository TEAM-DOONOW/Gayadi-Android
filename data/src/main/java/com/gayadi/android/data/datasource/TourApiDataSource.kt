package com.gayadi.android.data.datasource

import com.gayadi.android.data.model.TourPlaceDto
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import org.json.JSONObject

interface TourApiDataSource {
    suspend fun getPlaces(
        numOfRows: Int,
        contentTypeId: Int,
        lclsSystm1: String? = null,
        lclsSystm2: String? = null,
        lclsSystm3: String? = null,
        maxPages: Int? = null,
    ): List<TourPlaceDto>
}

class HttpTourApiDataSource(
    baseUrl: String,
    private val connectionFactory: (URL) -> HttpURLConnection = { url ->
        url.openConnection() as HttpURLConnection
    },
) : TourApiDataSource {
    private val normalizedBaseUrl = baseUrl.trimEnd('/')

    override suspend fun getPlaces(
        numOfRows: Int,
        contentTypeId: Int,
        lclsSystm1: String?,
        lclsSystm2: String?,
        lclsSystm3: String?,
        maxPages: Int?,
    ): List<TourPlaceDto> =
        withContext(Dispatchers.IO) {
            require(normalizedBaseUrl.isNotBlank()) { "관광 API 서버 주소가 설정되지 않았습니다." }
            require(numOfRows in 1..MAX_NUM_OF_ROWS) {
                "관광 API 페이지 크기는 1개 이상 ${MAX_NUM_OF_ROWS}개 이하여야 합니다."
            }
            require(maxPages == null || maxPages >= 1) {
                "관광 API 최대 페이지 수는 1개 이상이어야 합니다."
            }

            val firstPage = requestPage(
                pageNo = 1,
                numOfRows = numOfRows,
                contentTypeId = contentTypeId,
                lclsSystm1 = lclsSystm1,
                lclsSystm2 = lclsSystm2,
                lclsSystm3 = lclsSystm3,
            )
            val availablePages = (firstPage.totalCount + numOfRows - 1) / numOfRows
            val totalPages = maxPages?.let { minOf(availablePages, it) } ?: availablePages
            if (totalPages <= 1) {
                return@withContext firstPage.items
            }

            val semaphore = Semaphore(MAX_CONCURRENT_REQUESTS)
            val remainingItems = coroutineScope {
                (2..totalPages).map { pageNo ->
                    async {
                        semaphore.withPermit {
                            requestPage(
                                pageNo = pageNo,
                                numOfRows = numOfRows,
                                contentTypeId = contentTypeId,
                                lclsSystm1 = lclsSystm1,
                                lclsSystm2 = lclsSystm2,
                                lclsSystm3 = lclsSystm3,
                            ).items
                        }
                    }
                }.awaitAll().flatten()
            }
            firstPage.items + remainingItems
        }

    private fun requestPage(
        pageNo: Int,
        numOfRows: Int,
        contentTypeId: Int,
        lclsSystm1: String?,
        lclsSystm2: String?,
        lclsSystm3: String?,
    ): TourPage {
        val requestUrl = URL(buildString {
            append("$normalizedBaseUrl/api/v1/tour/areas")
            append("?numOfRows=$numOfRows&pageNo=$pageNo&contentTypeId=$contentTypeId")
            appendQueryParameter("lclsSystm1", lclsSystm1)
            appendQueryParameter("lclsSystm2", lclsSystm2)
            appendQueryParameter("lclsSystm3", lclsSystm3)
        })
        val connection = connectionFactory(requestUrl).apply {
            requestMethod = "GET"
            connectTimeout = CONNECT_TIMEOUT_MILLIS
            readTimeout = READ_TIMEOUT_MILLIS
            setRequestProperty("Accept", "application/json")
        }

        return try {
            val statusCode = connection.responseCode
            if (statusCode !in 200..299) {
                val detail = connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                throw IllegalStateException(
                    "관광 API 요청에 실패했습니다. (HTTP $statusCode)" +
                        detail.takeIf(String::isNotBlank)?.let { " $it" }.orEmpty(),
                )
            }
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            val root = JSONObject(body)
            TourPage(
                items = parsePlaces(root),
                totalCount = root.optInt("totalCount"),
            )
        } finally {
            connection.disconnect()
        }
    }

    private fun StringBuilder.appendQueryParameter(name: String, value: String?) {
        value?.takeIf(String::isNotBlank)?.let {
            append('&')
            append(name)
            append('=')
            append(URLEncoder.encode(it, StandardCharsets.UTF_8.name()))
        }
    }

    internal fun parsePlaces(body: String): List<TourPlaceDto> =
        parsePlaces(JSONObject(body))

    private fun parsePlaces(root: JSONObject): List<TourPlaceDto> {
        val items = root.getJSONArray("items")
        return buildList(items.length()) {
            repeat(items.length()) { index ->
                val item = items.getJSONObject(index)
                add(
                    TourPlaceDto(
                        contentId = item.optString("contentId"),
                        title = item.optString("title"),
                        address = item.optString("address"),
                        addressDetail = item.optString("addressDetail"),
                        firstImage = item.optString("firstImage"),
                        mapX = item.optString("mapX"),
                        mapY = item.optString("mapY"),
                        contentTypeId = item.optString("contentTypeId"),
                        lclsSystm1 = item.optString("lclsSystm1"),
                        lclsSystm2 = item.optString("lclsSystm2"),
                        lclsSystm3 = item.optString("lclsSystm3"),
                    ),
                )
            }
        }
    }

    private data class TourPage(
        val items: List<TourPlaceDto>,
        val totalCount: Int,
    )

    private companion object {
        const val MAX_NUM_OF_ROWS = 100
        const val MAX_CONCURRENT_REQUESTS = 8
        const val CONNECT_TIMEOUT_MILLIS = 10_000
        const val READ_TIMEOUT_MILLIS = 15_000
    }
}

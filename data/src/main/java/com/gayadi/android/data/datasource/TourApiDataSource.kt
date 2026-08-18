package com.gayadi.android.data.datasource

import com.gayadi.android.data.model.TourPlaceDto
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import org.json.JSONObject

interface TourApiDataSource {
    suspend fun getPlaces(numOfRows: Int, contentTypeId: Int): List<TourPlaceDto>
}

class HttpTourApiDataSource(
    baseUrl: String,
) : TourApiDataSource {
    private val normalizedBaseUrl = baseUrl.trimEnd('/')

    override suspend fun getPlaces(numOfRows: Int, contentTypeId: Int): List<TourPlaceDto> =
        withContext(Dispatchers.IO) {
            require(normalizedBaseUrl.isNotBlank()) { "관광 API 서버 주소가 설정되지 않았습니다." }
            require(numOfRows in 1..MAX_NUM_OF_ROWS) {
                "관광 API 페이지 크기는 1개 이상 ${MAX_NUM_OF_ROWS}개 이하여야 합니다."
            }

            val firstPage = requestPage(
                pageNo = 1,
                numOfRows = numOfRows,
                contentTypeId = contentTypeId,
            )
            val totalPages = (firstPage.totalCount + numOfRows - 1) / numOfRows
            if (totalPages <= 1) {
                return@withContext firstPage.items
            }

            val semaphore = Semaphore(MAX_CONCURRENT_REQUESTS)
            val remainingItems = coroutineScope {
                (2..totalPages).map { pageNo ->
                    async {
                        semaphore.withPermit {
                            requestPage(pageNo, numOfRows, contentTypeId).items
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
    ): TourPage {
        val requestUrl = URL(
            "$normalizedBaseUrl/api/v1/tour/areas" +
                "?numOfRows=$numOfRows&pageNo=$pageNo&contentTypeId=$contentTypeId",
        )
        val connection = (requestUrl.openConnection() as HttpURLConnection).apply {
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

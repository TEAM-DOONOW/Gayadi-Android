package com.gayadi.android.data.datasource

import com.gayadi.android.data.model.TourPlaceDto
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.net.URL
import java.nio.charset.StandardCharsets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.json.JSONObject

interface TourApiDataSource {
    suspend fun getPlaces(pageSize: Int, contentTypeId: Int): List<TourPlaceDto>
}

class HttpTourApiDataSource(
    baseUrl: String,
    private val connectionFactory: (URL) -> HttpURLConnection = { url ->
        url.openConnection() as HttpURLConnection
    },
) : TourApiDataSource {
    private val normalizedBaseUrl = baseUrl.trimEnd('/')

    override suspend fun getPlaces(pageSize: Int, contentTypeId: Int): List<TourPlaceDto> =
        withContext(Dispatchers.IO) {
            require(normalizedBaseUrl.isNotBlank()) { "관광 API 서버 주소가 설정되지 않았습니다." }
            require(pageSize in 1..MAX_PAGE_SIZE) {
                "관광 API 페이지 크기는 1개 이상 ${MAX_PAGE_SIZE}개 이하여야 합니다."
            }

            val places = mutableListOf<TourPlaceDto>()
            val seenCursors = mutableSetOf<String>()
            var cursor: String? = null
            do {
                currentCoroutineContext().ensureActive()
                val page = requestPage(pageSize, cursor, contentTypeId)
                places += page.items
                val nextCursor = page.nextCursor
                check(nextCursor == null || seenCursors.add(nextCursor)) {
                    "관광 API가 동일한 다음 페이지 커서를 반복했습니다."
                }
                cursor = nextCursor
            } while (cursor != null)
            places
        }

    private fun requestPage(
        pageSize: Int,
        cursor: String?,
        contentTypeId: Int,
    ): TourPage {
        val requestUrl = URL(
            "$normalizedBaseUrl/api/v1/tour/areas" +
                "?pageSize=$pageSize&contentTypeId=$contentTypeId" +
                cursor?.let { "&cursor=${it.urlEncoded()}" }.orEmpty(),
        )
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
                nextCursor = root.optNullableString("nextCursor"),
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

    private fun JSONObject.optNullableString(key: String): String? =
        if (!has(key) || isNull(key)) null else optString(key).takeIf(String::isNotBlank)

    private fun String.urlEncoded(): String =
        URLEncoder.encode(this, StandardCharsets.UTF_8.name())

    private data class TourPage(
        val items: List<TourPlaceDto>,
        val nextCursor: String?,
    )

    private companion object {
        const val MAX_PAGE_SIZE = 100
        const val CONNECT_TIMEOUT_MILLIS = 10_000
        const val READ_TIMEOUT_MILLIS = 15_000
    }
}

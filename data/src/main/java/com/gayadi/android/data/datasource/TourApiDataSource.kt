package com.gayadi.android.data.datasource

import com.gayadi.android.data.model.TourPlaceDto
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
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
            val requestUrl = URL(
                "$normalizedBaseUrl/api/v1/tour/areas" +
                    "?numOfRows=$numOfRows&contentTypeId=$contentTypeId",
            )
            val connection = (requestUrl.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = CONNECT_TIMEOUT_MILLIS
                readTimeout = READ_TIMEOUT_MILLIS
                setRequestProperty("Accept", "application/json")
            }

            try {
                val statusCode = connection.responseCode
                if (statusCode !in 200..299) {
                    val detail = connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                    throw IllegalStateException(
                        "관광 API 요청에 실패했습니다. (HTTP $statusCode)" +
                            detail.takeIf(String::isNotBlank)?.let { " $it" }.orEmpty(),
                    )
                }
                val body = connection.inputStream.bufferedReader().use { it.readText() }
                parsePlaces(body)
            } finally {
                connection.disconnect()
            }
        }

    internal fun parsePlaces(body: String): List<TourPlaceDto> {
        val items = JSONObject(body).getJSONArray("items")
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

    private companion object {
        const val CONNECT_TIMEOUT_MILLIS = 10_000
        const val READ_TIMEOUT_MILLIS = 15_000
    }
}

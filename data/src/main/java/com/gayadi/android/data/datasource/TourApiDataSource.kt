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
    suspend fun getPlaces(
        pageSize: Int,
        contentTypeId: Int,
        lclsSystm1: String? = null,
        lclsSystm2: String? = null,
        lclsSystm3: String? = null,
        maxPages: Int? = null,
        regionName: String? = null,
    ): List<TourPlaceDto>

    suspend fun getNearbyPlaces(
        pageSize: Int,
        mapX: String,
        mapY: String,
        radius: Int,
        arrange: String = "E",
        contentTypeId: String? = null,
        maxPages: Int? = null,
    ): List<TourPlaceDto> = error("주변 관광 API가 구현되지 않았습니다.")

    suspend fun searchPlaces(
        pageSize: Int,
        keyword: String,
        arrange: String = "C",
        lDongRegnCd: String? = null,
        lDongSignguCd: String? = null,
        lclsSystm1: String? = null,
        lclsSystm2: String? = null,
        lclsSystm3: String? = null,
        maxPages: Int? = null,
    ): List<TourPlaceDto> = error("키워드 관광 API가 구현되지 않았습니다.")
}

class HttpTourApiDataSource(
    baseUrl: String,
    private val accessToken: (suspend () -> String)? = null,
    private val connectionFactory: (URL) -> HttpURLConnection = { url ->
        url.openConnection() as HttpURLConnection
    },
) : TourApiDataSource {
    private val normalizedBaseUrl = baseUrl.trimEnd('/')

    override suspend fun getPlaces(
        pageSize: Int,
        contentTypeId: Int,
        lclsSystm1: String?,
        lclsSystm2: String?,
        lclsSystm3: String?,
        maxPages: Int?,
        regionName: String?,
    ): List<TourPlaceDto> =
        withContext(Dispatchers.IO) {
            require(normalizedBaseUrl.isNotBlank()) { "관광 API 서버 주소가 설정되지 않았습니다." }
            require(pageSize in 1..MAX_PAGE_SIZE) {
                "관광 API 페이지 크기는 1개 이상 ${MAX_PAGE_SIZE}개 이하여야 합니다."
            }
            require(maxPages == null || maxPages >= 1) {
                "관광 API 최대 페이지 수는 1개 이상이어야 합니다."
            }

            val places = mutableListOf<TourPlaceDto>()
            val seenCursors = mutableSetOf<String>()
            var cursor: String? = null
            var pagesLoaded = 0
            do {
                currentCoroutineContext().ensureActive()
                val page = requestPage(
                    pageSize = pageSize,
                    cursor = cursor,
                    contentTypeId = contentTypeId,
                    lclsSystm1 = lclsSystm1,
                    lclsSystm2 = lclsSystm2,
                    lclsSystm3 = lclsSystm3,
                    regionName = regionName,
                )
                pagesLoaded += 1
                places += page.items
                val nextCursor = page.nextCursor.takeUnless {
                    maxPages != null && pagesLoaded >= maxPages
                }
                check(nextCursor == null || seenCursors.add(nextCursor)) {
                    "관광 API가 동일한 다음 페이지 커서를 반복했습니다."
                }
                cursor = nextCursor
            } while (cursor != null)
            places
        }

    override suspend fun getNearbyPlaces(
        pageSize: Int,
        mapX: String,
        mapY: String,
        radius: Int,
        arrange: String,
        contentTypeId: String?,
        maxPages: Int?,
    ): List<TourPlaceDto> = withContext(Dispatchers.IO) {
        require(normalizedBaseUrl.isNotBlank()) { "관광 API 서버 주소가 설정되지 않았습니다." }
        require(pageSize in 1..MAX_PAGE_SIZE) {
            "관광 API 페이지 크기는 1개 이상 ${MAX_PAGE_SIZE}개 이하여야 합니다."
        }
        require(radius in 1..MAX_RADIUS_METERS) {
            "주변 관광 API 반경은 1m 이상 ${MAX_RADIUS_METERS}m 이하여야 합니다."
        }
        require(mapX.isNotBlank() && mapY.isNotBlank()) { "주변 관광 API 중심 좌표가 필요합니다." }
        require(arrange in VALID_ARRANGES) { "지원하지 않는 주변 관광 API 정렬 방식입니다: $arrange" }

        val places = mutableListOf<TourPlaceDto>()
        val seenCursors = mutableSetOf<String>()
        var cursor: String? = null
        var pagesLoaded = 0
        do {
            currentCoroutineContext().ensureActive()
            val page = requestNearbyPage(
                pageSize = pageSize,
                cursor = cursor,
                arrange = arrange,
                mapX = mapX,
                mapY = mapY,
                radius = radius,
                contentTypeId = contentTypeId,
            )
            pagesLoaded += 1
            places += page.items
            val nextCursor = page.nextCursor.takeUnless {
                maxPages != null && pagesLoaded >= maxPages
            }
            check(nextCursor == null || seenCursors.add(nextCursor)) {
                "관광 API가 동일한 다음 페이지 커서를 반복했습니다."
            }
            cursor = nextCursor
        } while (cursor != null)
        places
    }

    override suspend fun searchPlaces(
        pageSize: Int,
        keyword: String,
        arrange: String,
        lDongRegnCd: String?,
        lDongSignguCd: String?,
        lclsSystm1: String?,
        lclsSystm2: String?,
        lclsSystm3: String?,
        maxPages: Int?,
    ): List<TourPlaceDto> = withContext(Dispatchers.IO) {
        require(normalizedBaseUrl.isNotBlank()) { "관광 API 서버 주소가 설정되지 않았습니다." }
        require(keyword.isNotBlank()) { "키워드가 필요합니다." }
        require(pageSize in 1..MAX_PAGE_SIZE) {
            "관광 API 페이지 크기는 1개 이상 ${MAX_PAGE_SIZE}개 이하여야 합니다."
        }
        require(arrange in KEYWORD_ARRANGES) { "지원하지 않는 키워드 관광 API 정렬 방식입니다: $arrange" }

        val places = mutableListOf<TourPlaceDto>()
        val seenCursors = mutableSetOf<String>()
        var cursor: String? = null
        var pagesLoaded = 0
        do {
            currentCoroutineContext().ensureActive()
            val page = requestKeywordPage(
                pageSize = pageSize,
                keyword = keyword,
                arrange = arrange,
                cursor = cursor,
                lDongRegnCd = lDongRegnCd,
                lDongSignguCd = lDongSignguCd,
                lclsSystm1 = lclsSystm1,
                lclsSystm2 = lclsSystm2,
                lclsSystm3 = lclsSystm3,
            )
            pagesLoaded += 1
            places += page.items
            val nextCursor = page.nextCursor.takeUnless { maxPages != null && pagesLoaded >= maxPages }
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
        lclsSystm1: String?,
        lclsSystm2: String?,
        lclsSystm3: String?,
        regionName: String?,
    ): TourPage {
        require(!regionName.isNullOrBlank()) { "관광 API 지역명이 필요합니다." }
        val requestPageSize = pageSize.coerceAtMost(AREAS_MAX_PAGE_SIZE)
        val requestUrl = URL(buildString {
            append("$normalizedBaseUrl/api/v1/tour/areas?pageSize=$requestPageSize&contentTypeId=$contentTypeId")
            appendQueryParameter("regionName", regionName)
            appendQueryParameter("lclsSystm1", lclsSystm1)
            appendQueryParameter("lclsSystm2", lclsSystm2)
            appendQueryParameter("lclsSystm3", lclsSystm3)
            cursor?.let { append("&cursor=${it.urlEncoded()}") }
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
                nextCursor = root.optNullableString("nextCursor"),
            )
        } finally {
            connection.disconnect()
        }
    }

    private suspend fun requestNearbyPage(
        pageSize: Int,
        cursor: String?,
        arrange: String,
        mapX: String,
        mapY: String,
        radius: Int,
        contentTypeId: String?,
    ): TourPage {
        val requestUrl = URL(buildString {
            append("$normalizedBaseUrl/api/v1/tour/locations")
            append("?pageSize=$pageSize&arrange=${arrange.urlEncoded()}")
            append("&mapX=${mapX.urlEncoded()}&mapY=${mapY.urlEncoded()}&radius=$radius")
            contentTypeId?.takeIf(String::isNotBlank)?.let {
                append("&contentTypeId=${it.urlEncoded()}")
            }
            cursor?.let { append("&cursor=${it.urlEncoded()}") }
        })
        return executePageRequest(requestUrl, "주변 관광 API", authenticated = true)
    }

    private suspend fun requestKeywordPage(
        pageSize: Int,
        keyword: String,
        arrange: String,
        cursor: String?,
        lDongRegnCd: String?,
        lDongSignguCd: String?,
        lclsSystm1: String?,
        lclsSystm2: String?,
        lclsSystm3: String?,
    ): TourPage {
        val requestUrl = URL(buildString {
            append("$normalizedBaseUrl/api/v1/tour/keywords")
            append("?pageSize=$pageSize&arrange=${arrange.urlEncoded()}&keyword=${keyword.urlEncoded()}")
            appendQueryParameter("lDongRegnCd", lDongRegnCd)
            appendQueryParameter("lDongSignguCd", lDongSignguCd)
            appendQueryParameter("lclsSystm1", lclsSystm1)
            appendQueryParameter("lclsSystm2", lclsSystm2)
            appendQueryParameter("lclsSystm3", lclsSystm3)
            cursor?.let { append("&cursor=${it.urlEncoded()}") }
        })
        return executePageRequest(requestUrl, "키워드 관광 API", authenticated = true)
    }

    private suspend fun executePageRequest(
        requestUrl: URL,
        apiName: String,
        authenticated: Boolean = false,
    ): TourPage {
        val connection = connectionFactory(requestUrl).apply {
            requestMethod = "GET"
            connectTimeout = CONNECT_TIMEOUT_MILLIS
            readTimeout = READ_TIMEOUT_MILLIS
            setRequestProperty("Accept", "application/json")
            if (authenticated) {
                accessToken?.let { tokenProvider ->
                    val token = tokenProvider()
                    require(token.isNotBlank()) { "로그인이 필요해요." }
                    setRequestProperty("Authorization", "Bearer $token")
                }
            }
        }
        return try {
            val statusCode = connection.responseCode
            if (statusCode !in 200..299) {
                val detail = connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                throw IllegalStateException(
                    "$apiName 요청에 실패했습니다. (HTTP $statusCode)" +
                        detail.takeIf(String::isNotBlank)?.let { " $it" }.orEmpty(),
                )
            }
            val root = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
            TourPage(parsePlaces(root), root.optNullableString("nextCursor"))
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
                        contentId = item.optNullableString("placeId")
                            ?: item.optString("contentId"),
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
                        lDongRegnCd = item.optString("lDongRegnCd"),
                        lDongSignguCd = item.optString("lDongSignguCd"),
                        distanceMeters = item.optNullableInt("dist"),
                        crowdLevel = item.optString("crowdLevel"),
                        concentrationScore = item.optNullableInt("concentrationScore"),
                        crowdSource = item.optString("crowdSource"),
                        crowdEstimated = item.optBoolean("crowdEstimated"),
                        crowdProviderDataAvailable = item.optBoolean("crowdProviderDataAvailable"),
                        crowdConfidence = item.optString("crowdConfidence"),
                        crowdMessage = item.optString("crowdMessage"),
                    ),
                )
            }
        }
    }

    private fun JSONObject.optNullableString(key: String): String? =
        if (!has(key) || isNull(key)) null else optString(key).takeIf(String::isNotBlank)

    private fun JSONObject.optNullableInt(key: String): Int? =
        if (!has(key) || isNull(key)) null else optInt(key).takeIf { it >= 0 }

    private fun String.urlEncoded(): String =
        URLEncoder.encode(this, StandardCharsets.UTF_8.name())

    private data class TourPage(
        val items: List<TourPlaceDto>,
        val nextCursor: String?,
    )

    private companion object {
        const val STAY_CONTENT_TYPE_ID = 32
        const val MAX_PAGE_SIZE = 100
        const val AREAS_MAX_PAGE_SIZE = 20
        const val MAX_RADIUS_METERS = 20_000
        const val CONNECT_TIMEOUT_MILLIS = 10_000
        const val READ_TIMEOUT_MILLIS = 15_000
        val VALID_ARRANGES = setOf("A", "C", "D", "E")
        val KEYWORD_ARRANGES = setOf("A", "C", "D")
    }
}

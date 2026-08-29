package com.gayadi.android.data.datasource

import com.gayadi.android.data.model.TourPlaceDto
import com.gayadi.android.data.remote.GayadiHttpClient
import org.json.JSONObject

/** Adapts the server's canonical place IDs to the existing place presentation model. */
class ServerPlaceApiDataSource internal constructor(
    private val client: RemoteJsonClient,
) : TourApiDataSource {
    constructor(httpClient: GayadiHttpClient) : this(GayadiRemoteJsonClient(httpClient))

    override suspend fun getPlaces(
        pageSize: Int,
        contentTypeId: Int,
        lclsSystm1: String?,
        lclsSystm2: String?,
        lclsSystm3: String?,
        maxPages: Int?,
    ): List<TourPlaceDto> {
        require(pageSize > 0) { "장소 페이지 크기는 1 이상이어야 합니다." }
        require(maxPages == null || maxPages > 0) { "장소 페이지 수는 1 이상이어야 합니다." }
        val categories = categoriesFor(contentTypeId, lclsSystm2)
        return categories.flatMap { category ->
            loadCategory(category, pageSize.coerceAtMost(MAX_SERVER_PAGE_SIZE), maxPages)
        }.distinctBy(TourPlaceDto::contentId)
    }

    private suspend fun loadCategory(
        category: String,
        limit: Int,
        maxPages: Int?,
    ): List<TourPlaceDto> = buildList {
        var cursor: String? = null
        var pageCount = 0
        do {
            val response = client.getObject(
                path = PLACES_PATH,
                query = mapOf(
                    "category" to category,
                    "cursor" to cursor,
                    "limit" to limit.toString(),
                ),
                authenticated = false,
            )
            val items = response.getJSONArray("items")
            repeat(items.length()) { index -> add(items.getJSONObject(index).toTourPlaceDto()) }
            pageCount += 1
            cursor = response.optNullableValue("nextCursor")
                ?.takeUnless { maxPages != null && pageCount >= maxPages }
        } while (cursor != null)
    }

    private fun JSONObject.toTourPlaceDto(): TourPlaceDto {
        val categoryCode = optString("categoryCode")
        return TourPlaceDto(
            contentId = getLong("id").toString(),
            title = getString("name"),
            address = optString("address"),
            addressDetail = optString("roadAddress"),
            firstImage = optString("imageUrl"),
            mapX = optNullableValue("longitude").orEmpty(),
            mapY = optNullableValue("latitude").orEmpty(),
            contentTypeId = when (categoryCode) {
                "RESTAURANT", "CAFE" -> "39"
                "ACCOMMODATION" -> "32"
                else -> "12"
            },
            lclsSystm1 = if (categoryCode == "RESTAURANT" || categoryCode == "CAFE") "FD" else "",
            lclsSystm2 = when (categoryCode) {
                "RESTAURANT" -> "FD01"
                "CAFE" -> "FD05"
                else -> ""
            },
        )
    }

    private fun JSONObject.optNullableValue(key: String): String? =
        takeIf { has(key) && !isNull(key) }?.get(key)?.toString()?.takeIf(String::isNotBlank)

    private fun categoriesFor(contentTypeId: Int, foodCategory: String?): List<String> = when (contentTypeId) {
        12 -> listOf("ATTRACTION")
        32 -> listOf("ACCOMMODATION")
        39 -> when {
            foodCategory?.startsWith("FD05", ignoreCase = true) == true -> listOf("CAFE")
            foodCategory?.startsWith("FD01", ignoreCase = true) == true -> listOf("RESTAURANT")
            else -> listOf("RESTAURANT", "CAFE")
        }
        else -> listOf("ETC")
    }

    private companion object {
        const val PLACES_PATH = "/api/v1/places"
        const val MAX_SERVER_PAGE_SIZE = 50
    }
}

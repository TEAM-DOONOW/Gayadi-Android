package com.gayadi.android.data.datasource

import com.gayadi.android.data.model.TourPlaceDto
import java.net.URLEncoder
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import org.json.JSONObject

/** Adapts the server's canonical place IDs to the existing place presentation model. */
class ServerPlaceApiDataSource(private val client: GayadiApiClient) : TourApiDataSource {

    override suspend fun getPlaces(
        pageSize: Int,
        contentTypeId: Int,
        lclsSystm1: String?,
        lclsSystm2: String?,
        lclsSystm3: String?,
        maxPages: Int?,
        regionName: String?,
    ): List<TourPlaceDto> {
        require(pageSize > 0) { "장소 페이지 크기는 1 이상이어야 합니다." }
        require(maxPages == null || maxPages > 0) { "장소 페이지 수는 1 이상이어야 합니다." }
        val categories = categoriesFor(contentTypeId, lclsSystm2)
        return categories.flatMap { category ->
            loadPlacesPreferringRegion(
                category = category,
                regionName = regionName,
                limit = pageSize.coerceAtMost(MAX_SERVER_PAGE_SIZE),
                maxPages = maxPages,
            )
        }.distinctBy(TourPlaceDto::contentId)
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
    ): List<TourPlaceDto> {
        require(pageSize > 0) { "장소 페이지 크기는 1 이상이어야 합니다." }
        require(keyword.isNotBlank()) { "키워드가 필요합니다." }
        require(arrange in VALID_ARRANGES) { "지원하지 않는 장소 정렬 방식입니다: $arrange" }
        require(maxPages == null || maxPages > 0) { "장소 페이지 수는 1 이상이어야 합니다." }

        val categories = foodCategories(lclsSystm1, lclsSystm2)
        val places = (categories.ifEmpty { listOf(null) }).flatMap { category ->
            loadPlaces(
                filters = mapOf("query" to keyword.trim(), "category" to category),
                limit = pageSize.coerceAtMost(MAX_SERVER_PAGE_SIZE),
                maxPages = maxPages,
            )
        }.distinctBy(TourPlaceDto::contentId)
        return if (arrange == "A") places.sortedBy { it.title } else places
    }

    override suspend fun getNearbyPlaces(
        pageSize: Int,
        mapX: String,
        mapY: String,
        radius: Int,
        arrange: String,
        contentTypeId: String?,
        maxPages: Int?,
    ): List<TourPlaceDto> {
        require(pageSize > 0) { "장소 페이지 크기는 1 이상이어야 합니다." }
        val longitude = mapX.toDoubleOrNull()
        val latitude = mapY.toDoubleOrNull()
        require(longitude != null && latitude != null) { "주변 장소 중심 좌표가 필요합니다." }
        require(radius in 1..MAX_RADIUS_METERS) {
            "주변 장소 반경은 1m 이상 ${MAX_RADIUS_METERS}m 이하여야 합니다."
        }
        require(arrange in VALID_ARRANGES) { "지원하지 않는 장소 정렬 방식입니다: $arrange" }
        require(maxPages == null || maxPages > 0) { "장소 페이지 수는 1 이상이어야 합니다." }

        val categories = contentTypeId?.toIntOrNull()?.let { categoriesFor(it, null) }.orEmpty()
        val candidates = (categories.ifEmpty { listOf(null) }).flatMap { category ->
            loadPlaces(
                filters = mapOf("category" to category),
                limit = MAX_SERVER_PAGE_SIZE,
                maxPages = maxPages,
            )
        }.distinctBy(TourPlaceDto::contentId)
        val nearby = candidates.mapNotNull { place ->
            val placeLongitude = place.mapX.toDoubleOrNull() ?: return@mapNotNull null
            val placeLatitude = place.mapY.toDoubleOrNull() ?: return@mapNotNull null
            val distance = distanceMeters(latitude, longitude, placeLatitude, placeLongitude)
            place.copy(distanceMeters = distance).takeIf { distance <= radius }
        }
        val sorted = when (arrange) {
            "A" -> nearby.sortedBy { it.title }
            "E" -> nearby.sortedBy { it.distanceMeters }
            else -> nearby
        }
        val resultLimit = maxPages?.let { pages -> pageSize * pages } ?: Int.MAX_VALUE
        return sorted.take(resultLimit)
    }

    private suspend fun loadPlacesPreferringRegion(
        category: String?,
        regionName: String?,
        limit: Int,
        maxPages: Int?,
    ): List<TourPlaceDto> {
        val region = regionName?.trim()?.takeIf { it.isNotEmpty() }
        val filtered = loadPlaces(
            filters = mapOf("category" to category, "region" to region),
            limit = limit,
            maxPages = maxPages,
        )
        if (filtered.isNotEmpty() || region == null) return filtered
        return loadPlaces(
            filters = mapOf("category" to category),
            limit = limit,
            maxPages = maxPages,
        )
    }

    private suspend fun loadPlaces(
        filters: Map<String, String?>,
        limit: Int,
        maxPages: Int?,
    ): List<TourPlaceDto> = buildList {
        var cursor: String? = null
        var pageCount = 0
        do {
            val query = (filters + mapOf("cursor" to cursor, "limit" to limit.toString()))
                .filterValues { it != null }.entries.joinToString("&") {
                    it.key + "=" + URLEncoder.encode(it.value, Charsets.UTF_8.name())
                }
            val response = JSONObject(client.request("GET", "$PLACES_PATH?$query", authenticated = false))
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
        14 -> listOf("CULTURE")
        15, 25, 28 -> listOf("ATTRACTION")
        32 -> listOf("ACCOMMODATION")
        38 -> listOf("SHOPPING")
        39 -> when {
            foodCategory?.startsWith("FD05", ignoreCase = true) == true -> listOf("CAFE")
            foodCategory?.startsWith("FD01", ignoreCase = true) == true -> listOf("RESTAURANT")
            else -> listOf("RESTAURANT", "CAFE")
        }
        else -> listOf("ETC")
    }

    private fun foodCategories(level1: String?, level2: String?): List<String?> = when {
        level2?.startsWith("FD05", ignoreCase = true) == true -> listOf("CAFE")
        level2?.startsWith("FD01", ignoreCase = true) == true -> listOf("RESTAURANT")
        level1?.startsWith("FD", ignoreCase = true) == true -> listOf("RESTAURANT", "CAFE")
        else -> emptyList()
    }

    private fun distanceMeters(
        fromLatitude: Double,
        fromLongitude: Double,
        toLatitude: Double,
        toLongitude: Double,
    ): Int {
        val latitudeDelta = Math.toRadians(toLatitude - fromLatitude)
        val longitudeDelta = Math.toRadians(toLongitude - fromLongitude)
        val fromLatitudeRadians = Math.toRadians(fromLatitude)
        val toLatitudeRadians = Math.toRadians(toLatitude)
        val haversine = sin(latitudeDelta / 2) * sin(latitudeDelta / 2) +
            cos(fromLatitudeRadians) * cos(toLatitudeRadians) *
            sin(longitudeDelta / 2) * sin(longitudeDelta / 2)
        val centralAngle = 2 * atan2(sqrt(haversine), sqrt(1 - haversine))
        return (EARTH_RADIUS_METERS * centralAngle).toInt()
    }

    private companion object {
        const val PLACES_PATH = "/api/v1/places"
        const val MAX_SERVER_PAGE_SIZE = 50
        const val MAX_RADIUS_METERS = 20_000
        const val EARTH_RADIUS_METERS = 6_371_000.0
        val VALID_ARRANGES = setOf("A", "C", "D", "E")
    }
}

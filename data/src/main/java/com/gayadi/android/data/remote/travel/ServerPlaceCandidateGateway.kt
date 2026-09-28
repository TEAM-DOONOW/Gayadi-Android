package com.gayadi.android.data.remote.travel

import com.gayadi.android.data.mapper.toWeatherResult
import com.gayadi.android.data.datasource.GayadiApiClient
import com.gayadi.android.data.datasource.TourApiDataSource
import com.gayadi.android.data.mapper.toDomain
import com.gayadi.android.domain.error.rethrowCancellation
import com.gayadi.android.domain.model.RouteTransportMode
import com.gayadi.android.domain.model.TourPlace
import com.gayadi.android.domain.repository.*
import org.json.JSONObject

class ServerPlaceCandidateGateway(
    private val http: TravelJsonTransport,
    private val diagnostic: (String) -> Unit = {},
    private val tourDiscovery: TourApiDataSource? = null,
) : PlaceCandidateGateway {
    constructor(client: GayadiApiClient, diagnostic: (String) -> Unit = {}, tourDiscovery: TourApiDataSource? = null) :
        this(GayadiTravelJsonTransport(client), diagnostic, tourDiscovery)

    private val forecasts = mutableMapOf<Pair<String, Int>, Pair<Long, Map<String, TourPlace>>>()

    private suspend fun discoverForecasts(query: PlaceCandidateQuery): Map<String, TourPlace> {
        val discovery = tourDiscovery ?: return emptyMap()
        if (query.region.isBlank()) return emptyMap()
        val contentType = when (query.category) {
            "RESTAURANT", "CAFE" -> 39
            "ACCOMMODATION" -> 32
            else -> 12
        }
        val key = query.region to contentType
        val cached = forecasts[key]
        if (cached != null && System.nanoTime() - cached.first < 300_000_000_000L) return cached.second
        return try {
            // Discovery supplies current forecast metadata and canonical placeId values.
            val places = discovery.getPlaces(
                pageSize = 20, contentTypeId = contentType, maxPages = 3, regionName = query.region,
            ).map { it.toDomain() }.associateBy { it.contentId }
            forecasts[key] = System.nanoTime() to places
            places
        } catch (error: Exception) {
            error.rethrowCancellation()
            // A failed forecast lookup must not block searching or change the server ranking.
            emptyMap()
        }
    }

    override suspend fun search(query: PlaceCandidateQuery): PlaceCandidatePage {
        val tourForecasts = discoverForecasts(query)
        val params = buildMap<String, String?> {
            put("query", query.query.trim().takeIf(String::isNotEmpty))
            put("region", query.region.takeIf(String::isNotBlank))
            put("category", query.category)
            put("sort", query.sort.name)
            put("limit", query.limit.toString())
            if (query.sort == PlaceSort.TRAVEL_TIME) {
                put("transportMode", query.transportMode.name)
                query.origin?.let { put("originLatitude", it.latitude.toString()); put("originLongitude", it.longitude.toString()) }
                query.next?.let { put("nextLatitude", it.latitude.toString()); put("nextLongitude", it.longitude.toString()) }
            } else {
                put("cursor", query.cursor)
            }
        }
        // Use the authenticated transport: travel-time calculation requires the existing session.
        diagnostic("request sort=${query.sort} mode=${query.transportMode} originPresent=${query.origin != null} nextPresent=${query.next != null}")
        val response = http.getObject("/api/v1/places", params)
        val ranking = response.optJSONObject("ranking")
        diagnostic("response rankingPresent=${ranking != null} sort=${ranking?.optString("sort")} items=${response.optJSONArray("items")?.length()}")
        if (query.sort == PlaceSort.TRAVEL_TIME && query.origin != null && ranking?.optString("sort").isNullOrBlank()) {
            throw java.io.IOException("서버에서 이동시간 정렬 정보를 받지 못했어요. 잠시 후 다시 시도해 주세요.")
        }
        val sort = ranking?.optString("sort")?.takeIf(String::isNotBlank)?.let(PlaceSort::valueOf)
            ?: PlaceSort.RECENT
        val items = response.getJSONArray("items")
        return PlaceCandidatePage(
            items = List(items.length()) { index ->
                val candidate = candidate(items.getJSONObject(index))
                val forecast = tourForecasts[candidate.place.contentId]
                if (forecast == null) candidate else candidate.copy(place = candidate.place.copy(
                    crowdLevel = forecast.crowdLevel,
                    concentrationScore = forecast.concentrationScore,
                    crowdSource = forecast.crowdSource,
                    crowdEstimated = forecast.crowdEstimated,
                    crowdProviderDataAvailable = forecast.crowdProviderDataAvailable,
                    crowdConfidence = forecast.crowdConfidence,
                    crowdMessage = forecast.crowdMessage,
                    weatherInfo = candidate.place.weatherInfo ?: forecast.weatherInfo,
                    regionCode = forecast.regionCode.ifBlank { candidate.place.regionCode },
                    districtCode = forecast.districtCode.ifBlank { candidate.place.districtCode },
                ))
            },
            sort = sort,
            evaluatedCandidates = ranking?.optInt("evaluatedCandidates") ?: 0,
            limited = ranking?.optBoolean("limited") ?: false,
            nextCursor = response.optionalString("nextCursor").takeIf { sort == PlaceSort.RECENT },
            hasNext = sort == PlaceSort.RECENT && response.optBoolean("hasNext"),
        )
    }

    private fun candidate(item: JSONObject): PlaceCandidate {
        val travelTime = item.optJSONObject("travelTime")?.let {
            PlaceTravelTime(
                transportMode = RouteTransportMode.valueOf(it.getString("transportMode")),
                durationMinutes = it.getInt("durationMinutes"),
                onwardDurationMinutes = if (it.isNull("onwardDurationMinutes")) null else it.getInt("onwardDurationMinutes"),
                additionalDurationMinutes = if (it.isNull("additionalDurationMinutes")) null else it.getInt("additionalDurationMinutes"),
                configuredProvider = it.getString("configuredProvider"),
                fallback = it.getBoolean("fallback"),
            )
        }
        return PlaceCandidate(
            place = TourPlace(
                contentId = item.getLong("id").toString(), title = item.getString("name"),
                address = item.optionalString("address").orEmpty(), addressDetail = item.optionalString("roadAddress").orEmpty(),
                imageUrl = item.optionalString("imageUrl").orEmpty(),
                latitude = item.optionalString("latitude")?.toDoubleOrNull(),
                longitude = item.optionalString("longitude")?.toDoubleOrNull(),
                // Canonical catalogue rows may have NORMAL as a placeholder with no data.
                crowdLevel = item.optionalString("crowdLevel").orEmpty().takeIf {
                    item.optBoolean("crowdEstimated") ||
                        item.optBoolean("crowdProviderDataAvailable", item.optBoolean("crowdDataAvailable")) ||
                        item.optionalString("crowdSource") != null
                }.orEmpty(),
                regionCode = item.optionalString("lDongRegnCd") ?: item.optionalString("areaCode").orEmpty(),
                districtCode = item.optionalString("lDongSignguCd") ?: item.optionalString("districtCode").orEmpty(),
                concentrationScore = item.optionalString("concentrationScore")?.toIntOrNull(),
                crowdSource = item.optionalString("crowdSource").orEmpty(),
                crowdEstimated = item.optBoolean("crowdEstimated"),
                crowdProviderDataAvailable = item.optBoolean("crowdProviderDataAvailable", item.optBoolean("crowdDataAvailable")),
                crowdConfidence = item.optionalString("crowdConfidence").orEmpty(),
                crowdMessage = item.optionalString("crowdMessage").orEmpty(),
                weatherInfo = item.optJSONObject("weather")?.toWeatherResult(),
            ),
            categoryCode = item.optionalString("categoryCode").orEmpty(),
            travelTime = travelTime,
        )
    }
}

private fun JSONObject.optionalString(name: String): String? = if (isNull(name)) null else optString(name).takeIf(String::isNotBlank)

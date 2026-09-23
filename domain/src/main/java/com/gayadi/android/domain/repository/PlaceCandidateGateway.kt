package com.gayadi.android.domain.repository

import com.gayadi.android.domain.model.RouteTransportMode
import com.gayadi.android.domain.model.TourPlace

enum class PlaceSort { RECENT, TRAVEL_TIME }

data class PlaceCoordinate(val latitude: Double, val longitude: Double) {
    init {
        require(latitude.isFinite() && latitude in -90.0..90.0)
        require(longitude.isFinite() && longitude in -180.0..180.0)
    }
}

data class PlaceCandidateQuery(
    val query: String = "",
    val region: String = "",
    val category: String? = null,
    val sort: PlaceSort = PlaceSort.RECENT,
    val transportMode: RouteTransportMode = RouteTransportMode.PUBLIC_TRANSIT,
    val origin: PlaceCoordinate? = null,
    val next: PlaceCoordinate? = null,
    val limit: Int = 20,
    val cursor: String? = null,
) {
    init {
        require(limit in 1..50)
        require(next == null || origin != null)
        require(sort != PlaceSort.TRAVEL_TIME || cursor == null)
    }
}

data class PlaceTravelTime(
    val transportMode: RouteTransportMode,
    val durationMinutes: Int,
    val onwardDurationMinutes: Int? = null,
    val additionalDurationMinutes: Int? = null,
    val configuredProvider: String,
    val fallback: Boolean,
) {
    val isEstimate: Boolean get() = transportMode == RouteTransportMode.WALK ||
        transportMode == RouteTransportMode.BICYCLE || fallback || configuredProvider == "LOCAL_ESTIMATE"
}

data class PlaceCandidate(val place: TourPlace, val categoryCode: String, val travelTime: PlaceTravelTime?)
data class PlaceCandidatePage(
    val items: List<PlaceCandidate>,
    val sort: PlaceSort,
    val evaluatedCandidates: Int,
    val limited: Boolean,
    val nextCursor: String?,
    val hasNext: Boolean,
)

fun interface PlaceCandidateGateway {
    suspend fun search(query: PlaceCandidateQuery): PlaceCandidatePage
}

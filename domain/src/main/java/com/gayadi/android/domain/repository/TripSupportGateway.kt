package com.gayadi.android.domain.repository

import com.gayadi.android.domain.model.TourPlace

/** Less frequently used trip, context, and agent operations exposed by the backend. */
interface TripSupportGateway {
    suspend fun getPlace(placeId: String): TourPlace
    suspend fun generatePlan(tripId: String): TravelPlan
    suspend fun getPlan(tripId: String): TravelPlan
    suspend fun getDashboard(tripId: String): TripDashboard

    suspend fun recommendRoutes(tripId: String, type: String, userId: String? = null): List<RecommendedRoute>
    suspend fun listSelectedRoutes(tripId: String): List<RecommendedRoute>
    suspend fun selectRoute(
        tripId: String,
        type: String,
        optionId: String,
        userId: String? = null,
    ): RecommendedRoute
    suspend fun clearSelectedRoute(tripId: String, type: String)

    suspend fun submitTripSurvey(tripId: String, answers: List<SurveyAnswer>): TripSurveyResult
    suspend fun getTripPersonality(tripId: String): TripPersonality

    suspend fun observeEvent(tripId: String, command: EventObservationCommand): EventResult
    suspend fun listChangeProposals(tripId: String): List<ChangeProposal>
    suspend fun decideChangeProposal(
        tripId: String,
        proposalId: String,
        approve: Boolean,
        selectedOptionKey: String?,
        baseRevisionNo: Int,
    ): ChangeProposal

    suspend fun recommendPlaces(command: PlaceRecommendationCommand): PlaceRecommendations
    suspend fun respondToSituation(tripId: String, command: SituationCommand): SituationResult

    suspend fun getWeatherNow(latitude: Double, longitude: Double): WeatherResult
    suspend fun getUltraForecast(latitude: Double, longitude: Double): WeatherResult
    suspend fun getForecast(latitude: Double, longitude: Double): WeatherResult
    suspend fun getForecastVersion(fileType: String, baseDateTime: String): ForecastVersion
    suspend fun getCongestion(command: CongestionCommand): CongestionResult

    suspend fun getTourAreas(regionName: String, pageSize: Int = 10): TourPage
    suspend fun getNearbyTourPlaces(
        longitude: Double,
        latitude: Double,
        radiusMeters: Int,
        pageSize: Int = 10,
    ): TourPage
    suspend fun searchTourPlaces(keyword: String, pageSize: Int = 10): TourPage
    suspend fun getTourFestivals(startDate: String, endDate: String? = null, pageSize: Int = 10): TourPage
    suspend fun getTourStays(pageSize: Int = 10): TourPage
}

data class TravelPlan(val id: String, val tripId: String, val dayCount: Int, val itemCount: Int)

data class TripDashboard(
    val tripId: String,
    val participantCount: Int,
    val scheduleCount: Int,
    val visitedCount: Int,
    val pendingProposalCount: Int,
)

data class RecommendedRoute(
    val id: String,
    val optionId: String,
    val name: String,
    val durationMinutes: Int,
    val transferCount: Int,
    val summary: String,
    val stops: List<String>,
    val selected: Boolean,
)

data class SurveyAnswer(val questionId: String, val optionId: String)
data class TripSurveyResult(val attemptId: String, val tripId: String, val resultCode: String)
data class TripPersonality(val dominantProfile: String, val responseCount: Long, val distribution: Map<String, Long>)

data class EventObservationCommand(
    val placeId: String? = null,
    val eventType: String,
    val source: String,
    val severity: String,
    val values: Map<String, Any>,
)

data class EventResult(
    val eventId: String?,
    val proposal: ChangeProposal?,
    val impact: Boolean,
    val message: String,
)

data class ChangeProposalOption(val key: String, val placeId: String, val placeName: String)
data class ChangeProposal(
    val id: String,
    val status: String,
    val reason: String,
    val baseRevisionNo: Int,
    val options: List<ChangeProposalOption>,
)

data class PlaceRecommendationCommand(
    val destination: String,
    val profile: String,
    val latitude: Double,
    val longitude: Double,
    val keywords: List<String> = emptyList(),
    val limit: Int = 5,
    val externalProcessingConsent: Boolean,
)

data class RecommendedPlace(
    val placeId: String,
    val name: String,
    val category: String,
    val score: Double,
    val reason: String,
)

data class PlaceRecommendations(val places: List<RecommendedPlace>, val reasoning: String)

data class SituationCommand(
    val latitude: Double,
    val longitude: Double,
    val keywords: List<String> = emptyList(),
    val weatherCondition: String = "",
    val congestionLevel: String = "",
    val transitMissed: Boolean = false,
    val externalProcessingConsent: Boolean,
)

data class SituationResult(
    val summary: String,
    val routeRecalculationRequired: Boolean,
    val nextAction: String,
    val recommendations: PlaceRecommendations,
    val proposal: ChangeProposal?,
)

data class WeatherResult(
    val baseDate: String,
    val baseTime: String,
    val temperature: String?,
    val forecastSlotCount: Int,
)

data class ForecastVersion(val itemCount: Int)
data class CongestionCommand(
    val areaCode: String,
    val districtCode: String,
    val areaName: String = "",
    val placeName: String = "",
    val targetAt: String = "",
)
data class CongestionResult(
    val level: String,
    val score: Int,
    val estimated: Boolean,
    val providerDataAvailable: Boolean,
)
data class TourPage(val places: List<TourPlace>, val totalCount: Int, val nextCursor: String?)

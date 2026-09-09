package com.gayadi.android.domain.repository

enum class PlanningRouteType { DEPARTURE, ITINERARY, HOME }

data class RecommendedRoute(
    val id: String, val optionId: String, val name: String, val type: PlanningRouteType,
    val durationMinutes: Int?, val distanceMeters: Int?, val fare: Int?,
    val summary: String, val stops: List<String>, val isEstimate: Boolean,
    val userId: String? = null,
)
data class GeneratedPlanItem(val id: String, val title: String, val start: String, val end: String, val address: String)
data class GeneratedPlanDay(val date: String, val title: String, val items: List<GeneratedPlanItem>)
data class GeneratedPlan(val days: List<GeneratedPlanDay>)

interface PlanningGateway {
    suspend fun selectedRoutes(tripId: String): List<RecommendedRoute>
    suspend fun recommend(tripId: String, type: PlanningRouteType): List<RecommendedRoute>
    suspend fun select(tripId: String, route: RecommendedRoute): RecommendedRoute
    suspend fun clearSelection(tripId: String, type: PlanningRouteType)
    suspend fun getPlan(tripId: String): GeneratedPlan?
    suspend fun generatePlan(tripId: String): GeneratedPlan
}

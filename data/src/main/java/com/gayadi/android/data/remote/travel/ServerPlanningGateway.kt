package com.gayadi.android.data.remote.travel

import com.gayadi.android.data.datasource.GayadiApiClient
import com.gayadi.android.data.datasource.GayadiApiException
import com.gayadi.android.domain.repository.*
import org.json.JSONArray
import org.json.JSONObject

class ServerPlanningGateway(private val api: GayadiApiClient, private val auth: AuthRepository) : PlanningGateway {
    private fun path(tripId: String) = "/api/v1/trips/${id(tripId)}"
    private fun id(value: String) = requireNotNull(value.toLongOrNull()?.takeIf { it > 0 }) { "유효한 서버 ID가 필요해요" }
    private fun userId() = requireNotNull(auth.currentSession()).user.id
    override suspend fun selectedRoutes(tripId: String) = array(api.request("GET", "${path(tripId)}/route-selections"))
        .map(::route).filter { it.type == PlanningRouteType.ITINERARY || it.userId == userId().toString() }
    override suspend fun recommend(tripId: String, type: PlanningRouteType): List<RecommendedRoute> {
        val body = JSONObject().put("type", type.name)
        if (type != PlanningRouteType.ITINERARY) body.put("userId", userId())
        val result = JSONObject(api.request("POST", "${path(tripId)}/route-recommendations", body.toString()))
        return result.optJSONArray("options")?.let { values ->
            (0 until values.length()).map { route(values.getJSONObject(it)) }
        }?.takeIf { it.isNotEmpty() } ?: listOf(route(result))
    }
    override suspend fun select(tripId: String, route: RecommendedRoute): RecommendedRoute {
        val body = JSONObject().put("routeId", id(route.id)).put("optionId", route.optionId)
        if (route.type != PlanningRouteType.ITINERARY) body.put("userId", userId())
        return route(JSONObject(api.request("PUT", "${path(tripId)}/route-selections/${route.type.name}", body.toString())))
    }
    override suspend fun clearSelection(tripId: String, type: PlanningRouteType) {
        val query = if (type == PlanningRouteType.ITINERARY) "" else "?userId=${userId()}"
        api.request("DELETE", "${path(tripId)}/route-selections/${type.name}$query")
    }
    override suspend fun getPlan(tripId: String): GeneratedPlan? = try {
        plan(JSONObject(api.request("GET", "${path(tripId)}/plans")))
    } catch (error: GayadiApiException) {
        if (error.statusCode == 404) null else throw error
    }
    override suspend fun generatePlan(tripId: String) = plan(JSONObject(api.request("POST", "${path(tripId)}/plans")))
    internal companion object {
        fun route(json: JSONObject): RecommendedRoute = RecommendedRoute(
            id=json.getLong("id").toString(), optionId=json.getString("optionId"), name=json.getString("name"),
            type=when(json.getString("type")) { "IN_TRIP" -> PlanningRouteType.ITINERARY; "RETURN" -> PlanningRouteType.HOME; else -> PlanningRouteType.valueOf(json.getString("type")) },
            durationMinutes=json.nullableInt("durationMinutes"), distanceMeters=json.nullableInt("distanceMeters"), fare=json.nullableInt("fare"),
            summary=json.optString("summary"), stops=objects(json.optJSONArray("stops") ?: JSONArray()).map { it.optString("label").takeIf(String::isNotBlank) ?: it.optString("name") }.filter(String::isNotBlank),
            isEstimate=json.optBoolean("fallback") || json.optString("provider")=="LOCAL_ESTIMATE", userId=if(json.isNull("userId")) null else json.getLong("userId").toString(),
        )
        fun plan(json: JSONObject): GeneratedPlan {
            val days = json.optJSONArray("days")?.let(::objects)?.takeIf { it.isNotEmpty() } ?: listOf(json)
            return GeneratedPlan(days.map { day -> GeneratedPlanDay(day.getString("plan_date"), day.getString("title"),
                objects(day.getJSONArray("items")).map { item -> GeneratedPlanItem(item.getLong("id").toString(), item.getString("title"),
                    item.optString("planned_start"), item.optString("planned_end"), item.optString("address")) }) })
        }
        private fun JSONObject.nullableInt(key: String): Int? = if (isNull(key)) null else getInt(key)
        private fun array(value: String) = objects(JSONArray(value))
        private fun objects(value: JSONArray) = (0 until value.length()).map(value::getJSONObject)
    }
}

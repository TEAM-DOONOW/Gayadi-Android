package com.gayadi.android.data.remote.travel

import com.gayadi.android.domain.model.RouteTransportMode

import com.gayadi.android.data.datasource.GayadiApiClient
import com.gayadi.android.domain.model.TourPlace
import com.gayadi.android.domain.repository.ChangeProposal
import com.gayadi.android.domain.repository.ChangeProposalOption
import com.gayadi.android.domain.repository.CongestionCommand
import com.gayadi.android.domain.repository.CongestionResult
import com.gayadi.android.domain.repository.EventObservationCommand
import com.gayadi.android.domain.repository.EventResult
import com.gayadi.android.domain.repository.ForecastVersion
import com.gayadi.android.domain.repository.PlaceRecommendationCommand
import com.gayadi.android.domain.repository.PlaceRecommendations
import com.gayadi.android.domain.repository.RecommendedPlace
import com.gayadi.android.domain.repository.RecommendedRoute
import com.gayadi.android.domain.repository.SituationCommand
import com.gayadi.android.domain.repository.SituationResult
import com.gayadi.android.domain.repository.SurveyAnswer
import com.gayadi.android.domain.repository.TourPage
import com.gayadi.android.domain.repository.TravelPlan
import com.gayadi.android.domain.repository.TripDashboard
import com.gayadi.android.domain.repository.TripPersonality
import com.gayadi.android.domain.repository.TripSupportGateway
import com.gayadi.android.domain.repository.TripSurveyResult
import com.gayadi.android.domain.repository.WeatherResult
import org.json.JSONArray
import org.json.JSONObject

/** Typed adapter for the remaining user-facing trip, public-data, and agent APIs. */
class ServerTripSupportGateway(private val http: TravelJsonTransport) : TripSupportGateway {
    constructor(client: GayadiApiClient) : this(GayadiTravelJsonTransport(client))

    override suspend fun getPlace(placeId: String): TourPlace =
        place(http.getPublicObject("/api/v1/places/${placeId.serverId("placeId")}"))

    override suspend fun generatePlan(tripId: String): TravelPlan =
        plan(http.postObject("${tripPath(tripId)}/plans", JSONObject()))

    override suspend fun getPlan(tripId: String): TravelPlan =
        plan(http.getObject("${tripPath(tripId)}/plans"))

    override suspend fun getDashboard(tripId: String): TripDashboard {
        val value = http.getObject("${tripPath(tripId)}/dashboard")
        val progress = value.getJSONObject("progress")
        return TripDashboard(
            tripId = value.getJSONObject("trip").getLong("id").toString(),
            participantCount = value.getInt("participantCount"),
            scheduleCount = progress.getInt("scheduleCount"),
            visitedCount = progress.getInt("visitedCount"),
            pendingProposalCount = value.getJSONArray("pendingChangeProposals").length(),
        )
    }

    override suspend fun recommendRoutes(
        tripId: String,
        type: String,
        userId: String?,
        transportMode: RouteTransportMode,
    ): List<RecommendedRoute> {
        val body = JSONObject().put("type", type).put("transportMode", transportMode.name)
        userId?.let { body.put("userId", it.serverId("userId")) }
        val response = http.postObject(
            "${tripPath(tripId)}/route-recommendations",
            body,
        )
        val options = response.optJSONArray("options") ?: JSONArray().put(response)
        return options.objects().map(::route)
    }

    override suspend fun listSelectedRoutes(tripId: String): List<RecommendedRoute> =
        http.getArray("${tripPath(tripId)}/route-selections").objects().map(::route)

    override suspend fun selectRoute(
        tripId: String,
        type: String,
        optionId: String,
        userId: String?,
    ): RecommendedRoute = route(
        http.putObject(
            "${tripPath(tripId)}/route-selections/${type.pathSegment("type")}",
            JSONObject().put("optionId", optionId).apply {
                userId?.let { put("userId", it.serverId("userId")) }
            },
        ),
    )

    override suspend fun clearSelectedRoute(tripId: String, type: String) =
        http.delete("${tripPath(tripId)}/route-selections/${type.pathSegment("type")}")

    override suspend fun submitTripSurvey(
        tripId: String,
        answers: List<SurveyAnswer>,
    ): TripSurveyResult {
        require(answers.isNotEmpty()) { "answers must not be empty" }
        val body = JSONObject().put("answers", JSONArray(answers.map { answer ->
            JSONObject().put("questionId", answer.questionId).put("optionId", answer.optionId)
        }))
        val value = http.postObject("${tripPath(tripId)}/survey-responses", body)
        return TripSurveyResult(
            attemptId = value.getLong("attemptId").toString(),
            tripId = value.getLong("tripId").toString(),
            resultCode = value.getString("resultCode"),
        )
    }

    override suspend fun getTripPersonality(tripId: String): TripPersonality {
        val value = http.getObject("${tripPath(tripId)}/personality-profile")
        val distribution = value.getJSONObject("distribution")
        return TripPersonality(
            dominantProfile = value.getString("dominantProfile"),
            responseCount = value.getLong("responseCount"),
            distribution = distribution.keys().asSequence().associateWith(distribution::getLong),
        )
    }

    override suspend fun observeEvent(
        tripId: String,
        command: EventObservationCommand,
    ): EventResult {
        val body = JSONObject()
            .putNullableLong("placeId", command.placeId)
            .put("eventType", command.eventType)
            .put("source", command.source)
            .put("severity", command.severity)
            .put("values", JSONObject(command.values))
        val value = http.postObject("${tripPath(tripId)}/event-observations", body)
        return if (value.has("impact")) {
            EventResult(
                eventId = value.getLong("eventId").toString(),
                proposal = null,
                impact = value.getBoolean("impact"),
                message = value.optString("message"),
            )
        } else {
            val proposal = proposal(value)
            EventResult(proposal = proposal, eventId = value.optLongOrNull("eventId")?.toString(), impact = true, message = proposal.reason)
        }
    }

    override suspend fun listChangeProposals(tripId: String): List<ChangeProposal> =
        http.getArray("${tripPath(tripId)}/change-proposals").objects().map(::proposal)

    override suspend fun decideChangeProposal(
        tripId: String,
        proposalId: String,
        approve: Boolean,
        selectedOptionKey: String?,
        baseRevisionNo: Int,
    ): ChangeProposal = proposal(
        http.patchObject(
            "${tripPath(tripId)}/change-proposals/${proposalId.serverId("proposalId")}",
            JSONObject()
                .put("approve", approve)
                .put("selectedOptionKey", selectedOptionKey ?: JSONObject.NULL)
                .put("baseRevisionNo", baseRevisionNo),
        ),
    )

    override suspend fun recommendPlaces(command: PlaceRecommendationCommand): PlaceRecommendations {
        require(command.externalProcessingConsent) { "external processing consent is required" }
        return recommendations(http.postObject("/api/v1/recommendations/places", command.toJson()))
    }

    override suspend fun respondToSituation(
        tripId: String,
        command: SituationCommand,
    ): SituationResult {
        require(command.externalProcessingConsent) { "external processing consent is required" }
        val response = http.postObject("${tripPath(tripId)}/situation-responses", command.toJson())
        return SituationResult(
            summary = response.getString("situationSummary"),
            routeRecalculationRequired = response.getBoolean("routeRecalculationRequired"),
            nextAction = response.getString("nextAction"),
            recommendations = recommendations(response.getJSONObject("placeRecommendations")),
            proposal = response.optJSONObject("changeProposal")
                ?.takeIf { it.has("id") && !it.isNull("id") }
                ?.let(::proposal),
        )
    }

    override suspend fun getWeatherNow(latitude: Double, longitude: Double): WeatherResult =
        weather(http.getObject("/api/v1/weather/nowcasts", coordinates(latitude, longitude)))

    override suspend fun getUltraForecast(latitude: Double, longitude: Double): WeatherResult =
        weather(http.getObject("/api/v1/weather/ultra-forecast", coordinates(latitude, longitude)))

    override suspend fun getForecast(latitude: Double, longitude: Double): WeatherResult =
        weather(http.getObject("/api/v1/weather/forecast", coordinates(latitude, longitude)))

    override suspend fun getForecastVersion(fileType: String, baseDateTime: String): ForecastVersion {
        val response = http.getObject(
            "/api/v1/weather/version",
            mapOf("ftype" to fileType, "baseDateTime" to baseDateTime),
        )
        return ForecastVersion(response.getJSONArray("items").length())
    }

    override suspend fun getCongestion(command: CongestionCommand): CongestionResult {
        val response = http.getObject(
            "/api/v1/congestion/forecast",
            mapOf(
                "areaCode" to command.areaCode,
                "districtCode" to command.districtCode,
                "areaName" to command.areaName,
                "placeName" to command.placeName,
                "targetAt" to command.targetAt,
            ),
        )
        return CongestionResult(
            level = response.getString("level"),
            score = response.getInt("concentrationScore"),
            estimated = response.getBoolean("estimated"),
            providerDataAvailable = response.getBoolean("providerDataAvailable"),
        )
    }

    override suspend fun getTourAreas(regionName: String, pageSize: Int): TourPage = tourPage(
        http.getPublicObject(
            "/api/v1/tour/areas",
            mapOf("regionName" to regionName, "pageSize" to pageSize.toString()),
        ),
    )

    override suspend fun getNearbyTourPlaces(
        longitude: Double,
        latitude: Double,
        radiusMeters: Int,
        pageSize: Int,
    ): TourPage = tourPage(http.getObject(
        "/api/v1/tour/locations",
        mapOf(
            "mapX" to longitude.toString(), "mapY" to latitude.toString(),
            "radius" to radiusMeters.toString(), "pageSize" to pageSize.toString(),
        ),
    ))

    override suspend fun searchTourPlaces(keyword: String, pageSize: Int): TourPage = tourPage(
        http.getObject(
            "/api/v1/tour/keywords",
            mapOf("keyword" to keyword, "pageSize" to pageSize.toString()),
        ),
    )

    override suspend fun getTourFestivals(
        startDate: String,
        endDate: String?,
        pageSize: Int,
    ): TourPage = tourPage(http.getObject(
        "/api/v1/tour/festivals",
        mapOf(
            "eventStartDate" to startDate,
            "eventEndDate" to endDate,
            "pageSize" to pageSize.toString(),
        ),
    ))

    override suspend fun getTourStays(pageSize: Int): TourPage = tourPage(
        http.getObject("/api/v1/tour/stays", mapOf("pageSize" to pageSize.toString())),
    )

    private fun plan(value: JSONObject): TravelPlan {
        val days = value.optJSONArray("days") ?: JSONArray()
        val itemCount = days.objects().sumOf { it.optJSONArray("items")?.length() ?: 0 }
        return TravelPlan(
            id = value.getLong("id").toString(),
            tripId = value.getLong("trip_id").toString(),
            dayCount = days.length(),
            itemCount = itemCount,
        )
    }

    private fun route(value: JSONObject): RecommendedRoute {
        val stopValues = value.optJSONArray("stops")
            ?: value.optJSONObject("routeData")?.optJSONArray("stops")
            ?: JSONArray()
        return RecommendedRoute(
            id = value.getLong("id").toString(),
            optionId = value.optString("optionId"),
            name = value.optString("name"),
            durationMinutes = value.optInt("durationMinutes"),
            transferCount = value.optInt("transferCount"),
            summary = value.optString("summary"),
            stops = stopValues.objects().map { stop ->
                stop.optString("label").ifBlank { stop.optString("name") }
            }.filter(String::isNotBlank),
            selected = value.optString("status") == "SELECTED",
        )
    }

    private fun proposal(value: JSONObject): ChangeProposal = ChangeProposal(
        id = value.getLong("id").toString(),
        status = value.optString("status"),
        reason = value.optString("reason"),
        baseRevisionNo = value.optInt("baseRevisionNo"),
        options = (value.optJSONArray("options") ?: JSONArray()).objects().map { option ->
            ChangeProposalOption(
                key = option.getString("key"),
                placeId = option.getLong("placeId").toString(),
                placeName = option.getString("placeName"),
            )
        },
    )

    private fun recommendations(value: JSONObject): PlaceRecommendations = PlaceRecommendations(
        places = value.getJSONArray("recommendations").objects().map { item ->
            RecommendedPlace(
                placeId = item.optString("placeId"),
                name = item.optString("name"),
                category = item.optString("category"),
                score = item.optDouble("score"),
                reason = item.optString("reason"),
            )
        },
        reasoning = value.optString("reasoning"),
    )

    private fun weather(value: JSONObject) = WeatherResult(
        baseDate = value.getString("baseDate"),
        baseTime = value.getString("baseTime"),
        temperature = value.optNullableString("temperature"),
        forecastSlotCount = value.optJSONArray("forecast")?.length() ?: 0,
    )

    private fun tourPage(value: JSONObject): TourPage {
        val items = value.getJSONArray("items")
        return TourPage(
            places = items.objects().map(::tourPlace),
            totalCount = value.optInt("totalCount", items.length()),
            nextCursor = value.optNullableString("nextCursor"),
        )
    }

    private fun place(item: JSONObject) = TourPlace(
        contentId = item.getLong("id").toString(),
        title = item.getString("name"),
        address = item.optString("address"),
        addressDetail = item.optString("roadAddress"),
        imageUrl = item.optString("imageUrl"),
        longitude = item.optDoubleOrNull("longitude"),
        latitude = item.optDoubleOrNull("latitude"),
    )

    private fun tourPlace(item: JSONObject) = TourPlace(
        contentId = item.getString("contentId"),
        title = item.getString("title"),
        address = item.optString("address"),
        addressDetail = item.optString("addressDetail"),
        imageUrl = item.optString("firstImage"),
        longitude = item.optString("mapX").toDoubleOrNull(),
        latitude = item.optString("mapY").toDoubleOrNull(),
        contentTypeId = item.optString("contentTypeId"),
        lclsSystm1 = item.optString("lclsSystm1"),
        lclsSystm2 = item.optString("lclsSystm2"),
        lclsSystm3 = item.optString("lclsSystm3"),
        distanceMeters = item.optString("dist").toDoubleOrNull()?.toInt(),
    )

    private fun tripPath(tripId: String) = "/api/v1/trips/${tripId.serverId("tripId")}"

    private companion object {
        fun coordinates(latitude: Double, longitude: Double) =
            mapOf("lat" to latitude.toString(), "lon" to longitude.toString())
    }
}

private fun PlaceRecommendationCommand.toJson() = JSONObject()
    .put("destination", destination)
    .put("profile", profile)
    .put("latitude", latitude)
    .put("longitude", longitude)
    .put("keywords", JSONArray(keywords))
    .put("limit", limit)
    .put("externalProcessingConsent", externalProcessingConsent)

private fun SituationCommand.toJson() = JSONObject()
    .put("latitude", latitude)
    .put("longitude", longitude)
    .put("keywords", JSONArray(keywords))
    .put("situation", JSONObject()
        .put("weather", JSONObject().put("condition", weatherCondition))
        .put("congestion", JSONObject().put("level", congestionLevel))
        .put("transit", JSONObject().put("missed", transitMissed)))
    .put("externalProcessingConsent", externalProcessingConsent)

private fun JSONObject.putNullableLong(key: String, value: String?): JSONObject =
    put(key, value?.serverId(key) ?: JSONObject.NULL)

private fun String.serverId(field: String): Long =
    toLongOrNull()?.takeIf { it > 0 } ?: throw IllegalArgumentException("$field must be a positive server ID")

private fun String.pathSegment(field: String): String {
    require(matches(Regex("[A-Za-z][A-Za-z0-9_-]{0,39}"))) { "$field is invalid" }
    return this
}

private fun JSONObject.optLongOrNull(key: String): Long? =
    if (!has(key) || isNull(key)) null else getLong(key)

private fun JSONObject.optNullableString(key: String): String? =
    if (!has(key) || isNull(key)) null else getString(key)

private fun JSONObject.optDoubleOrNull(key: String): Double? =
    if (!has(key) || isNull(key)) null else getDouble(key)

private fun JSONArray.objects(): List<JSONObject> = List(length(), ::getJSONObject)

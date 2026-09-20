package com.gayadi.android.data.remote.agent

import com.gayadi.android.data.datasource.GayadiApiClient
import com.gayadi.android.domain.model.AgentChangeProposal
import com.gayadi.android.domain.model.AgentProposalOption
import com.gayadi.android.domain.model.AgentRecommendation
import com.gayadi.android.domain.model.AgentPlaceRecommendations
import com.gayadi.android.domain.model.AgentSituationResponse
import com.gayadi.android.domain.repository.AgentGateway
import java.time.OffsetDateTime
import java.time.ZoneId
import org.json.JSONArray
import org.json.JSONObject

class ServerAgentGateway(
    private val api: GayadiApiClient,
) : AgentGateway {
    override suspend fun recommendPlaces(
        destination: String,
        profile: String,
        latitude: Double,
        longitude: Double,
        keywords: List<String>,
        groupSize: Int,
    ): AgentPlaceRecommendations {
        val request = JSONObject()
            .put("destination", destination)
            .put("purpose", "PLACE_RECOMMENDATION")
            .put("profile", profile)
            .put("latitude", latitude)
            .put("longitude", longitude)
            .put("keywords", JSONArray(keywords.take(10)))
            .put("limit", 6)
            .put("groupSize", groupSize.coerceIn(1, 100))
            .put("targetAt", OffsetDateTime.now(ZoneId.of("Asia/Seoul")).toString())
            .put("situation", JSONObject())
            .put("externalProcessingConsent", true)
        return JSONObject(
            api.request("POST", "/api/v1/recommendations/places", request.toString()),
        ).toPlaceRecommendations()
    }

    override suspend fun analyzeSituation(
        tripId: String,
        latitude: Double,
        longitude: Double,
        keywords: List<String>,
    ): AgentSituationResponse {
        val request = JSONObject()
            .put("latitude", latitude)
            .put("longitude", longitude)
            .put("keywords", JSONArray(keywords.take(10)))
            .put("limit", 5)
            .put("targetAt", OffsetDateTime.now(ZoneId.of("Asia/Seoul")).toString())
            .put("situation", JSONObject())
            .put("externalProcessingConsent", true)
        return JSONObject(
            api.request("POST", "/api/v1/trips/${tripId.serverId()}/situation-responses", request.toString()),
        ).toSituationResponse()
    }

    override suspend fun listChangeProposals(tripId: String): List<AgentChangeProposal> {
        val items = JSONArray(
            api.request("GET", "/api/v1/trips/${tripId.serverId()}/change-proposals?limit=30&offset=0"),
        )
        return buildList(items.length()) {
            repeat(items.length()) { add(items.getJSONObject(it).toProposal()) }
        }
    }

    override suspend fun decideChangeProposal(
        tripId: String,
        proposalId: String,
        approve: Boolean,
        selectedOptionKey: String?,
        baseRevisionNo: Int,
    ): AgentChangeProposal {
        val request = JSONObject()
            .put("approve", approve)
            .put("baseRevisionNo", baseRevisionNo)
        if (approve) request.put("selectedOptionKey", requireNotNull(selectedOptionKey))
        return JSONObject(
            api.request(
                "PATCH",
                "/api/v1/trips/${tripId.serverId()}/change-proposals/${proposalId.serverId()}",
                request.toString(),
            ),
        ).toProposal()
    }

    private fun JSONObject.toSituationResponse(): AgentSituationResponse {
        val recommendationObject = optJSONObject("placeRecommendations") ?: JSONObject()
        val proposal = optJSONObject("changeProposal")
            ?.takeIf { it.has("id") && !it.isNull("id") }
            ?.toProposal()
        return AgentSituationResponse(
            situationSummary = optString("situationSummary"),
            routeRecalculationRequired = optBoolean("routeRecalculationRequired"),
            nextAction = optString("nextAction"),
            recommendations = recommendationObject.recommendations(),
            reasoning = recommendationObject.optString("reasoning"),
            changeProposal = proposal,
        )
    }

    private fun JSONObject.toPlaceRecommendations() = AgentPlaceRecommendations(
        recommendations = recommendations(),
        reasoning = optString("reasoning"),
    )

    private fun JSONObject.recommendations(): List<AgentRecommendation> {
        val items = optJSONArray("recommendations") ?: JSONArray()
        return buildList(items.length()) {
            repeat(items.length()) { index ->
                val item = items.getJSONObject(index)
                add(
                    AgentRecommendation(
                        placeId = item.optString("placeId"),
                        sourcePlaceId = item.optString("sourcePlaceId"),
                        name = item.optString("name"),
                        category = item.optString("category"),
                        score = item.optDouble("score", 0.0),
                        reason = item.optString("reason"),
                    ),
                )
            }
        }
    }

    private fun JSONObject.toProposal(): AgentChangeProposal {
        val optionArray = optJSONArray("options") ?: JSONArray()
        return AgentChangeProposal(
            id = optLong("id").takeIf { it > 0 }?.toString().orEmpty(),
            reason = optString("reason"),
            status = optString("status"),
            baseRevisionNo = if (has("baseRevisionNo") && !isNull("baseRevisionNo")) {
                optInt("baseRevisionNo")
            } else {
                null
            },
            options = buildList(optionArray.length()) {
                repeat(optionArray.length()) { index ->
                    val option = optionArray.getJSONObject(index)
                    add(
                        AgentProposalOption(
                            key = option.optString("key"),
                            placeId = option.optLong("placeId").takeIf { it > 0 }?.toString().orEmpty(),
                            placeName = option.optString("placeName"),
                            description = option.optString("description"),
                            requireIndoor = option.optBoolean("requireIndoor"),
                        ),
                    )
                }
            },
            selectedOptionKey = optString("selectedOptionKey").takeIf(String::isNotBlank),
        )
    }

    private fun String.serverId(): Long = toLongOrNull()
        ?.takeIf { it > 0 }
        ?: throw IllegalArgumentException("서버 식별자가 올바르지 않습니다.")
}

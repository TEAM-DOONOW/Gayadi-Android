package com.gayadi.android.domain.repository

import com.gayadi.android.domain.model.AgentChangeProposal
import com.gayadi.android.domain.model.AgentPlaceRecommendations
import com.gayadi.android.domain.model.AgentSituationResponse

interface AgentGateway {
    suspend fun recommendPlaces(
        destination: String,
        profile: String,
        latitude: Double,
        longitude: Double,
        keywords: List<String>,
        groupSize: Int,
    ): AgentPlaceRecommendations

    suspend fun analyzeSituation(
        tripId: String,
        latitude: Double,
        longitude: Double,
        keywords: List<String> = emptyList(),
    ): AgentSituationResponse

    suspend fun listChangeProposals(tripId: String): List<AgentChangeProposal>

    suspend fun decideChangeProposal(
        tripId: String,
        proposalId: String,
        approve: Boolean,
        selectedOptionKey: String?,
        baseRevisionNo: Int,
    ): AgentChangeProposal
}

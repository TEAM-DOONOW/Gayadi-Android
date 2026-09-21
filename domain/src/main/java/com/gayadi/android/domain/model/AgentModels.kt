package com.gayadi.android.domain.model

data class AgentRecommendation(
    val placeId: String,
    val sourcePlaceId: String,
    val name: String,
    val category: String,
    val score: Double,
    val reason: String,
)

data class AgentPlaceRecommendations(
    val recommendations: List<AgentRecommendation>,
    val reasoning: String,
)

data class AgentProposalOption(
    val key: String,
    val placeId: String,
    val placeName: String,
    val description: String,
    val requireIndoor: Boolean,
)

data class AgentChangeProposal(
    val id: String,
    val reason: String,
    val status: String,
    val baseRevisionNo: Int?,
    val options: List<AgentProposalOption>,
    val selectedOptionKey: String? = null,
)

data class AgentSituationResponse(
    val situationSummary: String,
    val routeRecalculationRequired: Boolean,
    val nextAction: String,
    val recommendations: List<AgentRecommendation>,
    val reasoning: String,
    val changeProposal: AgentChangeProposal? = null,
)

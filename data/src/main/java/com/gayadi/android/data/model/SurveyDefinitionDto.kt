package com.gayadi.android.data.model

/** Raw survey aggregate returned by Firestore or a local test source. */
data class SurveyDefinitionDto(
    val id: String,
    val title: String,
    val resultCodeOrder: List<String>,
    val questions: List<SurveyQuestionDto>,
    val results: List<SurveyResultDto>,
)

/** Raw travel-style result returned by a survey data source. */
data class SurveyResultDto(
    val code: String,
    val emoji: String,
    val name: String,
    val summary: String,
    val hashtags: List<String>,
    val strengths: List<String>,
    val weaknesses: List<String>,
    val characterKey: String?,
    val compatibleTypes: List<CompatibleTravelTypeDto> = emptyList(),
    val travelRole: TravelRoleDto? = null,
)

data class CompatibleTravelTypeDto(
    val code: String,
    val emoji: String,
    val name: String,
)

data class TravelRoleDto(
    val icon: String,
    val title: String,
    val description: String,
)

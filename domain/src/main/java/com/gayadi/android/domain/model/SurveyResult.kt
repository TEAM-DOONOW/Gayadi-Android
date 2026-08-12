package com.gayadi.android.domain.model

/** One of the eight travel-style results stored in Firestore. */
data class SurveyResult(
    val code: String,
    val emoji: String,
    val name: String,
    val summary: String,
    val hashtags: List<String>,
    val strengths: List<String>,
    val weaknesses: List<String>,
    val characterKey: String?,
    val compatibleTypes: List<CompatibleTravelType> = emptyList(),
    val travelRole: TravelRole? = null,
)

/** Another result type that tends to travel well with this result. */
data class CompatibleTravelType(
    val code: String,
    val emoji: String,
    val name: String,
)

/** A concise description of the role this type naturally takes during a trip. */
data class TravelRole(
    val icon: String,
    val title: String,
    val description: String,
)

package com.gayadi.android.domain.model

/** One of the eight travel-style results stored in Firestore. */
data class SurveyResult(
    val code: String,
    val emoji: String,
    val name: String,
    val summary: String,
    val traits: String?,
    val compatibleCode: String?,
    val oppositeCode: String?,
    val characterKey: String?,
)

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
)

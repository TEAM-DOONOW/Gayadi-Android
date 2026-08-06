package com.gayadi.android.domain.model

/** Domain model for one travel-style survey question. */
data class SurveyQuestion(
    val id: String,
    val order: Int,
    val dimension: String,
    val title: String,
    val options: List<SurveyOption>,
)

/** Domain model for one answer option and its scoring code. */
data class SurveyOption(
    val id: String,
    val text: String,
    val code: String,
)

package com.gayadi.android.data.model

/** Data-source representation of a survey question. */
data class SurveyQuestionDto(
    val id: String,
    val order: Int,
    val dimension: String,
    val title: String,
    val options: List<SurveyOptionDto>,
)

/** Raw answer option returned by a survey data source. */
data class SurveyOptionDto(
    val id: String,
    val text: String,
    val code: String,
)

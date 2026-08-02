package com.gayadi.android.data.model

/** Data-source representation of a survey question. */
data class SurveyQuestionDto(
    val id: Int,
    val title: String,
    val options: List<String>,
)

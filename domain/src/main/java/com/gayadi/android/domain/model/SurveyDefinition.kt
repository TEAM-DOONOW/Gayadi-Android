package com.gayadi.android.domain.model

/** Complete travel-style survey definition used by the presentation layer. */
data class SurveyDefinition(
    val id: String,
    val title: String,
    val resultCodeOrder: List<String>,
    val questions: List<SurveyQuestion>,
    val results: Map<String, SurveyResult>,
)

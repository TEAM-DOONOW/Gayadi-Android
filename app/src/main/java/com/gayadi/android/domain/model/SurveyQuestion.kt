package com.gayadi.android.domain.model

/** Domain model for one travel-style survey question. */
data class SurveyQuestion(
    val id: Int,
    val title: String,
    val options: List<String>,
)

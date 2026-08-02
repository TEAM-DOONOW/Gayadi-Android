package com.gayadi.android.domain.model

data class SurveyQuestion(
    val id: Int,
    val title: String,
    val options: List<String>,
)

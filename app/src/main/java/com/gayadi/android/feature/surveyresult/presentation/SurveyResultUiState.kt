package com.gayadi.android.feature.surveyresult.presentation

import com.gayadi.android.domain.model.SurveyResult

/** Immutable state for the Firestore-backed survey result screen. */
data class SurveyResultUiState(
    val isLoading: Boolean = true,
    val result: SurveyResult? = null,
    val errorMessage: String? = null,
)

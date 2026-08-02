package com.gayadi.android.feature.survey.presentation

import com.gayadi.android.domain.model.SurveyQuestion

data class SurveyUiState(
    val questions: List<SurveyQuestion> = emptyList(),
    val hasStarted: Boolean = false,
    val currentIndex: Int = 0,
    val selectedOption: Int? = null,
) {
    val currentQuestion: SurveyQuestion?
        get() = questions.getOrNull(currentIndex)

    val progress: Float
        get() = if (questions.isEmpty()) 0f else (currentIndex + 1f) / questions.size

    val isLastQuestion: Boolean
        get() = currentIndex == questions.lastIndex
}

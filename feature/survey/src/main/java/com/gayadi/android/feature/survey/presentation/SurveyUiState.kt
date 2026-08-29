package com.gayadi.android.feature.survey.presentation

import com.gayadi.android.domain.model.SurveyDefinition
import com.gayadi.android.domain.model.SurveyQuestion

/** Immutable state for the survey introduction and question flow. */
data class SurveyUiState(
    val definition: SurveyDefinition? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val resultErrorMessage: String? = null,
    val hasStarted: Boolean = false,
    val currentIndex: Int = 0,
    val selectedOption: Int? = null,
    val answers: Map<String, String> = emptyMap(),
    val isSubmitting: Boolean = false,
    val completedResultCode: String? = null,
) {
    /** Ordered questions from the loaded survey definition. */
    val questions: List<SurveyQuestion>
        get() = definition?.questions.orEmpty()

    /** Question displayed at the current survey index. */
    val currentQuestion: SurveyQuestion?
        get() = questions.getOrNull(currentIndex)

    /** Completion ratio in the inclusive range from zero to one. */
    val progress: Float
        get() = if (questions.isEmpty()) 0f else (currentIndex + 1f) / questions.size

    /** Whether the current index points at the final question. */
    val isLastQuestion: Boolean
        get() = currentIndex == questions.lastIndex

    /** Whether no survey content is currently available. */
    val isEmpty: Boolean
        get() = !isLoading && definition == null
}

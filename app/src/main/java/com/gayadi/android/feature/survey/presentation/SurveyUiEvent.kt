package com.gayadi.android.feature.survey.presentation

/** User-originated events handled during the travel survey. */
sealed interface SurveyUiEvent {
    /** Starts the survey from the introduction screen. */
    data object Start : SurveyUiEvent

    /** Selects an answer option for the current question. */
    data class OptionSelected(val index: Int) : SurveyUiEvent

    /** Requests movement to the next question or completion. */
    data object Next : SurveyUiEvent

    /** Requests survey questions again after an empty state. */
    data object Retry : SurveyUiEvent
}

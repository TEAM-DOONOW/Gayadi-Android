package com.gayadi.android.feature.survey.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.gayadi.android.domain.usecase.GetSurveyQuestionsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Owns survey flow state and exposes event-driven transitions. */
class SurveyViewModel(
    private val getSurveyQuestions: GetSurveyQuestionsUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        SurveyUiState(questions = getSurveyQuestions()),
    )
    /** Observable immutable state consumed by the Compose route. */
    val uiState: StateFlow<SurveyUiState> = _uiState.asStateFlow()

    /** Handles a survey event and returns true only when the survey completes. */
    fun onEvent(event: SurveyUiEvent): Boolean {
        when (event) {
            SurveyUiEvent.Start -> {
                if (!_uiState.value.isEmpty) {
                    _uiState.update { it.copy(hasStarted = true) }
                }
            }
            is SurveyUiEvent.OptionSelected -> {
                if (event.index in _uiState.value.currentQuestion.orEmptyOptions().indices) {
                    _uiState.update { it.copy(selectedOption = event.index) }
                }
            }
            SurveyUiEvent.Next -> {
                val state = _uiState.value
                if (state.selectedOption == null) return false
                if (state.isLastQuestion) return true
                _uiState.update {
                    it.copy(currentIndex = it.currentIndex + 1, selectedOption = null)
                }
            }
            SurveyUiEvent.Retry -> _uiState.update {
                it.copy(
                    questions = getSurveyQuestions(),
                    currentIndex = 0,
                    selectedOption = null,
                    hasStarted = false,
                )
            }
        }
        return false
    }

    companion object {
        /** Creates a ViewModel factory with the required use case. */
        fun factory(getSurveyQuestions: GetSurveyQuestionsUseCase) = viewModelFactory {
            initializer { SurveyViewModel(getSurveyQuestions) }
        }
    }
}

private fun com.gayadi.android.domain.model.SurveyQuestion?.orEmptyOptions(): List<String> =
    this?.options.orEmpty()

package com.gayadi.android.feature.survey.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.gayadi.android.domain.usecase.GetSurveyQuestionsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class SurveyViewModel(
    getSurveyQuestions: GetSurveyQuestionsUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        SurveyUiState(questions = getSurveyQuestions()),
    )
    val uiState: StateFlow<SurveyUiState> = _uiState.asStateFlow()

    fun startSurvey() {
        _uiState.update { it.copy(hasStarted = true) }
    }

    fun selectOption(index: Int) {
        if (index in _uiState.value.currentQuestion.orEmptyOptions().indices) {
            _uiState.update { it.copy(selectedOption = index) }
        }
    }

    fun moveToNextQuestion(): Boolean {
        val state = _uiState.value
        if (state.selectedOption == null) return false
        if (state.isLastQuestion) return true
        _uiState.update {
            it.copy(currentIndex = it.currentIndex + 1, selectedOption = null)
        }
        return false
    }

    companion object {
        fun factory(getSurveyQuestions: GetSurveyQuestionsUseCase) = viewModelFactory {
            initializer { SurveyViewModel(getSurveyQuestions) }
        }
    }
}

private fun com.gayadi.android.domain.model.SurveyQuestion?.orEmptyOptions(): List<String> =
    this?.options.orEmpty()

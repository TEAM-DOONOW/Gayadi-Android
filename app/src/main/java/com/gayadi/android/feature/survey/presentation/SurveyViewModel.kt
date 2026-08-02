package com.gayadi.android.feature.survey.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.gayadi.android.domain.usecase.CalculateSurveyResultUseCase
import com.gayadi.android.domain.usecase.GetSurveyUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Owns the Firestore-backed survey flow and result calculation. */
class SurveyViewModel(
    private val getSurvey: GetSurveyUseCase,
    private val calculateSurveyResult: CalculateSurveyResultUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(SurveyUiState())
    private var activeRequestGeneration = 0L
    /** Observable immutable state consumed by the Compose route. */
    val uiState: StateFlow<SurveyUiState> = _uiState.asStateFlow()

    init {
        loadSurvey()
    }

    /** Handles a survey event and returns a result code only when the final answer is submitted. */
    fun onEvent(event: SurveyUiEvent): String? {
        when (event) {
            SurveyUiEvent.Start -> {
                if (_uiState.value.definition != null) {
                    _uiState.update { it.copy(hasStarted = true) }
                }
            }

            is SurveyUiEvent.OptionSelected -> selectOption(event.index)
            SurveyUiEvent.Next -> return moveNextOrCalculate()
            SurveyUiEvent.Retry -> loadSurvey()
        }
        return null
    }

    private fun selectOption(index: Int) {
        val state = _uiState.value
        val question = state.currentQuestion ?: return
        val option = question.options.getOrNull(index) ?: return
        _uiState.update {
            it.copy(
                selectedOption = index,
                answers = it.answers + (question.id to option.code),
                resultErrorMessage = null,
            )
        }
    }

    private fun moveNextOrCalculate(): String? {
        val state = _uiState.value
        if (state.selectedOption == null) return null
        val definition = state.definition ?: return null
        if (state.isLastQuestion) {
            return runCatching { calculateSurveyResult(definition, state.answers) }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            resultErrorMessage = error.message ?: "결과를 계산하지 못했습니다.",
                        )
                    }
                }
                .getOrNull()
        }
        _uiState.update {
            it.copy(
                currentIndex = it.currentIndex + 1,
                selectedOption = null,
            )
        }
        return null
    }

    private fun loadSurvey() {
        val requestGeneration = ++activeRequestGeneration
        _uiState.value = SurveyUiState(isLoading = true)
        getSurvey callback@{ result ->
            if (requestGeneration != activeRequestGeneration) return@callback
            result.fold(
                onSuccess = { definition ->
                    _uiState.value = SurveyUiState(
                        definition = definition,
                        isLoading = false,
                    )
                },
                onFailure = { error ->
                    _uiState.value = SurveyUiState(
                        isLoading = false,
                        errorMessage = error.message ?: "설문을 불러오지 못했습니다.",
                    )
                },
            )
        }
    }

    override fun onCleared() {
        activeRequestGeneration++
        super.onCleared()
    }

    companion object {
        /** Creates a ViewModel factory with the required domain use cases. */
        fun factory(
            getSurvey: GetSurveyUseCase,
            calculateSurveyResult: CalculateSurveyResultUseCase,
        ) = viewModelFactory {
            initializer { SurveyViewModel(getSurvey, calculateSurveyResult) }
        }
    }
}

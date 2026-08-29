package com.gayadi.android.feature.survey.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.gayadi.android.domain.usecase.CalculateSurveyResultUseCase
import com.gayadi.android.domain.usecase.GetSurveyUseCase
import com.gayadi.android.domain.usecase.SubmitSurveyAnswersUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Owns the Firestore-backed survey flow and result calculation. */
class SurveyViewModel(
    private val getSurvey: GetSurveyUseCase,
    private val calculateSurveyResult: CalculateSurveyResultUseCase,
    private val submitSurveyAnswers: SubmitSurveyAnswersUseCase? = null,
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
            SurveyUiEvent.Start -> _uiState.update { it.copy(hasStarted = true) }

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
                answers = it.answers + (question.id to option.id),
                resultErrorMessage = null,
            )
        }
    }

    private fun moveNextOrCalculate(): String? {
        val state = _uiState.value
        if (state.selectedOption == null) return null
        val definition = state.definition ?: return null
        if (state.isLastQuestion) {
            val submit = submitSurveyAnswers
            if (submit != null) {
                _uiState.update { it.copy(isSubmitting = true, resultErrorMessage = null) }
                submit(state.answers) { result ->
                    result.fold(
                        onSuccess = { surveyResult ->
                            _uiState.update {
                                it.copy(isSubmitting = false, completedResultCode = surveyResult.code)
                            }
                        },
                        onFailure = { error ->
                            _uiState.update {
                                it.copy(
                                    isSubmitting = false,
                                    resultErrorMessage = error.message ?: "결과를 저장하지 못했습니다.",
                                )
                            }
                        },
                    )
                }
                return null
            }
            val scoringAnswers = definition.questions.associate { question ->
                val selectedId = state.answers[question.id]
                question.id to question.options.firstOrNull { it.id == selectedId }?.code.orEmpty()
            }
            return runCatching { calculateSurveyResult(definition, scoringAnswers) }
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

    fun consumeCompletedResult() {
        _uiState.update { it.copy(completedResultCode = null) }
    }

    /**
     * Loads the survey while preserving [SurveyUiState.hasStarted].
     *
     * The intro screen is shown before the content arrives, so the user can press start at any
     * point during the request. Resetting that flag here would bounce them back to the intro.
     */
    private fun loadSurvey() {
        val requestGeneration = ++activeRequestGeneration
        _uiState.value = SurveyUiState(isLoading = true, hasStarted = _uiState.value.hasStarted)
        getSurvey callback@{ result ->
            if (requestGeneration != activeRequestGeneration) return@callback
            result.fold(
                onSuccess = { definition ->
                    _uiState.value = SurveyUiState(
                        definition = definition,
                        isLoading = false,
                        hasStarted = _uiState.value.hasStarted,
                    )
                },
                onFailure = { error ->
                    _uiState.value = SurveyUiState(
                        isLoading = false,
                        errorMessage = error.message ?: "설문을 불러오지 못했습니다.",
                        hasStarted = _uiState.value.hasStarted,
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
            submitSurveyAnswers: SubmitSurveyAnswersUseCase? = null,
        ) = viewModelFactory {
            initializer { SurveyViewModel(getSurvey, calculateSurveyResult, submitSurveyAnswers) }
        }
    }
}

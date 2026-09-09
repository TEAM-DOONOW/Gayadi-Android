package com.gayadi.android.feature.survey.presentation

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import com.gayadi.android.domain.usecase.SubmitSurveyUseCase
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.gayadi.android.domain.usecase.CalculateSurveyResultUseCase
import com.gayadi.android.domain.usecase.GetSurveyUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Owns survey loading, answer selection, and backend submission. */
class SurveyViewModel(
    private val getSurvey: GetSurveyUseCase,
    private val calculateSurveyResult: CalculateSurveyResultUseCase,
    private val submitSurvey: SubmitSurveyUseCase? = null,
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
        if (_uiState.value.isSubmitting || _uiState.value.completedResultCode != null) return null
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
                answers = it.answers + (question.id to option.code),
                resultErrorMessage = null,
            )
        }
    }

    private fun moveNextOrCalculate(): String? {
        val state = _uiState.value
        if (state.selectedOption == null) return null
        val definition = state.definition ?: return null
        if (state.isLastQuestion && submitSurvey != null) {
            _uiState.update { it.copy(isSubmitting = true, resultErrorMessage = null) }
            viewModelScope.launch {
                try {
                    val resultCode = submitSurvey.invoke(definition, state.answers)
                    _uiState.update { it.copy(isSubmitting = false, completedResultCode = resultCode) }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: Exception) {
                    _uiState.update { it.copy(isSubmitting = false,
                        resultErrorMessage = error.message ?: "설문을 저장하지 못했어요. 다시 시도해 주세요.") }
                }
            }
            return null
        }
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
            submitSurvey: SubmitSurveyUseCase? = null,
        ) = viewModelFactory {
            initializer { SurveyViewModel(getSurvey, calculateSurveyResult, submitSurvey) }
        }
    }
}

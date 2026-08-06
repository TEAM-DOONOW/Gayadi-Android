package com.gayadi.android.feature.surveyresult.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.gayadi.android.domain.usecase.GetSurveyResultUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Loads and owns one travel-style result card. */
class SurveyResultViewModel(
    private val resultCode: String,
    private val getSurveyResult: GetSurveyResultUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(SurveyResultUiState())
    val uiState: StateFlow<SurveyResultUiState> = _uiState.asStateFlow()

    init {
        loadResult()
    }

    fun retry() = loadResult()

    private fun loadResult() {
        _uiState.value = SurveyResultUiState(isLoading = true)
        getSurveyResult(resultCode) { result ->
            _uiState.value = result.fold(
                onSuccess = { SurveyResultUiState(isLoading = false, result = it) },
                onFailure = {
                    SurveyResultUiState(
                        isLoading = false,
                        errorMessage = it.message ?: "결과를 불러오지 못했습니다.",
                    )
                },
            )
        }
    }

    companion object {
        fun factory(resultCode: String, getSurveyResult: GetSurveyResultUseCase) = viewModelFactory {
            initializer { SurveyResultViewModel(resultCode, getSurveyResult) }
        }
    }
}

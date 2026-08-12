package com.gayadi.android.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.gayadi.android.domain.model.SurveyResult
import com.gayadi.android.domain.usecase.GetSurveyResultUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class TravelProfileResultUiState(
    val result: SurveyResult? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
)

/** Loads the complete Firestore result referenced by the locally saved profile result code. */
class TravelProfileResultViewModel(
    private val resultCode: String?,
    private val getSurveyResult: GetSurveyResultUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(TravelProfileResultUiState())
    val uiState: StateFlow<TravelProfileResultUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun retry() = load()

    private fun load() {
        val code = resultCode?.takeIf(String::isNotBlank)
        if (code == null) {
            _uiState.value = TravelProfileResultUiState(
                isLoading = false,
                errorMessage = "저장된 여행 유형 결과가 없어요.",
            )
            return
        }

        _uiState.value = TravelProfileResultUiState(isLoading = true)
        getSurveyResult(code) { result ->
            result.fold(
                onSuccess = { surveyResult ->
                    _uiState.value = TravelProfileResultUiState(result = surveyResult, isLoading = false)
                },
                onFailure = { error ->
                    _uiState.value = TravelProfileResultUiState(
                        isLoading = false,
                        errorMessage = error.message ?: "여행 유형 결과를 불러오지 못했습니다.",
                    )
                },
            )
        }
    }

    companion object {
        fun factory(resultCode: String?, getSurveyResult: GetSurveyResultUseCase) = viewModelFactory {
            initializer { TravelProfileResultViewModel(resultCode, getSurveyResult) }
        }
    }
}

package com.gayadi.android.feature.surveyresult.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.gayadi.android.domain.usecase.GetBasicInfoUseCase
import com.gayadi.android.domain.usecase.GetSurveyResultUseCase
import com.gayadi.android.domain.usecase.SaveSurveyResultToProfileUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Loads and owns one travel-style result card. */
class SurveyResultViewModel(
    private val resultCode: String,
    private val getSurveyResult: GetSurveyResultUseCase,
    private val getBasicInfo: GetBasicInfoUseCase,
    private val saveSurveyResultToProfile: SaveSurveyResultToProfileUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(SurveyResultUiState())
    val uiState: StateFlow<SurveyResultUiState> = _uiState.asStateFlow()

    init {
        loadResult()
    }

    fun retry() = loadResult()

    private fun loadResult() {
        val nickname = getBasicInfo()?.nickname?.takeIf(String::isNotBlank)
        _uiState.value = SurveyResultUiState(isLoading = true, nickname = nickname)
        getSurveyResult(resultCode) { result ->
            _uiState.value = result.fold(
                onSuccess = {
                    saveSurveyResultToProfile(it)
                    SurveyResultUiState(isLoading = false, result = it, nickname = nickname)
                },
                onFailure = {
                    SurveyResultUiState(
                        isLoading = false,
                        nickname = nickname,
                        errorMessage = it.message ?: "결과를 불러오지 못했습니다.",
                    )
                },
            )
        }
    }

    companion object {
        fun factory(
            resultCode: String,
            getSurveyResult: GetSurveyResultUseCase,
            getBasicInfo: GetBasicInfoUseCase,
            saveSurveyResultToProfile: SaveSurveyResultToProfileUseCase,
        ) = viewModelFactory {
            initializer {
                SurveyResultViewModel(
                    resultCode,
                    getSurveyResult,
                    getBasicInfo,
                    saveSurveyResultToProfile,
                )
            }
        }
    }
}

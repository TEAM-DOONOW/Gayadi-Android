package com.gayadi.android.feature.surveyresult.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.gayadi.android.domain.usecase.GetBasicInfoUseCase
import com.gayadi.android.domain.usecase.GetSurveyResultUseCase
import com.gayadi.android.domain.usecase.SaveSurveyResultToProfileUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Loads and owns one travel-style result card. */
class SurveyResultViewModel(
    private val resultCode: String,
    private val getSurveyResult: GetSurveyResultUseCase,
    private val getBasicInfo: GetBasicInfoUseCase,
    private val saveSurveyResultToProfile: SaveSurveyResultToProfileUseCase,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {
    private val _uiState = MutableStateFlow(SurveyResultUiState())
    val uiState: StateFlow<SurveyResultUiState> = _uiState.asStateFlow()

    init {
        loadResult()
    }

    fun retry() = loadResult()

    private fun loadResult() {
        _uiState.value = SurveyResultUiState(isLoading = true)
        viewModelScope.launch(ioDispatcher) {
            val basicInfo = getBasicInfo()
            val nickname = basicInfo?.nickname?.takeIf(String::isNotBlank)
            val introduction = basicInfo?.introduction?.takeIf(String::isNotBlank)
            _uiState.value = SurveyResultUiState(
                isLoading = true,
                nickname = nickname,
                introduction = introduction,
            )
            getSurveyResult(resultCode) { result ->
                result.fold(
                    onSuccess = { surveyResult ->
                        viewModelScope.launch(ioDispatcher) {
                            saveSurveyResultToProfile(surveyResult).fold(
                                onSuccess = {
                                    _uiState.value = SurveyResultUiState(
                                        isLoading = false,
                                        result = surveyResult,
                                        nickname = nickname,
                                        introduction = introduction,
                                    )
                                },
                                onFailure = { error ->
                                    _uiState.value = SurveyResultUiState(
                                        isLoading = false,
                                        nickname = nickname,
                                        introduction = introduction,
                                        errorMessage = error.message ?: "결과를 저장하지 못했습니다.",
                                    )
                                },
                            )
                        }
                    },
                    onFailure = { error ->
                        _uiState.value = SurveyResultUiState(
                        isLoading = false,
                        nickname = nickname,
                        introduction = introduction,
                            errorMessage = error.message ?: "결과를 불러오지 못했습니다.",
                        )
                    },
                )
            }
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

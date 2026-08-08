package com.gayadi.android.feature.basicinfo.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.gayadi.android.domain.usecase.SaveBasicInfoUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Owns basic information form state and delegates persistence to the domain layer. */
class BasicInfoViewModel(
    private val saveBasicInfo: SaveBasicInfoUseCase,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {
    private val _uiState = MutableStateFlow(BasicInfoUiState())
    /** Observable immutable state consumed by the Compose route. */
    val uiState: StateFlow<BasicInfoUiState> = _uiState.asStateFlow()

    /** Handles form input and persistence events. */
    fun onEvent(event: BasicInfoUiEvent) {
        when (event) {
            is BasicInfoUiEvent.NicknameChanged ->
                _uiState.update {
                    it.copy(nickname = event.value.take(10), saveCompleted = false, errorMessage = null)
                }
            is BasicInfoUiEvent.IntroductionChanged ->
                _uiState.update {
                    it.copy(introduction = event.value.take(20), saveCompleted = false, errorMessage = null)
                }
            BasicInfoUiEvent.Submit -> {
                val state = _uiState.value
                if (!state.canSubmit) return
                _uiState.update { it.copy(isSaving = true, saveCompleted = false, errorMessage = null) }
                viewModelScope.launch(ioDispatcher) {
                    runCatching { saveBasicInfo(state.nickname, state.introduction) }
                        .onSuccess {
                            _uiState.update { it.copy(isSaving = false, saveCompleted = true) }
                        }
                        .onFailure { error ->
                            _uiState.update {
                                it.copy(
                                    isSaving = false,
                                    errorMessage = error.message ?: "기본 정보를 저장하지 못했습니다.",
                                )
                            }
                        }
                }
            }
        }
    }

    /** Consumes the one-shot navigation signal after the route handles it. */
    fun consumeSaveCompleted() {
        _uiState.update { it.copy(saveCompleted = false) }
    }

    companion object {
        /** Creates a ViewModel factory with the required use case. */
        fun factory(saveBasicInfo: SaveBasicInfoUseCase) = viewModelFactory {
            initializer { BasicInfoViewModel(saveBasicInfo) }
        }
    }
}

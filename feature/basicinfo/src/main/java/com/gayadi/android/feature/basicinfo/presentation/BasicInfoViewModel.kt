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

    /** Handles a UI event and returns true only when submission succeeds. */
    fun onEvent(event: BasicInfoUiEvent): Boolean {
        when (event) {
            is BasicInfoUiEvent.NicknameChanged ->
                _uiState.update { it.copy(nickname = event.value.take(10)) }
            is BasicInfoUiEvent.IntroductionChanged ->
                _uiState.update { it.copy(introduction = event.value.take(20)) }
            BasicInfoUiEvent.Submit -> {
                val state = _uiState.value
                if (!state.canSubmit) return false
                viewModelScope.launch(ioDispatcher) {
                    saveBasicInfo(state.nickname, state.introduction)
                }
                return true
            }
        }
        return false
    }

    companion object {
        /** Creates a ViewModel factory with the required use case. */
        fun factory(saveBasicInfo: SaveBasicInfoUseCase) = viewModelFactory {
            initializer { BasicInfoViewModel(saveBasicInfo) }
        }
    }
}

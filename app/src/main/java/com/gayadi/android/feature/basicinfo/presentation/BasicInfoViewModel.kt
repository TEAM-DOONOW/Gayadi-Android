package com.gayadi.android.feature.basicinfo.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.gayadi.android.domain.usecase.SaveBasicInfoUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class BasicInfoViewModel(
    private val saveBasicInfo: SaveBasicInfoUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(BasicInfoUiState())
    val uiState: StateFlow<BasicInfoUiState> = _uiState.asStateFlow()

    fun onNicknameChanged(value: String) {
        _uiState.update { it.copy(nickname = value.take(10)) }
    }

    fun onIntroductionChanged(value: String) {
        _uiState.update { it.copy(introduction = value.take(20)) }
    }

    fun submit(): Boolean {
        val state = _uiState.value
        if (!state.canSubmit) return false
        saveBasicInfo(state.nickname, state.introduction)
        return true
    }

    companion object {
        fun factory(saveBasicInfo: SaveBasicInfoUseCase) = viewModelFactory {
            initializer { BasicInfoViewModel(saveBasicInfo) }
        }
    }
}

package com.gayadi.android.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.gayadi.android.domain.model.UserProfile
import com.gayadi.android.domain.usecase.GetUserProfileUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ProfileUiState(val profile: UserProfile? = null)

class ProfileViewModel(getUserProfile: GetUserProfileUseCase) : ViewModel() {
    private val _uiState = MutableStateFlow(ProfileUiState(getUserProfile()))
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    companion object {
        fun factory(getUserProfile: GetUserProfileUseCase) = viewModelFactory {
            initializer { ProfileViewModel(getUserProfile) }
        }
    }
}

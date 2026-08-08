package com.gayadi.android.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.gayadi.android.domain.model.UserProfile
import com.gayadi.android.domain.usecase.GetUserProfileUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class RescheduleDecision { PENDING, ACCEPTED, REJECTED }

data class RealtimeHomeUiState(
    val profile: UserProfile? = null,
    val showRescheduleSheet: Boolean = false,
    val rescheduleDecision: RescheduleDecision = RescheduleDecision.PENDING,
)

class RealtimeHomeViewModel(
    getUserProfile: GetUserProfileUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        RealtimeHomeUiState(profile = getUserProfile()),
    )
    val uiState: StateFlow<RealtimeHomeUiState> = _uiState.asStateFlow()

    fun openRescheduleSuggestion() {
        if (_uiState.value.rescheduleDecision == RescheduleDecision.PENDING) {
            _uiState.update { it.copy(showRescheduleSheet = true) }
        }
    }

    fun dismissRescheduleSuggestion() {
        _uiState.update { it.copy(showRescheduleSheet = false) }
    }

    fun acceptRescheduleSuggestion() {
        _uiState.update {
            it.copy(
                showRescheduleSheet = false,
                rescheduleDecision = RescheduleDecision.ACCEPTED,
            )
        }
    }

    fun rejectRescheduleSuggestion() {
        _uiState.update {
            it.copy(
                showRescheduleSheet = false,
                rescheduleDecision = RescheduleDecision.REJECTED,
            )
        }
    }

    companion object {
        fun factory(getUserProfile: GetUserProfileUseCase) = viewModelFactory {
            initializer { RealtimeHomeViewModel(getUserProfile) }
        }
    }
}

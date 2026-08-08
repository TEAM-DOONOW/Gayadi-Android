package com.gayadi.android.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.gayadi.android.domain.model.UserProfile
import com.gayadi.android.domain.usecase.GetUserProfileUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

enum class RescheduleDecision { PENDING, ACCEPTED, REJECTED }

data class RealtimeHomeUiState(
    val profile: UserProfile? = null,
    val isProfileLoading: Boolean = true,
    val profileErrorMessage: String? = null,
    val showRescheduleSheet: Boolean = false,
    val rescheduleDecision: RescheduleDecision = RescheduleDecision.PENDING,
)

class RealtimeHomeViewModel(
    getUserProfile: GetUserProfileUseCase,
    ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {
    private val _uiState = MutableStateFlow(RealtimeHomeUiState())
    val uiState: StateFlow<RealtimeHomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch(ioDispatcher) {
            runCatching { getUserProfile() }.fold(
                onSuccess = { profile -> _uiState.update { it.copy(profile = profile, isProfileLoading = false) } },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(isProfileLoading = false, profileErrorMessage = error.message ?: "프로필을 불러오지 못했습니다.")
                    }
                },
            )
        }
    }

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
                rescheduleDecision = if (it.rescheduleDecision == RescheduleDecision.PENDING) {
                    RescheduleDecision.ACCEPTED
                } else {
                    it.rescheduleDecision
                },
            )
        }
    }

    fun rejectRescheduleSuggestion() {
        _uiState.update {
            it.copy(
                showRescheduleSheet = false,
                rescheduleDecision = if (it.rescheduleDecision == RescheduleDecision.PENDING) {
                    RescheduleDecision.REJECTED
                } else {
                    it.rescheduleDecision
                },
            )
        }
    }

    companion object {
        fun factory(getUserProfile: GetUserProfileUseCase) = viewModelFactory {
            initializer { RealtimeHomeViewModel(getUserProfile) }
        }
    }
}

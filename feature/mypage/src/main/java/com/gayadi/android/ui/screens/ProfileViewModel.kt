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
import kotlinx.coroutines.CancellationException
import com.gayadi.android.domain.error.rethrowCancellation
import com.gayadi.android.domain.error.userFacingMessage
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

data class ProfileUiState(
    val profile: UserProfile? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
)

class ProfileViewModel(
    private val getUserProfile: GetUserProfileUseCase,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()
    private var reloadJob: Job? = null

    init {
        reload()
    }

    fun reload() {
        reloadJob?.cancel()
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        reloadJob = viewModelScope.launch(ioDispatcher) {
            runCatchingPreservingCancellation { getUserProfile() }.fold(
                onSuccess = { profile -> _uiState.update { it.copy(profile = profile, isLoading = false) } },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.userFacingMessage("프로필을 불러오지 못했습니다."),
                        )
                    }
                },
            )
        }
    }

    fun showError(message: String) {
        _uiState.update { it.copy(isLoading = false, errorMessage = message) }
    }

    companion object {
        fun factory(getUserProfile: GetUserProfileUseCase) = viewModelFactory {
            initializer { ProfileViewModel(getUserProfile) }
        }
    }
}

private inline fun <T> runCatchingPreservingCancellation(block: () -> T): Result<T> = try {
    Result.success(block())
} catch (cancelled: CancellationException) {
    throw cancelled
} catch (error: Exception) {
    error.rethrowCancellation()
    Result.failure(error)
}

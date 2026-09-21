package com.gayadi.android.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialInterruptedException
import com.gayadi.android.domain.model.UserProfile
import com.gayadi.android.domain.usecase.GetUserProfileUseCase
import com.gayadi.android.domain.usecase.SignInWithGoogleUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class GoogleLoginUiState(
    val isLoginInProgress: Boolean = false,
    val loginError: String? = null,
    val loginCompleted: Boolean = false,
    val completedProfile: UserProfile? = null,
)

class GoogleLoginViewModel(
    private val signInWithGoogle: SignInWithGoogleUseCase,
    private val getUserProfile: GetUserProfileUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(GoogleLoginUiState())
    val uiState: StateFlow<GoogleLoginUiState> = _uiState.asStateFlow()

    fun signIn(requestToken: suspend () -> String) {
        if (_uiState.value.isLoginInProgress) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoginInProgress = true, loginError = null) }
            try {
                val idToken = requestToken()
                val profile = withContext(NonCancellable) {
                    signInWithGoogle(idToken)
                    getUserProfile()
                }
                _uiState.update {
                    it.copy(
                        isLoginInProgress = false,
                        loginError = null,
                        loginCompleted = true,
                        completedProfile = profile,
                    )
                }
            } catch (exception: GetCredentialCancellationException) {
                _uiState.update {
                    it.copy(isLoginInProgress = false, loginError = GOOGLE_LOGIN_CANCELLED_MESSAGE)
                }
            } catch (exception: GetCredentialInterruptedException) {
                _uiState.update {
                    it.copy(isLoginInProgress = false, loginError = GOOGLE_LOGIN_CANCELLED_MESSAGE)
                }
            } catch (cancelled: CancellationException) {
                if (!isActive) throw cancelled
                _uiState.update {
                    it.copy(isLoginInProgress = false, loginError = GOOGLE_LOGIN_CANCELLED_MESSAGE)
                }
            } catch (exception: Exception) {
                _uiState.update {
                    it.copy(
                        isLoginInProgress = false,
                        loginError = googleLoginUserMessage(exception),
                    )
                }
            }
        }
    }

    fun consumeCompletion() {
        _uiState.update { it.copy(loginCompleted = false, completedProfile = null) }
    }

    companion object {
        fun factory(
            signInWithGoogle: SignInWithGoogleUseCase,
            getUserProfile: GetUserProfileUseCase,
        ) = viewModelFactory {
            initializer { GoogleLoginViewModel(signInWithGoogle, getUserProfile) }
        }
    }
}

package com.gayadi.android.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.gayadi.android.domain.model.AuthSession
import com.gayadi.android.domain.repository.AuthRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AuthMode { LOGIN, SIGN_UP }

data class AuthCompletion(
    val session: AuthSession,
    val isNewAccount: Boolean,
)

data class AuthUiState(
    val mode: AuthMode = AuthMode.LOGIN,
    val email: String = "",
    val password: String = "",
    val nickname: String = "",
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
    val completion: AuthCompletion? = null,
) {
    val canSubmit: Boolean
        get() = email.isNotBlank() && password.isNotBlank() &&
            (mode == AuthMode.LOGIN || nickname.isNotBlank()) && !isSubmitting
}

class AuthViewModel(
    private val authRepository: AuthRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun selectMode(mode: AuthMode) {
        if (_uiState.value.isSubmitting || _uiState.value.mode == mode) return
        _uiState.update { it.copy(mode = mode, password = "", errorMessage = null, completion = null) }
    }

    fun updateEmail(value: String) {
        _uiState.update { it.copy(email = value.take(MAX_EMAIL_LENGTH), errorMessage = null) }
    }

    fun updatePassword(value: String) {
        _uiState.update { it.copy(password = value.take(MAX_PASSWORD_LENGTH), errorMessage = null) }
    }

    fun updateNickname(value: String) {
        _uiState.update { it.copy(nickname = value.take(MAX_NICKNAME_LENGTH), errorMessage = null) }
    }

    fun submit() {
        val state = _uiState.value
        if (!state.canSubmit) return
        val validationError = validate(state)
        if (validationError != null) {
            _uiState.update { it.copy(errorMessage = validationError) }
            return
        }

        _uiState.update { it.copy(isSubmitting = true, errorMessage = null, completion = null) }
        viewModelScope.launch(ioDispatcher) {
            val result = when (state.mode) {
                AuthMode.LOGIN -> authRepository.login(state.email.trim(), state.password)
                AuthMode.SIGN_UP -> authRepository.signup(
                    email = state.email.trim(),
                    password = state.password,
                    nickname = state.nickname.trim(),
                )
            }
            result.fold(
                onSuccess = { session ->
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            password = "",
                            completion = AuthCompletion(session, state.mode == AuthMode.SIGN_UP),
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            errorMessage = error.message ?: "로그인 요청을 처리하지 못했습니다.",
                        )
                    }
                },
            )
        }
    }

    fun consumeCompletion() {
        _uiState.update { it.copy(completion = null) }
    }

    private fun validate(state: AuthUiState): String? {
        val email = state.email.trim()
        if (!EMAIL_PATTERN.matches(email)) return "올바른 이메일 주소를 입력해 주세요."
        if (state.password.length > MAX_PASSWORD_LENGTH) return "비밀번호는 72자 이하여야 합니다."
        if (state.mode == AuthMode.SIGN_UP && state.password.length < MIN_SIGN_UP_PASSWORD_LENGTH) {
            return "비밀번호는 6자 이상이어야 합니다."
        }
        if (state.mode == AuthMode.SIGN_UP && !NICKNAME_PATTERN.matches(state.nickname.trim())) {
            return "닉네임은 문자, 숫자, 공백, 밑줄, 하이픈만 사용할 수 있습니다."
        }
        return null
    }

    companion object {
        private const val MAX_EMAIL_LENGTH = 255
        private const val MIN_SIGN_UP_PASSWORD_LENGTH = 6
        private const val MAX_PASSWORD_LENGTH = 72
        private const val MAX_NICKNAME_LENGTH = 10
        private val EMAIL_PATTERN = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")
        private val NICKNAME_PATTERN = Regex("^[\\p{L}\\p{N} _-]+$")

        fun factory(authRepository: AuthRepository) = viewModelFactory {
            initializer { AuthViewModel(authRepository) }
        }
    }
}

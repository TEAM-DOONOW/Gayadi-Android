package com.gayadi.android.domain.usecase

import com.gayadi.android.domain.model.AuthSession
import com.gayadi.android.domain.repository.AuthRepository

class SignInWithGoogleUseCase(
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke(idToken: String): AuthSession {
        require(idToken.isNotBlank()) { "Google ID Token이 비어 있습니다." }
        return authRepository.signInWithGoogle(idToken)
    }
}

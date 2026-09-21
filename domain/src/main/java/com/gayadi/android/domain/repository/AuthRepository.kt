package com.gayadi.android.domain.repository

import com.gayadi.android.domain.model.AuthSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

interface AuthRepository {
    suspend fun signInWithGoogle(idToken: String): AuthSession
    suspend fun refreshSession(): AuthSession
    suspend fun validAccessToken(): String
    fun currentSession(): AuthSession?
    fun observeSession(): Flow<AuthSession?> = emptyFlow()
    fun clearSession()
}

package com.gayadi.android.domain.repository

import com.gayadi.android.domain.model.AuthSession

interface AuthRepository {
    suspend fun signInWithGoogle(idToken: String): AuthSession
    suspend fun refreshSession(): AuthSession
    suspend fun validAccessToken(): String
    fun currentSession(): AuthSession?
    fun clearSession()
}

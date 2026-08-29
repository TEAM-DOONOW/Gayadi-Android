package com.gayadi.android.domain.repository

import com.gayadi.android.domain.model.AuthSession
import com.gayadi.android.domain.model.AuthUser

/** Server-backed account registration, authentication, and session contract. */
interface AuthRepository {
    suspend fun signup(email: String, password: String, nickname: String): Result<AuthSession>

    suspend fun login(email: String, password: String): Result<AuthSession>

    suspend fun current(): Result<AuthUser>

    /** Ends the local session. The server currently has no token-revocation endpoint. */
    suspend fun logout(): Result<Unit>

    /** Permanently withdraws the account and clears the local session on success. */
    suspend fun withdraw(): Result<Unit>
}

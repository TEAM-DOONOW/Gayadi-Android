package com.gayadi.android.data.repository

import com.gayadi.android.data.datasource.AuthApiDataSource
import com.gayadi.android.data.datasource.AuthTokenRequestException
import com.gayadi.android.domain.model.AuthSession
import com.gayadi.android.domain.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class DefaultAuthRepository(
    private val authApiDataSource: AuthApiDataSource,
    private val sessionStore: AuthSessionStore = InMemoryAuthSessionStore(),
    private val epochSeconds: () -> Long = { System.currentTimeMillis() / 1000 },
) : AuthRepository {
    private val sessionState = MutableStateFlow(sessionStore.load())
    private val refreshMutex = Mutex()

    override suspend fun signInWithGoogle(idToken: String): AuthSession =
        authApiDataSource.exchangeGoogleIdToken(idToken).also(::replaceSession)

    override suspend fun refreshSession(): AuthSession = refreshMutex.withLock {
        val current = sessionState.value ?: throw expiredSession()
        if (current.isRefreshTokenExpired(epochSeconds())) throw expiredSession()
        refresh(current)
    }

    override suspend fun validAccessToken(): String {
        val current = requireNotNull(sessionState.value) { "로그인 세션이 없습니다." }
        if (!current.isAccessTokenExpiring(epochSeconds())) return current.accessToken
        return refreshMutex.withLock {
            val latest = sessionState.value ?: throw expiredSession()
            if (!latest.isAccessTokenExpiring(epochSeconds())) {
                latest.accessToken
            } else {
                if (latest.isRefreshTokenExpired(epochSeconds())) throw expiredSession()
                refresh(latest).accessToken
            }
        }
    }

    override fun currentSession(): AuthSession? = sessionState.value

    override fun observeSession(): StateFlow<AuthSession?> = sessionState.asStateFlow()

    override fun clearSession() {
        sessionStore.clear()
        sessionState.value = null
    }

    private fun replaceSession(newSession: AuthSession) {
        sessionStore.save(newSession)
        sessionState.value = newSession
    }

    private suspend fun refresh(session: AuthSession): AuthSession = try {
        authApiDataSource.refreshToken(session.refreshToken).also(::replaceSession)
    } catch (error: AuthTokenRequestException) {
        if (
            error.statusCode == 400 ||
            error.statusCode == 401 ||
            error.statusCode == 403 ||
            error.errorCode.equals("AUTH_REFRESH_TOKEN_INVALID", ignoreCase = true)
        ) {
            throw expiredSession(error)
        }
        throw error
    }

    private fun expiredSession(cause: Throwable? = null): IllegalStateException {
        clearSession()
        return IllegalStateException(SESSION_EXPIRED_MESSAGE, cause)
    }

    private companion object {
        const val SESSION_EXPIRED_MESSAGE = "로그인이 만료되었어요. 다시 로그인해 주세요."
    }
}

interface AuthSessionStore {
    fun load(): AuthSession?
    fun save(session: AuthSession)
    fun clear()
}

class InMemoryAuthSessionStore : AuthSessionStore {
    private var session: AuthSession? = null

    override fun load(): AuthSession? = session

    override fun save(session: AuthSession) {
        this.session = session
    }

    override fun clear() {
        session = null
    }
}

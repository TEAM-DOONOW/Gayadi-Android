package com.gayadi.android.data.repository

import com.gayadi.android.data.datasource.AuthApiDataSource
import com.gayadi.android.domain.model.AuthSession
import com.gayadi.android.domain.repository.AuthRepository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class DefaultAuthRepository(
    private val authApiDataSource: AuthApiDataSource,
    private val sessionStore: AuthSessionStore = InMemoryAuthSessionStore(),
    private val epochSeconds: () -> Long = { System.currentTimeMillis() / 1000 },
) : AuthRepository {
    @Volatile
    private var session: AuthSession? = sessionStore.load()
    private val refreshMutex = Mutex()

    override suspend fun signInWithGoogle(idToken: String): AuthSession =
        authApiDataSource.exchangeGoogleIdToken(idToken).also(::replaceSession)

    override suspend fun refreshSession(): AuthSession = refreshMutex.withLock {
        val current = requireNotNull(session) { "로그인 세션이 없습니다." }
        require(!current.isRefreshTokenExpired(epochSeconds())) { "로그인 세션이 만료되었습니다." }
        authApiDataSource.refreshToken(current.refreshToken).also(::replaceSession)
    }

    override suspend fun validAccessToken(): String {
        val current = requireNotNull(session) { "로그인 세션이 없습니다." }
        if (!current.isAccessTokenExpiring(epochSeconds())) return current.accessToken
        return refreshMutex.withLock {
            val latest = requireNotNull(session) { "로그인 세션이 없습니다." }
            if (!latest.isAccessTokenExpiring(epochSeconds())) {
                latest.accessToken
            } else {
                require(!latest.isRefreshTokenExpired(epochSeconds())) { "로그인 세션이 만료되었습니다." }
                authApiDataSource.refreshToken(latest.refreshToken).also(::replaceSession).accessToken
            }
        }
    }

    override fun currentSession(): AuthSession? = session

    override fun clearSession() {
        session = null
        sessionStore.clear()
    }

    private fun replaceSession(newSession: AuthSession) {
        sessionStore.save(newSession)
        session = newSession
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

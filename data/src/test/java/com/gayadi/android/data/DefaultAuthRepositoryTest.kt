package com.gayadi.android.data

import com.gayadi.android.data.datasource.AuthApiDataSource
import com.gayadi.android.data.repository.DefaultAuthRepository
import com.gayadi.android.data.repository.InMemoryAuthSessionStore
import com.gayadi.android.domain.model.AuthSession
import com.gayadi.android.domain.model.AuthUser
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class DefaultAuthRepositoryTest {
    @Test
    fun expiredAccessToken_isRefreshedAndRotated() = runTest {
        val api = RecordingAuthApiDataSource(session("access-1", "refresh-1", issuedAt = 1))
        val store = InMemoryAuthSessionStore().apply { save(api.loginSession) }
        val repository = DefaultAuthRepository(api, store) { 100 }

        val token = repository.validAccessToken()

        assertEquals("access-2", token)
        assertEquals("refresh-1", api.receivedRefreshToken)
        assertEquals("refresh-2", repository.currentSession()?.refreshToken)
    }

    @Test
    fun concurrentExpiredRequests_performOnlyOneRefresh() = runTest {
        val api = RecordingAuthApiDataSource(session("access-1", "refresh-1", issuedAt = 1))
        val store = InMemoryAuthSessionStore().apply { save(api.loginSession) }
        val repository = DefaultAuthRepository(api, store) { 100 }

        val tokens = List(5) { async { repository.validAccessToken() } }.awaitAll()

        assertEquals(List(5) { "access-2" }, tokens)
        assertEquals(1, api.refreshCount)
    }

    private fun session(access: String, refresh: String, issuedAt: Long) = AuthSession(
        accessToken = access,
        tokenType = "Bearer",
        expiresInSeconds = 120,
        refreshToken = refresh,
        refreshExpiresInSeconds = 1_000,
        issuedAtEpochSeconds = issuedAt,
        user = AuthUser(12, "가야디", "traveler@example.com"),
    )

    private inner class RecordingAuthApiDataSource(
        val loginSession: AuthSession,
    ) : AuthApiDataSource {
        var receivedRefreshToken: String? = null
        var refreshCount = 0

        override suspend fun exchangeGoogleIdToken(idToken: String): AuthSession = loginSession

        override suspend fun refreshToken(refreshToken: String): AuthSession {
            refreshCount += 1
            receivedRefreshToken = refreshToken
            return session("access-2", "refresh-2", issuedAt = 100)
        }
    }
}

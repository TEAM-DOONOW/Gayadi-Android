package com.gayadi.android.data

import com.gayadi.android.data.datasource.AuthApiDataSource
import com.gayadi.android.data.datasource.AuthTokenRequestException
import com.gayadi.android.data.repository.DefaultAuthRepository
import com.gayadi.android.data.repository.InMemoryAuthSessionStore
import com.gayadi.android.domain.model.AuthSession
import com.gayadi.android.domain.model.AuthUser
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
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

    @Test
    fun observeSessionEmitsAfterGoogleSignIn() = runTest {
        val api = RecordingAuthApiDataSource(session("access-1", "refresh-1", issuedAt = 1))
        val repository = DefaultAuthRepository(api, InMemoryAuthSessionStore()) { 100 }

        assertEquals(null, repository.currentSession())
        repository.signInWithGoogle("id-token")
        assertEquals("access-1", repository.currentSession()?.accessToken)
    }

    @Test
    fun rejectedRefreshToken_clearsStaleSession() = runTest {
        val api = RecordingAuthApiDataSource(session("access-1", "refresh-1", issuedAt = 1)).apply {
            refreshFailure = AuthTokenRequestException(401, "AUTH_REFRESH_TOKEN_INVALID", "The refresh token is invalid or expired.")
        }
        val store = InMemoryAuthSessionStore().apply { save(api.loginSession) }
        val repository = DefaultAuthRepository(api, store) { 100 }

        val error = assertThrows(IllegalStateException::class.java) {
            kotlinx.coroutines.runBlocking { repository.validAccessToken() }
        }

        assertEquals("로그인이 만료되었어요. 다시 로그인해 주세요.", error.message)
        assertNull(repository.currentSession())
        assertNull(store.load())
    }

    @Test
    fun locallyExpiredRefreshToken_clearsStaleSession() = runTest {
        val api = RecordingAuthApiDataSource(session("access-1", "refresh-1", issuedAt = 1))
        val store = InMemoryAuthSessionStore().apply {
            save(api.loginSession.copy(expiresInSeconds = 1, refreshExpiresInSeconds = 2))
        }
        val repository = DefaultAuthRepository(api, store) { 100 }

        val error = assertThrows(IllegalStateException::class.java) {
            kotlinx.coroutines.runBlocking { repository.validAccessToken() }
        }

        assertEquals("로그인이 만료되었어요. 다시 로그인해 주세요.", error.message)
        assertNull(repository.currentSession())
        assertEquals(0, api.refreshCount)
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
        var refreshFailure: Exception? = null

        override suspend fun exchangeGoogleIdToken(idToken: String): AuthSession = loginSession

        override suspend fun refreshToken(refreshToken: String): AuthSession {
            refreshCount += 1
            receivedRefreshToken = refreshToken
            refreshFailure?.let { throw it }
            return session("access-2", "refresh-2", issuedAt = 100)
        }
    }
}

package com.gayadi.android.data.repository

import com.gayadi.android.data.remote.GayadiHttpClient
import com.gayadi.android.data.remote.TokenStore
import com.gayadi.android.data.remote.toAuthSession
import com.gayadi.android.data.remote.toAuthUser
import com.gayadi.android.domain.model.AuthSession
import com.gayadi.android.domain.model.AuthUser
import com.gayadi.android.domain.repository.AuthRepository
import kotlinx.coroutines.CancellationException
import org.json.JSONObject

class RemoteAuthRepository(
    private val httpClient: GayadiHttpClient,
    private val tokenStore: TokenStore,
) : AuthRepository {
    override suspend fun signup(
        email: String,
        password: String,
        nickname: String,
    ): Result<AuthSession> = remoteResult {
        authenticate(
            path = "/api/v1/auth/registrations",
            body = JSONObject()
                .put("email", email)
                .put("password", password)
                .put("nickname", nickname),
        )
    }

    override suspend fun login(email: String, password: String): Result<AuthSession> = remoteResult {
        authenticate(
            path = "/api/v1/auth/tokens",
            body = JSONObject()
                .put("email", email)
                .put("password", password),
        )
    }

    override suspend fun current(): Result<AuthUser> = remoteResult {
        httpClient.getObject("/api/v1/users/current").toAuthUser()
    }

    override suspend fun logout(): Result<Unit> = remoteResult {
        tokenStore.clearAccessToken()
    }

    override suspend fun withdraw(): Result<Unit> = remoteResult {
        httpClient.delete("/api/v1/users/current")
        tokenStore.clearAccessToken()
    }

    private suspend fun authenticate(path: String, body: JSONObject): AuthSession {
        val session = httpClient.postObject(path, body, authenticated = false).toAuthSession()
        tokenStore.writeAccessToken(session.accessToken)
        return session
    }

    private suspend fun <T> remoteResult(block: suspend () -> T): Result<T> = try {
        Result.success(block())
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (exception: Exception) {
        Result.failure(exception)
    }
}

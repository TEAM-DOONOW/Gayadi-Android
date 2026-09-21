package com.gayadi.android.data.datasource

import com.gayadi.android.domain.model.AuthSession
import com.gayadi.android.domain.model.AuthUser
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

interface AuthApiDataSource {
    suspend fun exchangeGoogleIdToken(idToken: String): AuthSession
    suspend fun refreshToken(refreshToken: String): AuthSession
}

class HttpAuthApiDataSource(
    baseUrl: String,
    private val connectionFactory: (URL) -> HttpURLConnection = { url ->
        url.openConnection() as HttpURLConnection
    },
) : AuthApiDataSource {
    private val normalizedBaseUrl = baseUrl.trimEnd('/')

    override suspend fun exchangeGoogleIdToken(idToken: String): AuthSession =
        postTokenRequest("/api/v1/auth/google-tokens", "idToken", idToken)

    override suspend fun refreshToken(refreshToken: String): AuthSession =
        postTokenRequest("/api/v1/auth/token-refreshes", "refreshToken", refreshToken)

    private suspend fun postTokenRequest(path: String, field: String, token: String): AuthSession =
        withContext(Dispatchers.IO) {
            require(token.isNotBlank()) { "인증 토큰이 비어 있습니다." }
            val connection = connectionFactory(
                URL("$normalizedBaseUrl$path"),
            ).apply {
                requestMethod = "POST"
                connectTimeout = CONNECT_TIMEOUT_MILLIS
                readTimeout = READ_TIMEOUT_MILLIS
                doOutput = true
                setRequestProperty("Accept", "application/json")
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            }

            try {
                val requestBody = JSONObject().put(field, token).toString()
                connection.outputStream.bufferedWriter(Charsets.UTF_8).use { writer ->
                    writer.write(requestBody)
                }

                val statusCode = connection.responseCode
                val responseBody = if (statusCode in 200..299) {
                    connection.inputStream.bufferedReader().use { it.readText() }
                } else {
                    connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                }
                if (statusCode !in 200..299) {
                    val error = parseError(statusCode, responseBody)
                    throw AuthTokenRequestException(
                        statusCode = statusCode,
                        errorCode = error.code,
                        serverMessage = error.message,
                    )
                }
                parseSession(responseBody)
            } finally {
                connection.disconnect()
            }
        }

    internal fun parseSession(body: String): AuthSession {
        val root = JSONObject(body)
        val payload = root.optJSONObject("data") ?: root
        val user = payload.getJSONObject("user")
        return AuthSession(
            accessToken = payload.getString("accessToken"),
            tokenType = payload.optString("tokenType", "Bearer"),
            expiresInSeconds = payload.optLong("expiresIn"),
            refreshToken = payload.getString("refreshToken"),
            refreshExpiresInSeconds = payload.getLong("refreshExpiresIn"),
            issuedAtEpochSeconds = System.currentTimeMillis() / 1000,
            user = AuthUser(
                id = user.getLong("id"),
                nickname = user.optString("nickname").takeIf(String::isNotBlank),
                email = user.optString("email"),
                introduction = user.optNullableString("introduction"),
                profileImageUrl = user.optNullableString("profileImageUrl"),
                status = user.optNullableString("status"),
                lastLoginAt = user.optNullableString("lastLoginAt"),
                createdAt = user.optNullableString("createdAt"),
                updatedAt = user.optNullableString("updatedAt"),
            ),
        )
    }

    private fun parseError(statusCode: Int, body: String): AuthTokenError {
        val parsed = runCatching {
            val root = JSONObject(body)
            val code = root.optString("code").trim()
            val message = root.optString("message").ifBlank { root.optString("error") }
            AuthTokenError(code.takeIf(String::isNotBlank), message.trim().takeIf(String::isNotBlank))
        }.getOrDefault(AuthTokenError(null, null))
        return parsed.copy(
            message = parsed.message ?: "인증 요청을 처리하지 못했습니다. (HTTP $statusCode)",
        )
    }

    private companion object {
        const val CONNECT_TIMEOUT_MILLIS = 10_000
        const val READ_TIMEOUT_MILLIS = 15_000
    }
}

class AuthTokenRequestException(
    val statusCode: Int,
    val errorCode: String?,
    val serverMessage: String?,
) : IOException(
    if (statusCode == 400 || statusCode == 401 || errorCode == "AUTH_REFRESH_TOKEN_INVALID") {
        "로그인이 만료되었어요. 다시 로그인해 주세요."
    } else {
        "인증 서버 요청을 처리하지 못했어요. 잠시 후 다시 시도해 주세요."
    },
)

private data class AuthTokenError(
    val code: String?,
    val message: String?,
)

private fun JSONObject.optNullableString(name: String): String? =
    takeUnless { isNull(name) }?.optString(name)?.takeIf(String::isNotBlank)

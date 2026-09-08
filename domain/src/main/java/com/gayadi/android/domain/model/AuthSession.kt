package com.gayadi.android.domain.model

data class AuthSession(
    val accessToken: String,
    val tokenType: String,
    val expiresInSeconds: Long,
    val refreshToken: String,
    val refreshExpiresInSeconds: Long,
    val issuedAtEpochSeconds: Long,
    val user: AuthUser,
) {
    fun isAccessTokenExpiring(nowEpochSeconds: Long, leewaySeconds: Long = 60): Boolean =
        nowEpochSeconds >= issuedAtEpochSeconds + expiresInSeconds - leewaySeconds

    fun isRefreshTokenExpired(nowEpochSeconds: Long): Boolean =
        nowEpochSeconds >= issuedAtEpochSeconds + refreshExpiresInSeconds
}

data class AuthUser(
    val id: Long,
    val nickname: String?,
    val email: String,
    val introduction: String? = null,
    val profileImageUrl: String? = null,
    val status: String? = null,
    val lastLoginAt: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
)

package com.gayadi.android.domain.model

/** Authentication state returned after a successful registration or login. */
data class AuthSession(
    val accessToken: String,
    val tokenType: String,
    val expiresInSeconds: Long,
    val user: AuthUser,
)

/** Server-backed identity and profile fields for the signed-in user. */
data class AuthUser(
    val id: Long,
    val email: String,
    val nickname: String,
    val introduction: String? = null,
    val profileImageUrl: String? = null,
    val status: String? = null,
    val resultCode: String? = null,
    val travelStyleName: String? = null,
    val characterKey: String? = null,
    val strengths: List<String> = emptyList(),
    val weaknesses: List<String> = emptyList(),
)

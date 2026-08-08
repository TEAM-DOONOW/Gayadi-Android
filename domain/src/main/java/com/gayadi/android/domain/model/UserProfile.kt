package com.gayadi.android.domain.model

/** User profile shared by onboarding, the home screen, and My Page. */
data class UserProfile(
    val nickname: String,
    val introduction: String,
    val resultCode: String? = null,
    val travelStyleName: String? = null,
    val characterKey: String? = null,
    val strengths: List<String> = emptyList(),
    val weaknesses: List<String> = emptyList(),
)

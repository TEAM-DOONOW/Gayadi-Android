package com.gayadi.android.data.model

/** Local representation of the complete user profile. */
data class UserProfileEntity(
    val nickname: String,
    val introduction: String,
    val resultCode: String? = null,
    val travelStyleName: String? = null,
    val characterKey: String? = null,
    val strengths: List<String> = emptyList(),
    val weaknesses: List<String> = emptyList(),
)

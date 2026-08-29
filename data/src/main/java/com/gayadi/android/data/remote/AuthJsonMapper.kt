package com.gayadi.android.data.remote

import com.gayadi.android.domain.model.AuthSession
import com.gayadi.android.domain.model.AuthUser
import com.gayadi.android.domain.model.UserProfile
import org.json.JSONArray
import org.json.JSONObject

internal fun JSONObject.toAuthSession(): AuthSession = AuthSession(
    accessToken = getString("accessToken"),
    tokenType = optNullableString("tokenType") ?: "Bearer",
    expiresInSeconds = getLong("expiresIn"),
    user = getJSONObject("user").toAuthUser(),
)

internal fun JSONObject.toAuthUser(): AuthUser = AuthUser(
    id = getLong("id"),
    email = optNullableString("email").orEmpty(),
    nickname = getString("nickname"),
    introduction = optNullableString("introduction"),
    profileImageUrl = optNullableString("profileImageUrl")
        ?: optNullableString("profile_image_url"),
    status = optNullableString("status"),
    resultCode = optNullableString("resultCode"),
    travelStyleName = optNullableString("travelStyleName"),
    characterKey = optNullableString("characterKey"),
    strengths = optStringList("strengths"),
    weaknesses = optStringList("weaknesses"),
)

internal fun AuthUser.toUserProfile(): UserProfile = UserProfile(
    nickname = nickname,
    introduction = introduction.orEmpty(),
    resultCode = resultCode,
    travelStyleName = travelStyleName,
    characterKey = characterKey,
    strengths = strengths,
    weaknesses = weaknesses,
)

private fun JSONObject.optNullableString(name: String): String? =
    takeIf { has(name) && !isNull(name) }
        ?.optString(name)
        ?.takeIf(String::isNotBlank)

private fun JSONObject.optStringList(name: String): List<String> =
    optJSONArray(name)?.toStringList().orEmpty()

private fun JSONArray.toStringList(): List<String> = buildList(length()) {
    repeat(length()) { index ->
        if (!isNull(index)) add(getString(index))
    }
}

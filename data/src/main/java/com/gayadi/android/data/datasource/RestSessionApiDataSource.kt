package com.gayadi.android.data.datasource

import org.json.JSONObject

class RestSessionApiDataSource(private val api: GayadiApiClient) {
    suspend fun logout(refreshToken: String) {
        require(refreshToken.isNotBlank()) { "로그인 세션이 없어요." }
        // Swagger defines logout by refresh token, so an expired access token must not block it.
        api.request("DELETE", "/api/v1/auth/sessions/current",
            JSONObject().put("refreshToken", refreshToken).toString(), authenticated = false)
    }
}

package com.gayadi.android.data.datasource

import com.gayadi.android.domain.model.BasicInfo
import com.gayadi.android.domain.model.UserProfile
import org.json.JSONObject

interface ProfileApiDataSource {
    suspend fun updateCurrentUser(basicInfo: BasicInfo)
    suspend fun currentUser(): UserProfile
    suspend fun deleteCurrentUser()
}

class HttpProfileApiDataSource(private val api: GayadiApiClient) : ProfileApiDataSource {
    override suspend fun updateCurrentUser(basicInfo: BasicInfo) {
        api.request("PATCH", PATH, JSONObject()
            .put("nickname", basicInfo.nickname)
            .put("introduction", basicInfo.introduction).toString())
    }

    override suspend fun currentUser(): UserProfile {
        val json = JSONObject(api.request("GET", PATH))
        fun optional(name: String): String? = if (json.isNull(name)) null else
            json.optString(name).takeIf(String::isNotBlank)
        fun strings(name: String): List<String> = json.optJSONArray(name)?.let { values ->
            (0 until values.length()).map { values.getString(it) }
        }.orEmpty()
        return UserProfile(
            nickname = json.getString("nickname"),
            introduction = optional("introduction").orEmpty(),
            resultCode = optional("resultCode"),
            travelStyleName = optional("travelStyleName"),
            characterKey = optional("characterKey"),
            strengths = strings("strengths"),
            weaknesses = strings("weaknesses"),
        )
    }

    override suspend fun deleteCurrentUser() { api.request("DELETE", PATH) }
    private companion object { const val PATH = "/api/v1/users/current" }
}

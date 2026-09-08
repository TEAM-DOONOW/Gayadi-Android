package com.gayadi.android.data.datasource

import com.gayadi.android.domain.model.BasicInfo
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

interface ProfileApiDataSource {
    suspend fun updateCurrentUser(accessToken: String, basicInfo: BasicInfo)
}

class HttpProfileApiDataSource(
    baseUrl: String,
) : ProfileApiDataSource {
    private val normalizedBaseUrl = baseUrl.trimEnd('/')
    private val client = OkHttpClient()

    override suspend fun updateCurrentUser(accessToken: String, basicInfo: BasicInfo) =
        withContext(Dispatchers.IO) {
            require(accessToken.isNotBlank()) { "로그인이 필요합니다." }
            val body = JSONObject()
                .put("nickname", basicInfo.nickname)
                .put("introduction", basicInfo.introduction)
                .toString()
                .toRequestBody(JSON_MEDIA_TYPE)
            val request = Request.Builder()
                .url("$normalizedBaseUrl/api/v1/users/current")
                .header("Authorization", "Bearer $accessToken")
                .header("Accept", "application/json")
                .patch(body)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val responseBody = response.body?.string().orEmpty()
                    throw ProfileApiException(response.code, errorMessage(response.code, responseBody))
                }
            }
        }

    private fun errorMessage(statusCode: Int, body: String): String {
        val serverMessage = runCatching {
            JSONObject(body).optString("message")
        }.getOrDefault("")
        return serverMessage.ifBlank { "프로필을 수정하지 못했습니다. (HTTP $statusCode)" }
    }

    private companion object {
        val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}

class ProfileApiException(
    val statusCode: Int,
    message: String,
) : IOException(message)

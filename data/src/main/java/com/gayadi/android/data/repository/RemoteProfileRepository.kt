package com.gayadi.android.data.repository

import com.gayadi.android.data.remote.GayadiHttpClient
import com.gayadi.android.data.remote.TokenStore
import com.gayadi.android.data.remote.toAuthUser
import com.gayadi.android.data.remote.toUserProfile
import com.gayadi.android.domain.model.BasicInfo
import com.gayadi.android.domain.model.SurveyResult
import com.gayadi.android.domain.model.UserProfile
import com.gayadi.android.domain.repository.ProfileRepository
import kotlinx.coroutines.CancellationException
import org.json.JSONObject

class RemoteProfileRepository(
    private val httpClient: GayadiHttpClient,
    private val tokenStore: TokenStore,
) : ProfileRepository {
    override suspend fun saveBasicInfo(basicInfo: BasicInfo) {
        httpClient.patchObject(
            path = "/api/v1/users/current",
            body = JSONObject()
                .put("nickname", basicInfo.nickname)
                .put("introduction", basicInfo.introduction),
        )
    }

    override suspend fun getBasicInfo(): BasicInfo = fetchProfile().let { profile ->
        BasicInfo(profile.nickname, profile.introduction)
    }

    override suspend fun saveSurveyResult(result: SurveyResult): Result<Unit> = remoteResult {
        val current = fetchProfile()
        check(current.resultCode == result.code) {
            "설문 결과가 서버에 반영되지 않았습니다. 설문 제출 API를 먼저 호출해 주세요."
        }
    }

    override suspend fun getProfile(): UserProfile = fetchProfile()

    override suspend fun clearProfile(): Result<Unit> = remoteResult {
        httpClient.delete("/api/v1/users/current")
        tokenStore.clearAccessToken()
    }

    private suspend fun fetchProfile(): UserProfile =
        httpClient.getObject("/api/v1/users/current").toAuthUser().toUserProfile()

    private suspend fun <T> remoteResult(block: suspend () -> T): Result<T> = try {
        Result.success(block())
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (exception: Exception) {
        Result.failure(exception)
    }
}

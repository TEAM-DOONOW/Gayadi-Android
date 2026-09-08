package com.gayadi.android.data.repository

import com.gayadi.android.data.datasource.ProfileApiDataSource
import com.gayadi.android.data.datasource.ProfileApiException
import com.gayadi.android.domain.model.BasicInfo
import com.gayadi.android.domain.model.SurveyResult
import com.gayadi.android.domain.model.UserProfile
import com.gayadi.android.domain.repository.AuthRepository
import com.gayadi.android.domain.repository.ProfileRepository

class AuthenticatedProfileRepository(
    private val localRepository: ProfileRepository,
    private val apiDataSource: ProfileApiDataSource,
    private val authRepository: AuthRepository,
) : ProfileRepository {
    override suspend fun saveBasicInfo(basicInfo: BasicInfo) {
        try {
            apiDataSource.updateCurrentUser(authRepository.validAccessToken(), basicInfo)
        } catch (error: ProfileApiException) {
            if (error.statusCode != 401) throw error
            val refreshed = authRepository.refreshSession()
            apiDataSource.updateCurrentUser(refreshed.accessToken, basicInfo)
        }
        localRepository.saveBasicInfo(basicInfo)
    }

    override suspend fun getBasicInfo(): BasicInfo? = localRepository.getBasicInfo()

    override suspend fun saveSurveyResult(result: SurveyResult): Result<Unit> =
        localRepository.saveSurveyResult(result)

    override suspend fun getProfile(): UserProfile? = localRepository.getProfile()

    override suspend fun clearProfile(): Result<Unit> = localRepository.clearProfile()
}

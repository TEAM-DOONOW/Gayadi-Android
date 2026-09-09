package com.gayadi.android.data.repository

import com.gayadi.android.data.datasource.ProfileApiDataSource
import com.gayadi.android.data.datasource.apiResult
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
        apiDataSource.updateCurrentUser(basicInfo)
        localRepository.saveBasicInfo(basicInfo)
    }

    override suspend fun getBasicInfo(): BasicInfo? =
        getProfile()?.let { BasicInfo(it.nickname, it.introduction) }

    override suspend fun saveSurveyResult(result: SurveyResult): Result<Unit> = apiResult {
        // The submission endpoint already saved the result. Cache it only after a fresh profile
        // read so an older account's local profile cannot be reused after switching accounts.
        val profile = requireNotNull(getProfile()) { "로그인이 필요해요." }
        localRepository.saveBasicInfo(BasicInfo(profile.nickname, profile.introduction))
        localRepository.saveSurveyResult(result).getOrThrow()
    }

    override suspend fun getProfile(): UserProfile? =
        if (authRepository.currentSession() == null) null else apiDataSource.currentUser()

    override suspend fun clearProfile(): Result<Unit> = apiResult {
        apiDataSource.deleteCurrentUser()
        authRepository.clearSession()
        localRepository.clearProfile().getOrThrow()
    }
}

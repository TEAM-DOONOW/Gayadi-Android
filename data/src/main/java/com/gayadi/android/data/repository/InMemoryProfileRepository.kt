package com.gayadi.android.data.repository

import com.gayadi.android.domain.model.BasicInfo
import com.gayadi.android.domain.model.SurveyResult
import com.gayadi.android.domain.model.UserProfile
import com.gayadi.android.domain.repository.ProfileRepository
import com.gayadi.android.data.datasource.ProfileLocalDataSource
import com.gayadi.android.data.mapper.toDomain
import com.gayadi.android.data.mapper.toEntity

/** Profile repository backed by a local data source. */
class InMemoryProfileRepository(
    private val localDataSource: ProfileLocalDataSource,
) : ProfileRepository {

    override fun saveBasicInfo(basicInfo: BasicInfo) {
        val current = localDataSource.getProfile()?.toDomain()
        localDataSource.saveProfile(
            UserProfile(
                nickname = basicInfo.nickname,
                introduction = basicInfo.introduction,
                resultCode = current?.resultCode,
                travelStyleName = current?.travelStyleName,
                characterKey = current?.characterKey,
                strengths = current?.strengths.orEmpty(),
                weaknesses = current?.weaknesses.orEmpty(),
            ).toEntity(),
        )
    }

    override fun getBasicInfo(): BasicInfo? = getProfile()?.let {
        BasicInfo(nickname = it.nickname, introduction = it.introduction)
    }

    override fun saveSurveyResult(result: SurveyResult) {
        val current = getProfile() ?: return
        localDataSource.saveProfile(
            current.copy(
                resultCode = result.code,
                travelStyleName = result.name,
                characterKey = result.characterKey,
                strengths = result.strengths,
                weaknesses = result.weaknesses,
            ).toEntity(),
        )
    }

    override fun getProfile(): UserProfile? = localDataSource.getProfile()?.toDomain()

    override fun clearProfile(): Result<Unit> = runCatching(localDataSource::clearProfile)
}

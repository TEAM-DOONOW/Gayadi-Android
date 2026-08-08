package com.gayadi.android.data.repository

import com.gayadi.android.domain.model.BasicInfo
import com.gayadi.android.domain.model.SurveyResult
import com.gayadi.android.domain.model.UserProfile
import com.gayadi.android.domain.repository.ProfileRepository
import com.gayadi.android.data.datasource.ProfileLocalDataSource
import com.gayadi.android.data.mapper.toDomain
import com.gayadi.android.data.mapper.toEntity
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Profile repository backed by a local data source. */
class InMemoryProfileRepository(
    private val localDataSource: ProfileLocalDataSource,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ProfileRepository {

    override suspend fun saveBasicInfo(basicInfo: BasicInfo) = withContext(ioDispatcher) {
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

    override suspend fun getBasicInfo(): BasicInfo? = withContext(ioDispatcher) {
        localDataSource.getProfile()?.toDomain()?.let {
            BasicInfo(nickname = it.nickname, introduction = it.introduction)
        }
    }

    override suspend fun saveSurveyResult(result: SurveyResult): Result<Unit> = withContext(ioDispatcher) {
        runCatching {
            val current = localDataSource.getProfile()?.toDomain() ?: error("기본 프로필이 없습니다.")
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
    }

    override suspend fun getProfile(): UserProfile? = withContext(ioDispatcher) {
        localDataSource.getProfile()?.toDomain()
    }

    override suspend fun clearProfile(): Result<Unit> = withContext(ioDispatcher) {
        runCatching(localDataSource::clearProfile)
    }
}

package com.gayadi.android.data.repository

import com.gayadi.android.domain.model.BasicInfo
import com.gayadi.android.domain.repository.ProfileRepository
import com.gayadi.android.data.datasource.ProfileLocalDataSource
import com.gayadi.android.data.mapper.toDomain
import com.gayadi.android.data.mapper.toEntity

/** Profile repository backed by a local data source. */
class InMemoryProfileRepository(
    private val localDataSource: ProfileLocalDataSource,
) : ProfileRepository {

    override fun saveBasicInfo(basicInfo: BasicInfo) {
        localDataSource.saveBasicInfo(basicInfo.toEntity())
    }

    override fun getBasicInfo(): BasicInfo? = localDataSource.getBasicInfo()?.toDomain()
}

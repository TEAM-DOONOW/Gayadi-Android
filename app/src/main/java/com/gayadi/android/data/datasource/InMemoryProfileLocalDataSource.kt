package com.gayadi.android.data.datasource

import com.gayadi.android.data.model.BasicInfoEntity

/** In-memory profile data source used until persistent storage is connected. */
class InMemoryProfileLocalDataSource : ProfileLocalDataSource {
    private var basicInfo: BasicInfoEntity? = null

    /** Stores the profile for the current app process. */
    override fun saveBasicInfo(basicInfo: BasicInfoEntity) {
        this.basicInfo = basicInfo
    }

    /** Returns the profile stored in the current app process. */
    override fun getBasicInfo(): BasicInfoEntity? = basicInfo
}

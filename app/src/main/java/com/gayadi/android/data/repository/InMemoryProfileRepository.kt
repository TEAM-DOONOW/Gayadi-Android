package com.gayadi.android.data.repository

import com.gayadi.android.domain.model.BasicInfo
import com.gayadi.android.domain.repository.ProfileRepository

class InMemoryProfileRepository : ProfileRepository {
    private var basicInfo: BasicInfo? = null

    override fun saveBasicInfo(basicInfo: BasicInfo) {
        this.basicInfo = basicInfo
    }

    override fun getBasicInfo(): BasicInfo? = basicInfo
}

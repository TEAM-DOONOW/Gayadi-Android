package com.gayadi.android.domain.repository

import com.gayadi.android.domain.model.BasicInfo

interface ProfileRepository {
    fun saveBasicInfo(basicInfo: BasicInfo)

    fun getBasicInfo(): BasicInfo?
}

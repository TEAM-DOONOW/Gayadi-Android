package com.gayadi.android.domain.repository

import com.gayadi.android.domain.model.BasicInfo

/** Domain contract for basic profile persistence. */
interface ProfileRepository {
    /** Persists the supplied basic information. */
    fun saveBasicInfo(basicInfo: BasicInfo)

    /** Returns saved basic information when available. */
    fun getBasicInfo(): BasicInfo?
}

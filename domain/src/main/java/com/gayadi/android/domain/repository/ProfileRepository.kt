package com.gayadi.android.domain.repository

import com.gayadi.android.domain.model.BasicInfo
import com.gayadi.android.domain.model.SurveyResult
import com.gayadi.android.domain.model.UserProfile

/** Domain contract for basic profile persistence. */
interface ProfileRepository {
    /** Persists the supplied basic information. */
    fun saveBasicInfo(basicInfo: BasicInfo)

    /** Returns saved basic information when available. */
    fun getBasicInfo(): BasicInfo?

    /** Merges the completed survey result into the saved profile. */
    fun saveSurveyResult(result: SurveyResult)

    /** Returns the complete locally persisted profile when available. */
    fun getProfile(): UserProfile?
}

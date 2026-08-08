package com.gayadi.android.domain.repository

import com.gayadi.android.domain.model.BasicInfo
import com.gayadi.android.domain.model.SurveyResult
import com.gayadi.android.domain.model.UserProfile

/** Domain contract for basic profile persistence. */
interface ProfileRepository {
    /** Persists the supplied basic information. */
    suspend fun saveBasicInfo(basicInfo: BasicInfo)

    /** Returns saved basic information when available. */
    suspend fun getBasicInfo(): BasicInfo?

    /** Merges the completed survey result into the saved profile. */
    suspend fun saveSurveyResult(result: SurveyResult): Result<Unit>

    /** Returns the complete locally persisted profile when available. */
    suspend fun getProfile(): UserProfile?

    /** Removes all locally persisted profile data. */
    suspend fun clearProfile(): Result<Unit>
}

package com.gayadi.android.data.datasource

import com.gayadi.android.data.model.BasicInfoEntity

/** Defines local persistence operations for basic profile information. */
interface ProfileLocalDataSource {
    /** Saves the supplied profile entity. */
    fun saveBasicInfo(basicInfo: BasicInfoEntity)

    /** Returns the saved profile entity, or null when none exists. */
    fun getBasicInfo(): BasicInfoEntity?
}

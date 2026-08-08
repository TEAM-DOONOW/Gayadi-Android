package com.gayadi.android.data.datasource

import com.gayadi.android.data.model.UserProfileEntity

/** Defines local persistence operations for basic profile information. */
interface ProfileLocalDataSource {
    /** Saves the supplied profile entity. */
    fun saveProfile(profile: UserProfileEntity)

    /** Returns the saved profile entity, or null when none exists. */
    fun getProfile(): UserProfileEntity?
}

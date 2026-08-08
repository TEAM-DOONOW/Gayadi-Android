package com.gayadi.android.data.datasource

import com.gayadi.android.data.model.UserProfileEntity

/** In-memory profile data source used until persistent storage is connected. */
class InMemoryProfileLocalDataSource : ProfileLocalDataSource {
    private var profile: UserProfileEntity? = null

    /** Stores the profile for the current app process. */
    override fun saveProfile(profile: UserProfileEntity) {
        this.profile = profile
    }

    /** Returns the profile stored in the current app process. */
    override fun getProfile(): UserProfileEntity? = profile
}

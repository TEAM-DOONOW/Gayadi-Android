package com.gayadi.android.domain.usecase

import com.gayadi.android.domain.model.UserProfile
import com.gayadi.android.domain.repository.ProfileRepository

/** Reads the complete locally persisted user profile. */
class GetUserProfileUseCase(
    private val profileRepository: ProfileRepository,
) {
    operator fun invoke(): UserProfile? = profileRepository.getProfile()
}

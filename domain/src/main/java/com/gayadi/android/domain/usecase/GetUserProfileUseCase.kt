package com.gayadi.android.domain.usecase

import com.gayadi.android.domain.model.UserProfile
import com.gayadi.android.domain.repository.ProfileRepository

/** Reads the current user profile from the configured repository. */
class GetUserProfileUseCase(
    private val profileRepository: ProfileRepository,
) {
    suspend operator fun invoke(): UserProfile? = profileRepository.getProfile()
}

package com.gayadi.android.domain.usecase

import com.gayadi.android.domain.repository.ProfileRepository

/** Removes the locally persisted user profile after account-deletion confirmation. */
class ClearUserProfileUseCase(
    private val profileRepository: ProfileRepository,
) {
    suspend operator fun invoke(): Result<Unit> = profileRepository.clearProfile()
}

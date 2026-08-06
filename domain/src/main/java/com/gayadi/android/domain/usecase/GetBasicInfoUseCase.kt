package com.gayadi.android.domain.usecase

import com.gayadi.android.domain.model.BasicInfo
import com.gayadi.android.domain.repository.ProfileRepository

/** Reads the saved onboarding profile through the domain repository contract. */
class GetBasicInfoUseCase(
    private val profileRepository: ProfileRepository,
) {
    operator fun invoke(): BasicInfo? = profileRepository.getBasicInfo()
}

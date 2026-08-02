package com.gayadi.android.domain.usecase

import com.gayadi.android.domain.model.BasicInfo
import com.gayadi.android.domain.repository.ProfileRepository

/** Normalizes and persists basic onboarding information. */
class SaveBasicInfoUseCase(
    private val profileRepository: ProfileRepository,
) {
    /** Trims user input and saves it through the profile repository. */
    operator fun invoke(nickname: String, introduction: String) {
        profileRepository.saveBasicInfo(
            BasicInfo(
                nickname = nickname.trim(),
                introduction = introduction.trim(),
            ),
        )
    }
}

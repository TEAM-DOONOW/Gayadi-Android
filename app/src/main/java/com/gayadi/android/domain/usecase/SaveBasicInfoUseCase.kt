package com.gayadi.android.domain.usecase

import com.gayadi.android.domain.model.BasicInfo
import com.gayadi.android.domain.repository.ProfileRepository

class SaveBasicInfoUseCase(
    private val profileRepository: ProfileRepository,
) {
    operator fun invoke(nickname: String, introduction: String) {
        profileRepository.saveBasicInfo(
            BasicInfo(
                nickname = nickname.trim(),
                introduction = introduction.trim(),
            ),
        )
    }
}

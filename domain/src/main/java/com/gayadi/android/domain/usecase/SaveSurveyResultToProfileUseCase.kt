package com.gayadi.android.domain.usecase

import com.gayadi.android.domain.model.SurveyResult
import com.gayadi.android.domain.repository.ProfileRepository

/** Persists the survey result as part of the user's local profile. */
class SaveSurveyResultToProfileUseCase(
    private val profileRepository: ProfileRepository,
) {
    operator fun invoke(result: SurveyResult) = profileRepository.saveSurveyResult(result)
}

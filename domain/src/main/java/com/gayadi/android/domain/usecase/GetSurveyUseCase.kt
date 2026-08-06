package com.gayadi.android.domain.usecase

import com.gayadi.android.domain.model.SurveyDefinition
import com.gayadi.android.domain.repository.SurveyRepository

/** Loads the active travel survey through the domain repository contract. */
class GetSurveyUseCase(
    private val surveyRepository: SurveyRepository,
) {
    operator fun invoke(callback: (Result<SurveyDefinition>) -> Unit) {
        surveyRepository.loadSurvey(callback)
    }
}

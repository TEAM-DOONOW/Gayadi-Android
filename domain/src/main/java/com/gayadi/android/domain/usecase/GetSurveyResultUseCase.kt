package com.gayadi.android.domain.usecase

import com.gayadi.android.domain.model.SurveyResult
import com.gayadi.android.domain.repository.SurveyRepository

/** Loads one travel-style result through the domain repository contract. */
class GetSurveyResultUseCase(
    private val surveyRepository: SurveyRepository,
) {
    operator fun invoke(code: String, callback: (Result<SurveyResult>) -> Unit) {
        surveyRepository.loadResult(code, callback)
    }
}

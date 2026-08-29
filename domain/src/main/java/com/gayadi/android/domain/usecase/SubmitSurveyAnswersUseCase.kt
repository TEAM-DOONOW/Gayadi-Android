package com.gayadi.android.domain.usecase

import com.gayadi.android.domain.model.SurveyResult
import com.gayadi.android.domain.repository.SurveyRepository

/** Sends selected option identifiers to the server for authoritative scoring. */
class SubmitSurveyAnswersUseCase(
    private val surveyRepository: SurveyRepository,
) {
    operator fun invoke(
        answers: Map<String, String>,
        callback: (Result<SurveyResult>) -> Unit,
    ) {
        surveyRepository.submitAnswers(answers, callback)
    }
}

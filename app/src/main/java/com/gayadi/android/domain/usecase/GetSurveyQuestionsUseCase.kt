package com.gayadi.android.domain.usecase

import com.gayadi.android.domain.model.SurveyQuestion
import com.gayadi.android.domain.repository.SurveyRepository

class GetSurveyQuestionsUseCase(
    private val surveyRepository: SurveyRepository,
) {
    operator fun invoke(): List<SurveyQuestion> = surveyRepository.getQuestions()
}

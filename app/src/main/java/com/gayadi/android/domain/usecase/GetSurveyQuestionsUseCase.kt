package com.gayadi.android.domain.usecase

import com.gayadi.android.domain.model.SurveyQuestion
import com.gayadi.android.domain.repository.SurveyRepository

/** Retrieves survey questions through the domain repository contract. */
class GetSurveyQuestionsUseCase(
    private val surveyRepository: SurveyRepository,
) {
    /** Returns the ordered survey questions. */
    operator fun invoke(): List<SurveyQuestion> = surveyRepository.getQuestions()
}

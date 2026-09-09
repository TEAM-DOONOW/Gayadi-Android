package com.gayadi.android.domain.usecase

import com.gayadi.android.domain.model.SurveyDefinition
import com.gayadi.android.domain.repository.SurveySubmissionRepository

class SubmitSurveyUseCase(private val repository: SurveySubmissionRepository) {
    suspend operator fun invoke(definition: SurveyDefinition, answers: Map<String, String>): String {
        val optionIds = definition.questions.associate { question ->
            val option = question.options.single { it.code == answers[question.id] }
            question.id to option.id
        }
        return repository.submit(optionIds)
    }
}

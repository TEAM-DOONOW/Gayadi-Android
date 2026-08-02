package com.gayadi.android.data.repository

import com.gayadi.android.data.datasource.SurveyDataSource
import com.gayadi.android.data.mapper.toDomain
import com.gayadi.android.domain.model.SurveyQuestion
import com.gayadi.android.domain.repository.SurveyRepository

/** Survey repository that maps data-source DTOs into domain models. */
class MockSurveyRepository(
    private val dataSource: SurveyDataSource,
) : SurveyRepository {
    /** Returns mapped survey questions from the configured source. */
    override fun getQuestions(): List<SurveyQuestion> =
        dataSource.getQuestions().map { it.toDomain() }
}

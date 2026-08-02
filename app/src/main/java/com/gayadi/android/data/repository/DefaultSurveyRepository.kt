package com.gayadi.android.data.repository

import com.gayadi.android.data.datasource.SurveyDataSource
import com.gayadi.android.data.mapper.toDomain
import com.gayadi.android.domain.model.SurveyDefinition
import com.gayadi.android.domain.model.SurveyResult
import com.gayadi.android.domain.repository.SurveyRepository

/** Maps survey data-source DTOs into domain models. */
class DefaultSurveyRepository(
    private val dataSource: SurveyDataSource,
) : SurveyRepository {
    override fun loadSurvey(callback: (Result<SurveyDefinition>) -> Unit) {
        dataSource.loadSurvey { result -> callback(result.mapCatching { it.toDomain() }) }
    }

    override fun loadResult(code: String, callback: (Result<SurveyResult>) -> Unit) {
        dataSource.loadResult(code) { result -> callback(result.mapCatching { it.toDomain() }) }
    }
}

package com.gayadi.android.domain.repository

import com.gayadi.android.domain.model.SurveyDefinition
import com.gayadi.android.domain.model.SurveyResult

/** Domain contract for retrieving travel survey questions. */
interface SurveyRepository {
    /** Loads the active survey definition. */
    fun loadSurvey(callback: (Result<SurveyDefinition>) -> Unit)

    /** Loads one result definition by code. */
    fun loadResult(code: String, callback: (Result<SurveyResult>) -> Unit)

    /** Submits question-to-option identifiers and returns the server-scored result. */
    fun submitAnswers(
        answers: Map<String, String>,
        callback: (Result<SurveyResult>) -> Unit,
    ) {
        callback(Result.failure(UnsupportedOperationException("설문 제출을 지원하지 않습니다.")))
    }
}

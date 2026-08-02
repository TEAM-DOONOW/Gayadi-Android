package com.gayadi.android.domain.repository

import com.gayadi.android.domain.model.SurveyDefinition
import com.gayadi.android.domain.model.SurveyResult

/** Domain contract for retrieving travel survey questions. */
interface SurveyRepository {
    /** Loads the active survey definition. */
    fun loadSurvey(callback: (Result<SurveyDefinition>) -> Unit)

    /** Loads one result definition by code. */
    fun loadResult(code: String, callback: (Result<SurveyResult>) -> Unit)
}

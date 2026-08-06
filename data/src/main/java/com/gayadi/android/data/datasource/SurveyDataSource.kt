package com.gayadi.android.data.datasource

import com.gayadi.android.data.model.SurveyDefinitionDto
import com.gayadi.android.data.model.SurveyResultDto

/** Defines the source contract for survey question data. */
interface SurveyDataSource {
    /** Loads the active travel survey aggregate. */
    fun loadSurvey(callback: (Result<SurveyDefinitionDto>) -> Unit)

    /** Loads one travel result by its three-letter code. */
    fun loadResult(code: String, callback: (Result<SurveyResultDto>) -> Unit)
}

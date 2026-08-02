package com.gayadi.android.data.datasource

import com.gayadi.android.data.model.SurveyQuestionDto

/** Defines the source contract for survey question data. */
interface SurveyDataSource {
    /** Returns every question required by the travel survey. */
    fun getQuestions(): List<SurveyQuestionDto>
}

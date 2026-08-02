package com.gayadi.android.domain.repository

import com.gayadi.android.domain.model.SurveyQuestion

/** Domain contract for retrieving travel survey questions. */
interface SurveyRepository {
    /** Returns the ordered survey question set. */
    fun getQuestions(): List<SurveyQuestion>
}

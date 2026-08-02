package com.gayadi.android.domain.repository

import com.gayadi.android.domain.model.SurveyQuestion

interface SurveyRepository {
    fun getQuestions(): List<SurveyQuestion>
}

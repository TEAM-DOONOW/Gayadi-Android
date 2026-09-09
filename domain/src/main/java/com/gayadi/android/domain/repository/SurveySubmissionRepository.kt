package com.gayadi.android.domain.repository

interface SurveySubmissionRepository {
    /** Maps question IDs to option IDs (a/b), never to personality codes (P/N/etc.). */
    suspend fun submit(answers: Map<String, String>): String
}

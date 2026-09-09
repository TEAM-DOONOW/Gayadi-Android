package com.gayadi.android.data.repository

import com.gayadi.android.data.datasource.GayadiApiClient
import com.gayadi.android.domain.repository.SurveySubmissionRepository
import org.json.JSONArray
import org.json.JSONObject

class RestSurveySubmissionRepository(private val api: GayadiApiClient) : SurveySubmissionRepository {
    override suspend fun submit(answers: Map<String, String>): String {
        val body = JSONObject().put("answers", JSONArray(answers.map { (question, option) ->
            JSONObject().put("questionId", question).put("optionId", option)
        }))
        val response = JSONObject(api.request(
            "POST", "/api/v1/surveys/travel-personality-v1/submissions", body.toString(),
        ))
        return response.getString("resultCode").also {
            require(it in setOf("PNA", "PNR", "PCA", "PCR", "SNA", "SNR", "SCA", "SCR")) {
                "설문 결과를 확인하지 못했어요."
            }
        }
    }
}

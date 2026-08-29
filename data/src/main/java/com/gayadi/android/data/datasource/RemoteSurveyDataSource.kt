package com.gayadi.android.data.datasource

import com.gayadi.android.data.model.SurveyAnswerDto
import com.gayadi.android.data.model.SurveyDefinitionDto
import com.gayadi.android.data.model.SurveyResultDto
import com.gayadi.android.data.remote.GayadiHttpClient
import kotlinx.coroutines.CoroutineScope
import org.json.JSONArray
import org.json.JSONObject

interface SurveySubmissionDataSource {
    fun submitAnswers(
        answers: List<SurveyAnswerDto>,
        callback: (Result<SurveyResultDto>) -> Unit,
    )
}

class RemoteSurveyDataSource internal constructor(
    private val client: RemoteJsonClient,
    private val coroutineScope: CoroutineScope,
) : SurveyDataSource, SurveySubmissionDataSource {
    constructor(
        httpClient: GayadiHttpClient,
        coroutineScope: CoroutineScope = defaultRemoteDataSourceScope(),
    ) : this(GayadiRemoteJsonClient(httpClient), coroutineScope)

    override fun loadSurvey(callback: (Result<SurveyDefinitionDto>) -> Unit) {
        coroutineScope.launchRemoteRequest(callback) {
            client.getObject(SURVEY_PATH, authenticated = false).toSurveyDefinitionDto()
        }
    }

    override fun loadResult(code: String, callback: (Result<SurveyResultDto>) -> Unit) {
        coroutineScope.launchRemoteRequest(callback) {
            val normalizedCode = code.trim().uppercase()
            require(normalizedCode.matches(RESULT_CODE_PATTERN)) { "성향 결과 코드가 올바르지 않습니다." }
            client.getObject("$SURVEY_PATH/results/$normalizedCode", authenticated = false).toSurveyResultDto()
        }
    }

    override fun submitAnswers(
        answers: List<SurveyAnswerDto>,
        callback: (Result<SurveyResultDto>) -> Unit,
    ) {
        coroutineScope.launchRemoteRequest(callback) {
            require(answers.isNotEmpty()) { "설문 답변이 없습니다." }
            val response = client.postObject(
                path = "$SURVEY_PATH/submissions",
                body = JSONObject().put(
                    "answers",
                    JSONArray().apply {
                        answers.forEach { answer ->
                            put(
                                JSONObject()
                                    .put("questionId", answer.questionId)
                                    .put("optionId", answer.optionId),
                            )
                        }
                    },
                ),
                authenticated = true,
            )
            response.getJSONObject("result").toSurveyResultDto()
        }
    }

    private companion object {
        const val SURVEY_PATH = "/api/v1/surveys/travel-personality-v1"
        val RESULT_CODE_PATTERN = Regex("[A-Z]{3}")
    }
}

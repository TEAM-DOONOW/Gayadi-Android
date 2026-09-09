package com.gayadi.android.data.datasource

import com.gayadi.android.data.model.SurveyDefinitionDto
import com.gayadi.android.data.model.SurveyResultDto
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** Reads the same survey contract as Swagger, including the server's question IDs. */
class RestSurveyDataSource(
    private val api: GayadiApiClient,
    private val scope: CoroutineScope,
) : SurveyDataSource {
    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()

    override fun loadSurvey(callback: (Result<SurveyDefinitionDto>) -> Unit) {
        scope.launch {
            callback(apiResult {
                requireNotNull(moshi.adapter(SurveyDefinitionDto::class.java).fromJson(
                    api.request("GET", PATH, authenticated = false),
                ))
            })
        }
    }

    override fun loadResult(code: String, callback: (Result<SurveyResultDto>) -> Unit) {
        scope.launch {
            callback(apiResult {
                require(code in setOf("PNA", "PNR", "PCA", "PCR", "SNA", "SNR", "SCA", "SCR")) {
                    "알 수 없는 설문 결과예요."
                }
                requireNotNull(moshi.adapter(SurveyResultDto::class.java).fromJson(
                    api.request("GET", "$PATH/results/$code", authenticated = false),
                ))
            })
        }
    }

    private companion object { const val PATH = "/api/v1/surveys/travel-personality-v1" }
}

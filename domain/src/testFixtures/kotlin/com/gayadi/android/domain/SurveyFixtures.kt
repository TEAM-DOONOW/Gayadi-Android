package com.gayadi.android.domain

import com.gayadi.android.domain.model.SurveyDefinition
import com.gayadi.android.domain.model.SurveyOption
import com.gayadi.android.domain.model.SurveyQuestion
import com.gayadi.android.domain.model.SurveyResult
import com.gayadi.android.domain.repository.SurveyRepository

class FakeSurveyRepository(
    var surveyResult: Result<SurveyDefinition>,
) : SurveyRepository {
    override fun loadSurvey(callback: (Result<SurveyDefinition>) -> Unit) = callback(surveyResult)

    override fun loadResult(code: String, callback: (Result<SurveyResult>) -> Unit) {
        callback(surveyResult.mapCatching { it.results.getValue(code) })
    }
}

fun createSurveyDefinition(): SurveyDefinition {
    val dimensions = listOf(
        Triple("preparation", "P", "S"),
        Triple("place", "N", "C"),
        Triple("energy", "A", "R"),
    )
    val questions = dimensions.flatMapIndexed { dimensionIndex, (dimension, left, right) ->
        (1..3).map { questionIndex ->
            val order = dimensionIndex * 3 + questionIndex
            SurveyQuestion(
                id = "q${order.toString().padStart(2, '0')}",
                order = order,
                dimension = dimension,
                title = "질문 $order",
                options = listOf(
                    SurveyOption("a", "왼쪽", left),
                    SurveyOption("b", "오른쪽", right),
                ),
            )
        }
    }
    val codes = listOf("PNA", "PNR", "PCA", "PCR", "SNA", "SNR", "SCA", "SCR")
    return SurveyDefinition(
        id = "travel-personality-v1",
        title = "여행 성향 판단 설문조사",
        resultCodeOrder = dimensions.map { it.first },
        questions = questions,
        results = codes.associateWith { code ->
            SurveyResult(code, "🐶", code, code, emptyList(), emptyList(), emptyList(), null)
        },
    )
}

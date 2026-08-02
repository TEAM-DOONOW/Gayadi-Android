package com.gayadi.android.domain.usecase

import com.gayadi.android.domain.model.SurveyDefinition

/** Calculates one of the eight three-letter travel-style result codes. */
class CalculateSurveyResultUseCase {
    operator fun invoke(
        definition: SurveyDefinition,
        answers: Map<String, String>,
    ): String {
        require(answers.keys.containsAll(definition.questions.map { it.id })) {
            "모든 설문 문항에 답해야 합니다."
        }

        val winners = definition.resultCodeOrder.map { dimension ->
            val questions = definition.questions.filter { it.dimension == dimension }
            require(questions.isNotEmpty()) { "결과 차원에 문항이 없습니다: $dimension" }

            val counts = questions
                .map { question ->
                    val answer = answers.getValue(question.id)
                    require(question.options.any { it.code == answer }) {
                        "문항 ${question.id}의 선택 코드가 올바르지 않습니다: $answer"
                    }
                    answer
                }
                .groupingBy { it }
                .eachCount()
            val maxCount = counts.values.max()
            val winningCodes = counts.filterValues { it == maxCount }.keys
            require(winningCodes.size == 1) { "결과 차원에서 동점이 발생했습니다: $dimension" }
            winningCodes.single()
        }

        return winners.joinToString("").also { resultCode ->
            require(definition.results.containsKey(resultCode)) {
                "결과 유형을 찾을 수 없습니다: $resultCode"
            }
        }
    }
}

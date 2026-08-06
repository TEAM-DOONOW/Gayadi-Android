package com.gayadi.android.data

import com.gayadi.android.data.datasource.InMemoryProfileLocalDataSource
import com.gayadi.android.data.datasource.SurveyDataSource
import com.gayadi.android.data.model.SurveyDefinitionDto
import com.gayadi.android.data.model.SurveyOptionDto
import com.gayadi.android.data.model.SurveyQuestionDto
import com.gayadi.android.data.model.SurveyResultDto
import com.gayadi.android.data.repository.DefaultSurveyRepository
import com.gayadi.android.data.repository.InMemoryProfileRepository
import com.gayadi.android.domain.model.BasicInfo
import org.junit.Assert.assertEquals
import org.junit.Test

/** Verifies repository delegation and data-to-domain mapping. */
class RepositoryMapperTest {
    @Test
    fun profileRepository_mapsEntityAndDomainModel() {
        val repository = InMemoryProfileRepository(InMemoryProfileLocalDataSource())
        val expected = BasicInfo("가야디", "여행을 좋아해요")

        repository.saveBasicInfo(expected)

        assertEquals(expected, repository.getBasicInfo())
    }

    @Test
    fun surveyRepository_mapsAggregateToDomainModel() {
        val expectedResult = SurveyResultDto("PNA", "⛰️", "플래너", "소개", null, null, null, "character_pna")
        val dataSource = object : SurveyDataSource {
            override fun loadSurvey(callback: (Result<SurveyDefinitionDto>) -> Unit) {
                callback(
                    Result.success(
                        SurveyDefinitionDto(
                            id = "travel-personality-v1",
                            title = "여행 성향 판단 설문조사",
                            resultCodeOrder = listOf("preparation", "place", "energy"),
                            questions = listOf(
                                SurveyQuestionDto(
                                    id = "q01",
                                    order = 1,
                                    dimension = "preparation",
                                    title = "질문",
                                    options = listOf(SurveyOptionDto("a", "선택지", "P")),
                                ),
                            ),
                            results = listOf(expectedResult),
                        ),
                    ),
                )
            }

            override fun loadResult(code: String, callback: (Result<SurveyResultDto>) -> Unit) {
                callback(Result.success(expectedResult))
            }
        }
        val repository = DefaultSurveyRepository(dataSource)
        var questionTitle: String? = null
        var resultName: String? = null
        var resultCharacterKey: String? = null

        repository.loadSurvey { questionTitle = it.getOrThrow().questions.single().title }
        repository.loadResult("PNA") {
            resultName = it.getOrThrow().name
            resultCharacterKey = it.getOrThrow().characterKey
        }

        assertEquals("질문", questionTitle)
        assertEquals("플래너", resultName)
        assertEquals("character_pna", resultCharacterKey)
    }
}

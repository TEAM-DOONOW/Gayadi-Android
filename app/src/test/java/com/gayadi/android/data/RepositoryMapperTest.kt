package com.gayadi.android.data

import com.gayadi.android.data.datasource.InMemoryProfileLocalDataSource
import com.gayadi.android.data.datasource.SurveyDataSource
import com.gayadi.android.data.model.SurveyQuestionDto
import com.gayadi.android.data.repository.InMemoryProfileRepository
import com.gayadi.android.data.repository.MockSurveyRepository
import com.gayadi.android.domain.model.BasicInfo
import org.junit.Assert.assertEquals
import org.junit.Test

/** Verifies repository delegation and data-to-domain mapping. */
class RepositoryMapperTest {
    /** Profile data survives the entity mapping round trip. */
    @Test
    fun profileRepository_mapsEntityAndDomainModel() {
        val repository = InMemoryProfileRepository(InMemoryProfileLocalDataSource())
        val expected = BasicInfo("가야디", "여행을 좋아해요")

        repository.saveBasicInfo(expected)

        assertEquals(expected, repository.getBasicInfo())
    }

    /** Survey DTO fields are mapped to the domain model. */
    @Test
    fun surveyRepository_mapsDtoToDomainModel() {
        val dataSource = object : SurveyDataSource {
            override fun getQuestions() = listOf(
                SurveyQuestionDto(7, "질문", listOf("선택지")),
            )
        }

        val question = MockSurveyRepository(dataSource).getQuestions().single()

        assertEquals(7, question.id)
        assertEquals("질문", question.title)
        assertEquals(listOf("선택지"), question.options)
    }
}

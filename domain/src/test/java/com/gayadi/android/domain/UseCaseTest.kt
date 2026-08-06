package com.gayadi.android.domain

import com.gayadi.android.domain.model.BasicInfo
import com.gayadi.android.domain.model.SurveyDefinition
import com.gayadi.android.domain.repository.ProfileRepository
import com.gayadi.android.domain.usecase.CalculateSurveyResultUseCase
import com.gayadi.android.domain.usecase.GetSurveyUseCase
import com.gayadi.android.domain.usecase.SaveBasicInfoUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

/** Verifies domain use-case behavior independently of data implementations. */
class UseCaseTest {
    @Test
    fun saveBasicInfo_trimsInput() {
        val repository = FakeProfileRepository()

        SaveBasicInfoUseCase(repository)("  가야디 ", " 여행가 ")

        assertEquals(BasicInfo("가야디", "여행가"), repository.saved)
    }

    @Test
    fun getSurvey_returnsRepositoryData() {
        val expected = createSurveyDefinition()
        val useCase = GetSurveyUseCase(FakeSurveyRepository(Result.success(expected)))
        var actual: SurveyDefinition? = null

        useCase { actual = it.getOrThrow() }

        assertEquals(expected, actual)
    }

    @Test
    fun calculateSurveyResult_returnsAllEightCodes() {
        val definition = createSurveyDefinition()
        val useCase = CalculateSurveyResultUseCase()

        definition.results.keys.forEach { expectedCode ->
            val codeByDimension = definition.resultCodeOrder.zip(expectedCode.map(Char::toString)).toMap()
            val answers = definition.questions.associate { question ->
                question.id to codeByDimension.getValue(question.dimension)
            }

            assertEquals(expectedCode, useCase(definition, answers))
        }
    }

    @Test
    fun calculateSurveyResult_rejectsMissingAnswer() {
        val definition = createSurveyDefinition()
        val answers = definition.questions.dropLast(1).associate { it.id to it.options.first().code }

        assertThrows(IllegalArgumentException::class.java) {
            CalculateSurveyResultUseCase()(definition, answers)
        }
    }

    @Test
    fun calculateSurveyResult_rejectsUnknownOptionCode() {
        val definition = createSurveyDefinition()
        val answers = definition.questions.associate { it.id to it.options.first().code }
            .toMutableMap()
            .apply { this[definition.questions.first().id] = "INVALID" }

        assertThrows(IllegalArgumentException::class.java) {
            CalculateSurveyResultUseCase()(definition, answers)
        }
    }
}

private class FakeProfileRepository : ProfileRepository {
    var saved: BasicInfo? = null

    override fun saveBasicInfo(basicInfo: BasicInfo) {
        saved = basicInfo
    }

    override fun getBasicInfo(): BasicInfo? = saved
}

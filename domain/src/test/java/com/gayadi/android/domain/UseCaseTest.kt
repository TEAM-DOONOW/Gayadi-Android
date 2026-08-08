package com.gayadi.android.domain

import com.gayadi.android.domain.model.BasicInfo
import com.gayadi.android.domain.model.SurveyDefinition
import com.gayadi.android.domain.model.SurveyResult
import com.gayadi.android.domain.model.UserProfile
import com.gayadi.android.domain.repository.ProfileRepository
import com.gayadi.android.domain.usecase.CalculateSurveyResultUseCase
import com.gayadi.android.domain.usecase.ClearUserProfileUseCase
import com.gayadi.android.domain.usecase.GetSurveyUseCase
import com.gayadi.android.domain.usecase.GetUserProfileUseCase
import com.gayadi.android.domain.usecase.SaveBasicInfoUseCase
import com.gayadi.android.domain.usecase.SaveSurveyResultToProfileUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
    fun surveyResult_roundTripPreservesAllProfileFields() {
        val repository = FakeProfileRepository()
        SaveBasicInfoUseCase(repository)("가야디", "여행가")
        val result = SurveyResult(
            code = "PNA",
            emoji = "⛰️",
            name = "등반잉",
            summary = "계획형 여행",
            hashtags = listOf("계획"),
            strengths = listOf("준비"),
            weaknesses = listOf("유연성"),
            characterKey = "character_pna",
        )

        SaveSurveyResultToProfileUseCase(repository)(result)

        assertEquals(
            UserProfile("가야디", "여행가", "PNA", "등반잉", "character_pna", listOf("준비"), listOf("유연성")),
            GetUserProfileUseCase(repository)(),
        )
    }

    @Test
    fun clearProfile_removesBasicInfoAndSurveyResult() {
        val repository = FakeProfileRepository()
        SaveBasicInfoUseCase(repository)("가야디", "여행가")
        SaveSurveyResultToProfileUseCase(repository)(
            SurveyResult("PNA", "⛰️", "등반잉", "계획형", emptyList(), emptyList(), emptyList(), "character_pna"),
        )

        ClearUserProfileUseCase(repository)().getOrThrow()

        assertNull(GetUserProfileUseCase(repository)())
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
    var surveyResult: SurveyResult? = null

    override fun saveBasicInfo(basicInfo: BasicInfo) {
        saved = basicInfo
    }

    override fun getBasicInfo(): BasicInfo? = saved
    override fun saveSurveyResult(result: SurveyResult): Result<Unit> {
        surveyResult = result
        return Result.success(Unit)
    }
    override fun getProfile(): UserProfile? = saved?.let {
        UserProfile(
            nickname = it.nickname,
            introduction = it.introduction,
            resultCode = surveyResult?.code,
            travelStyleName = surveyResult?.name,
            characterKey = surveyResult?.characterKey,
            strengths = surveyResult?.strengths.orEmpty(),
            weaknesses = surveyResult?.weaknesses.orEmpty(),
        )
    }
    override fun clearProfile(): Result<Unit> {
        saved = null
        surveyResult = null
        return Result.success(Unit)
    }
}

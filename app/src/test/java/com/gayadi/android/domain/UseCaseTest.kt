package com.gayadi.android.domain

import com.gayadi.android.domain.model.BasicInfo
import com.gayadi.android.domain.model.SurveyQuestion
import com.gayadi.android.domain.repository.ProfileRepository
import com.gayadi.android.domain.repository.SurveyRepository
import com.gayadi.android.domain.usecase.GetSurveyQuestionsUseCase
import com.gayadi.android.domain.usecase.SaveBasicInfoUseCase
import org.junit.Assert.assertEquals
import org.junit.Test

/** Verifies domain use-case behavior independently of data implementations. */
class UseCaseTest {
    /** Basic information is trimmed before persistence. */
    @Test
    fun saveBasicInfo_trimsInput() {
        val repository = FakeProfileRepository()

        SaveBasicInfoUseCase(repository)("  가야디 ", " 여행가 ")

        assertEquals(BasicInfo("가야디", "여행가"), repository.saved)
    }

    /** Survey questions are returned from the repository without UI dependencies. */
    @Test
    fun getSurveyQuestions_returnsRepositoryData() {
        val expected = listOf(SurveyQuestion(1, "질문", listOf("답")))
        val useCase = GetSurveyQuestionsUseCase(object : SurveyRepository {
            override fun getQuestions() = expected
        })

        assertEquals(expected, useCase())
    }
}

private class FakeProfileRepository : ProfileRepository {
    var saved: BasicInfo? = null

    override fun saveBasicInfo(basicInfo: BasicInfo) {
        saved = basicInfo
    }

    override fun getBasicInfo(): BasicInfo? = saved
}

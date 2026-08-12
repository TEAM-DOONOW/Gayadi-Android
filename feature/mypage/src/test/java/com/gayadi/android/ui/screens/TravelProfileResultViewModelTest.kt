package com.gayadi.android.ui.screens

import com.gayadi.android.domain.model.SurveyDefinition
import com.gayadi.android.domain.model.SurveyResult
import com.gayadi.android.domain.repository.SurveyRepository
import com.gayadi.android.domain.usecase.GetSurveyResultUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class TravelProfileResultViewModelTest {
    @Test
    fun loadSuccess_exposesCompleteSavedResult() {
        val expected = surveyResult("PNA")
        val viewModel = TravelProfileResultViewModel(
            resultCode = "PNA",
            getSurveyResult = GetSurveyResultUseCase(FakeResultRepository(Result.success(expected))),
        )

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(expected, viewModel.uiState.value.result)
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun missingResultCode_exposesRecoverableEmptyState() {
        val repository = FakeResultRepository(Result.success(surveyResult("PNA")))
        val viewModel = TravelProfileResultViewModel(
            resultCode = null,
            getSurveyResult = GetSurveyResultUseCase(repository),
        )

        assertFalse(viewModel.uiState.value.isLoading)
        assertNull(viewModel.uiState.value.result)
        assertEquals("저장된 여행 유형 결과가 없어요.", viewModel.uiState.value.errorMessage)
        assertEquals(0, repository.loadCount)
    }

    @Test
    fun retry_recoversFromResultLoadFailure() {
        val repository = FakeResultRepository(Result.failure(IllegalStateException("연결 실패")))
        val viewModel = TravelProfileResultViewModel(
            resultCode = "PNA",
            getSurveyResult = GetSurveyResultUseCase(repository),
        )

        assertEquals("연결 실패", viewModel.uiState.value.errorMessage)

        repository.result = Result.success(surveyResult("PNA"))
        viewModel.retry()

        assertEquals("PNA", viewModel.uiState.value.result?.code)
        assertEquals(2, repository.loadCount)
    }

    private class FakeResultRepository(var result: Result<SurveyResult>) : SurveyRepository {
        var loadCount = 0

        override fun loadSurvey(callback: (Result<SurveyDefinition>) -> Unit) =
            callback(Result.failure(UnsupportedOperationException()))

        override fun loadResult(code: String, callback: (Result<SurveyResult>) -> Unit) {
            loadCount += 1
            callback(result)
        }
    }

    private fun surveyResult(code: String) = SurveyResult(
        code = code,
        emoji = "⛰️",
        name = "정상까지 계획대로, 등반잉",
        summary = "자연 속으로 뛰어드는 여행 스타일",
        hashtags = listOf("# 자연 정복"),
        strengths = listOf("준비를 잘해요."),
        weaknesses = listOf("쉬어가야 해요."),
        characterKey = "character_pna",
    )
}

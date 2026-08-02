package com.gayadi.android.feature.surveyresult

import com.gayadi.android.domain.FakeSurveyRepository
import com.gayadi.android.domain.createSurveyDefinition
import com.gayadi.android.domain.usecase.GetSurveyResultUseCase
import com.gayadi.android.feature.surveyresult.presentation.SurveyResultViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

/** Verifies Firestore-backed result loading and retry behavior. */
class SurveyResultViewModelTest {
    @Test
    fun initialization_loadsRequestedResult() {
        val definition = createSurveyDefinition()
        val viewModel = SurveyResultViewModel(
            "SCA",
            GetSurveyResultUseCase(FakeSurveyRepository(Result.success(definition))),
        )

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals("SCA", viewModel.uiState.value.result?.code)
    }

    @Test
    fun retry_recoversFromFailure() {
        val repository = FakeSurveyRepository(Result.failure(IllegalStateException("network")))
        val viewModel = SurveyResultViewModel("SCA", GetSurveyResultUseCase(repository))

        assertNull(viewModel.uiState.value.result)
        assertEquals("network", viewModel.uiState.value.errorMessage)

        repository.surveyResult = Result.success(createSurveyDefinition())
        viewModel.retry()

        assertEquals("SCA", viewModel.uiState.value.result?.code)
    }
}

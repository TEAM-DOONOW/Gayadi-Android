package com.gayadi.android.feature.survey

import com.gayadi.android.domain.FakeSurveyRepository
import com.gayadi.android.domain.createSurveyDefinition
import com.gayadi.android.domain.usecase.CalculateSurveyResultUseCase
import com.gayadi.android.domain.usecase.GetSurveyUseCase
import com.gayadi.android.feature.survey.presentation.SurveyUiEvent
import com.gayadi.android.feature.survey.presentation.SurveyViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Verifies survey loading, selection, transition, and completion state. */
class SurveyViewModelTest {
    private val definition = createSurveyDefinition()

    @Test
    fun initialization_loadsSurvey() {
        val viewModel = createViewModel()

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(9, viewModel.uiState.value.questions.size)
    }

    @Test
    fun events_startSurveyAndSelectOption() {
        val viewModel = createViewModel()

        viewModel.onEvent(SurveyUiEvent.Start)
        viewModel.onEvent(SurveyUiEvent.OptionSelected(1))

        assertTrue(viewModel.uiState.value.hasStarted)
        assertEquals(1, viewModel.uiState.value.selectedOption)
        assertEquals("S", viewModel.uiState.value.answers["q01"])
    }

    @Test
    fun nextEvent_advancesAndResetsSelection() {
        val viewModel = createViewModel()
        viewModel.onEvent(SurveyUiEvent.OptionSelected(0))

        assertNull(viewModel.onEvent(SurveyUiEvent.Next))
        assertEquals(1, viewModel.uiState.value.currentIndex)
        assertNull(viewModel.uiState.value.selectedOption)
    }

    @Test
    fun nextEvent_calculatesFinalResult() {
        val viewModel = createViewModel()

        repeat(definition.questions.size) { index ->
            viewModel.onEvent(SurveyUiEvent.OptionSelected(0))
            val result = viewModel.onEvent(SurveyUiEvent.Next)
            if (index < definition.questions.lastIndex) {
                assertNull(result)
            } else {
                assertEquals("PNA", result)
            }
        }
    }

    @Test
    fun invalidEvents_doNotChangeQuestion() {
        val viewModel = createViewModel()

        viewModel.onEvent(SurveyUiEvent.OptionSelected(99))

        assertNull(viewModel.uiState.value.selectedOption)
        assertNull(viewModel.onEvent(SurveyUiEvent.Next))
        assertEquals(0, viewModel.uiState.value.currentIndex)
    }

    @Test
    fun failedLoad_showsErrorAndRetryRecovers() {
        val repository = FakeSurveyRepository(Result.failure(IllegalStateException("network")))
        val viewModel = SurveyViewModel(
            GetSurveyUseCase(repository),
            CalculateSurveyResultUseCase(),
        )

        assertTrue(viewModel.uiState.value.isEmpty)
        assertEquals("network", viewModel.uiState.value.errorMessage)

        repository.surveyResult = Result.success(definition)
        viewModel.onEvent(SurveyUiEvent.Retry)

        assertFalse(viewModel.uiState.value.isEmpty)
        assertEquals(definition, viewModel.uiState.value.definition)
    }

    private fun createViewModel(): SurveyViewModel = SurveyViewModel(
        GetSurveyUseCase(FakeSurveyRepository(Result.success(definition))),
        CalculateSurveyResultUseCase(),
    )
}

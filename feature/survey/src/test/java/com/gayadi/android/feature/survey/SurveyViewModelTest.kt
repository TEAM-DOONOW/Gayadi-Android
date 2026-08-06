package com.gayadi.android.feature.survey

import com.gayadi.android.domain.FakeSurveyRepository
import com.gayadi.android.domain.createSurveyDefinition
import com.gayadi.android.domain.model.SurveyDefinition
import com.gayadi.android.domain.model.SurveyResult
import com.gayadi.android.domain.repository.SurveyRepository
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
    fun finalCalculationFailure_showsRecoverableError() {
        val invalidDefinition = definition.copy(results = emptyMap())
        val viewModel = SurveyViewModel(
            GetSurveyUseCase(FakeSurveyRepository(Result.success(invalidDefinition))),
            CalculateSurveyResultUseCase(),
        )

        repeat(invalidDefinition.questions.size) {
            viewModel.onEvent(SurveyUiEvent.OptionSelected(0))
            assertNull(viewModel.onEvent(SurveyUiEvent.Next))
        }

        assertEquals("결과 유형을 찾을 수 없습니다: PNA", viewModel.uiState.value.resultErrorMessage)
        viewModel.onEvent(SurveyUiEvent.OptionSelected(1))
        assertNull(viewModel.uiState.value.resultErrorMessage)
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

    @Test
    fun retry_ignoresStaleResponseFromPreviousRequest() {
        val repository = DelayedSurveyRepository()
        val viewModel = SurveyViewModel(
            GetSurveyUseCase(repository),
            CalculateSurveyResultUseCase(),
        )

        viewModel.onEvent(SurveyUiEvent.Retry)
        repository.complete(1, Result.success(definition))
        repository.complete(0, Result.failure(IllegalStateException("stale")))

        assertEquals(definition, viewModel.uiState.value.definition)
        assertNull(viewModel.uiState.value.errorMessage)
    }

    private fun createViewModel(): SurveyViewModel = SurveyViewModel(
        GetSurveyUseCase(FakeSurveyRepository(Result.success(definition))),
        CalculateSurveyResultUseCase(),
    )
}

private class DelayedSurveyRepository : SurveyRepository {
    private val callbacks = mutableListOf<(Result<SurveyDefinition>) -> Unit>()

    override fun loadSurvey(callback: (Result<SurveyDefinition>) -> Unit) {
        callbacks += callback
    }

    override fun loadResult(code: String, callback: (Result<SurveyResult>) -> Unit) = Unit

    fun complete(index: Int, result: Result<SurveyDefinition>) {
        callbacks[index](result)
    }
}

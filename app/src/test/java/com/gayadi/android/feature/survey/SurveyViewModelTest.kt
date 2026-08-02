package com.gayadi.android.feature.survey

import com.gayadi.android.domain.model.SurveyQuestion
import com.gayadi.android.domain.repository.SurveyRepository
import com.gayadi.android.domain.usecase.GetSurveyQuestionsUseCase
import com.gayadi.android.feature.survey.presentation.SurveyUiEvent
import com.gayadi.android.feature.survey.presentation.SurveyViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Verifies survey start, selection, transition, and completion state. */
class SurveyViewModelTest {
    private val questions = listOf(
        SurveyQuestion(1, "첫 질문", listOf("A", "B")),
        SurveyQuestion(2, "둘째 질문", listOf("A", "B")),
    )

    /** Start and selection events update visible state. */
    @Test
    fun events_startSurveyAndSelectOption() {
        val viewModel = createViewModel()

        viewModel.onEvent(SurveyUiEvent.Start)
        viewModel.onEvent(SurveyUiEvent.OptionSelected(1))

        assertTrue(viewModel.uiState.value.hasStarted)
        assertEquals(1, viewModel.uiState.value.selectedOption)
    }

    /** Next clears selection and advances exactly one question. */
    @Test
    fun nextEvent_advancesAndResetsSelection() {
        val viewModel = createViewModel()
        viewModel.onEvent(SurveyUiEvent.OptionSelected(0))

        assertFalse(viewModel.onEvent(SurveyUiEvent.Next))
        assertEquals(1, viewModel.uiState.value.currentIndex)
        assertNull(viewModel.uiState.value.selectedOption)
    }

    /** Next reports completion only after answering the last question. */
    @Test
    fun nextEvent_completesLastQuestion() {
        val viewModel = createViewModel()
        viewModel.onEvent(SurveyUiEvent.OptionSelected(0))
        viewModel.onEvent(SurveyUiEvent.Next)
        viewModel.onEvent(SurveyUiEvent.OptionSelected(1))

        assertTrue(viewModel.onEvent(SurveyUiEvent.Next))
    }

    /** Invalid option indexes and unanswered transitions are ignored. */
    @Test
    fun invalidEvents_doNotChangeQuestion() {
        val viewModel = createViewModel()

        viewModel.onEvent(SurveyUiEvent.OptionSelected(99))

        assertNull(viewModel.uiState.value.selectedOption)
        assertFalse(viewModel.onEvent(SurveyUiEvent.Next))
        assertEquals(0, viewModel.uiState.value.currentIndex)
    }

    /** Empty question data blocks start and remains recoverable through retry. */
    @Test
    fun emptyQuestions_blockStartAndRetryDataSource() {
        var availableQuestions: List<SurveyQuestion> = emptyList()
        val viewModel = SurveyViewModel(
            GetSurveyQuestionsUseCase(object : SurveyRepository {
                override fun getQuestions() = availableQuestions
            }),
        )

        viewModel.onEvent(SurveyUiEvent.Start)
        assertTrue(viewModel.uiState.value.isEmpty)
        assertFalse(viewModel.uiState.value.hasStarted)

        availableQuestions = questions
        viewModel.onEvent(SurveyUiEvent.Retry)
        assertFalse(viewModel.uiState.value.isEmpty)
        assertEquals(questions, viewModel.uiState.value.questions)
    }

    private fun createViewModel(): SurveyViewModel = SurveyViewModel(
        GetSurveyQuestionsUseCase(object : SurveyRepository {
            override fun getQuestions() = questions
        }),
    )
}

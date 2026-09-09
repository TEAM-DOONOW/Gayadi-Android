package com.gayadi.android.feature.survey

import com.gayadi.android.domain.FakeSurveyRepository
import com.gayadi.android.domain.createSurveyDefinition
import com.gayadi.android.domain.repository.SurveySubmissionRepository
import com.gayadi.android.domain.usecase.CalculateSurveyResultUseCase
import com.gayadi.android.domain.usecase.GetSurveyUseCase
import com.gayadi.android.domain.usecase.SubmitSurveyUseCase
import com.gayadi.android.feature.survey.presentation.SurveyUiEvent
import com.gayadi.android.feature.survey.presentation.SurveyViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SurveySubmissionViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun cleanup() { Dispatchers.resetMain() }

    @Test fun `waits for server result and ignores duplicate submit taps`() = runTest(dispatcher) {
        val response = CompletableDeferred<String>()
        var requests = 0
        val vm = viewModel(object : SurveySubmissionRepository {
            override suspend fun submit(answers: Map<String, String>): String {
                requests++
                assertEquals(9, answers.size)
                assertTrue(answers.values.all { it == "a" })
                return response.await()
            }
        })
        answerAll(vm)
        assertTrue(vm.uiState.value.isSubmitting)
        assertNull(vm.uiState.value.completedResultCode)
        vm.onEvent(SurveyUiEvent.Next)
        runCurrent()
        assertEquals(1, requests)
        response.complete("SCR")
        runCurrent()
        assertEquals("SCR", vm.uiState.value.completedResultCode)
        assertFalse(vm.uiState.value.isSubmitting)
    }

    @Test fun `failed submission keeps answers for explicit retry`() = runTest(dispatcher) {
        var calls = 0
        val vm = viewModel(object : SurveySubmissionRepository {
            override suspend fun submit(answers: Map<String, String>): String {
                calls++
                if (calls == 1) error("저장 실패")
                return "PNA"
            }
        })
        answerAll(vm)
        runCurrent()
        assertFalse(vm.uiState.value.isSubmitting)
        assertNull(vm.uiState.value.completedResultCode)
        assertEquals("저장 실패", vm.uiState.value.resultErrorMessage)
        assertEquals(9, vm.uiState.value.answers.size)
        vm.onEvent(SurveyUiEvent.Next)
        runCurrent()
        assertEquals("PNA", vm.uiState.value.completedResultCode)
        assertEquals(2, calls)
    }

    private fun answerAll(vm: SurveyViewModel) {
        repeat(9) {
            vm.onEvent(SurveyUiEvent.OptionSelected(0))
            vm.onEvent(SurveyUiEvent.Next)
        }
    }
    private fun viewModel(repository: SurveySubmissionRepository) = SurveyViewModel(
        GetSurveyUseCase(FakeSurveyRepository(Result.success(createSurveyDefinition()))),
        CalculateSurveyResultUseCase(), SubmitSurveyUseCase(repository),
    )
}

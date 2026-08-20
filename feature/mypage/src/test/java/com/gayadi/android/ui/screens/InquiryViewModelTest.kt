package com.gayadi.android.ui.screens

import com.gayadi.android.domain.model.InquiryCategory
import com.gayadi.android.domain.model.InquiryDraft
import com.gayadi.android.domain.repository.InquiryRepository
import com.gayadi.android.domain.usecase.SubmitInquiryUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class InquiryViewModelTest {
    @Test
    fun submitDisabled_untilEveryFieldIsFilled() {
        val viewModel = InquiryViewModel(SubmitInquiryUseCase(RecordingInquiryRepository()))

        assertFalse(viewModel.uiState.value.canSubmit)

        viewModel.updateTitle("일정 메모 기능 제안")
        viewModel.updateMessage("일정마다 메모를 남기고 싶어요")
        assertFalse(viewModel.uiState.value.canSubmit)

        viewModel.updateContactEmail("traveler@example.com")
        assertTrue(viewModel.uiState.value.canSubmit)
    }

    @Test
    fun submitSuccess_marksStateSubmitted() {
        val repository = RecordingInquiryRepository()
        val viewModel = InquiryViewModel(SubmitInquiryUseCase(repository))

        viewModel.updateCategory(InquiryCategory.FEATURE)
        viewModel.updateTitle("일정 메모 기능 제안")
        viewModel.updateMessage("일정마다 메모를 남기고 싶어요")
        viewModel.updateContactEmail("traveler@example.com")
        viewModel.submit()

        assertTrue(viewModel.uiState.value.isSubmitted)
        assertFalse(viewModel.uiState.value.isSubmitting)
        assertNull(viewModel.uiState.value.errorMessage)
        assertEquals(InquiryCategory.FEATURE, repository.submitted?.category)
    }

    @Test
    fun submitFailure_keepsInputAndExposesError() {
        val viewModel = InquiryViewModel(
            SubmitInquiryUseCase(FailingInquiryRepository(IllegalStateException("전송 실패"))),
        )

        viewModel.updateTitle("일정 메모 기능 제안")
        viewModel.updateMessage("일정마다 메모를 남기고 싶어요")
        viewModel.updateContactEmail("traveler@example.com")
        viewModel.submit()

        assertFalse(viewModel.uiState.value.isSubmitted)
        assertEquals("전송 실패", viewModel.uiState.value.errorMessage)
        assertEquals("일정 메모 기능 제안", viewModel.uiState.value.title)
    }

    @Test
    fun submitInvalidEmail_surfacesValidationMessageFromUseCase() {
        val repository = RecordingInquiryRepository()
        val viewModel = InquiryViewModel(SubmitInquiryUseCase(repository))

        viewModel.updateTitle("일정 메모 기능 제안")
        viewModel.updateMessage("일정마다 메모를 남기고 싶어요")
        viewModel.updateContactEmail("gayadi.example.com")
        viewModel.submit()

        assertEquals("답변받을 이메일 주소를 정확히 입력해 주세요", viewModel.uiState.value.errorMessage)
        assertNull(repository.submitted)
    }

    @Test
    fun titleAndMessage_areCappedAtTheirMaximumLength() {
        val viewModel = InquiryViewModel(SubmitInquiryUseCase(RecordingInquiryRepository()))

        viewModel.updateTitle("가".repeat(InquiryViewModel.TITLE_MAX_LENGTH + 20))
        viewModel.updateMessage("나".repeat(InquiryViewModel.MESSAGE_MAX_LENGTH + 20))

        assertEquals(InquiryViewModel.TITLE_MAX_LENGTH, viewModel.uiState.value.title.length)
        assertEquals(InquiryViewModel.MESSAGE_MAX_LENGTH, viewModel.uiState.value.message.length)
    }

    @Test
    fun reset_clearsSubmittedStateForAnotherInquiry() {
        val viewModel = InquiryViewModel(SubmitInquiryUseCase(RecordingInquiryRepository()))

        viewModel.updateTitle("일정 메모 기능 제안")
        viewModel.updateMessage("일정마다 메모를 남기고 싶어요")
        viewModel.updateContactEmail("traveler@example.com")
        viewModel.submit()
        viewModel.reset()

        assertFalse(viewModel.uiState.value.isSubmitted)
        assertEquals("", viewModel.uiState.value.title)
    }

    private class RecordingInquiryRepository : InquiryRepository {
        var submitted: InquiryDraft? = null

        override fun submit(draft: InquiryDraft, callback: (Result<Unit>) -> Unit) {
            submitted = draft
            callback(Result.success(Unit))
        }
    }

    private class FailingInquiryRepository(private val error: Throwable) : InquiryRepository {
        override fun submit(draft: InquiryDraft, callback: (Result<Unit>) -> Unit) =
            callback(Result.failure(error))
    }
}

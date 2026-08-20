package com.gayadi.android.domain

import com.gayadi.android.domain.model.InquiryCategory
import com.gayadi.android.domain.model.InquiryDraft
import com.gayadi.android.domain.repository.InquiryRepository
import com.gayadi.android.domain.usecase.SubmitInquiryUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SubmitInquiryUseCaseTest {
    @Test
    fun submit_trimsInputBeforeSendingToRepository() {
        val repository = RecordingInquiryRepository()
        var actual: Result<Unit>? = null

        SubmitInquiryUseCase(repository)(
            draft(title = "  일정 메모  ", message = "  일정마다 메모를 남기고 싶어요  ", email = " a@b.co "),
        ) { result -> actual = result }

        assertTrue(actual?.isSuccess == true)
        assertEquals("일정 메모", repository.submitted?.title)
        assertEquals("일정마다 메모를 남기고 싶어요", repository.submitted?.message)
        assertEquals("a@b.co", repository.submitted?.contactEmail)
    }

    @Test
    fun submit_rejectsBlankTitleWithoutCallingRepository() {
        val repository = RecordingInquiryRepository()
        var actual: Result<Unit>? = null

        SubmitInquiryUseCase(repository)(draft(title = "   ")) { result -> actual = result }

        assertEquals("문의 제목을 입력해 주세요", actual?.exceptionOrNull()?.message)
        assertNull(repository.submitted)
    }

    @Test
    fun submit_rejectsTooShortMessage() {
        var actual: Result<Unit>? = null

        SubmitInquiryUseCase(RecordingInquiryRepository())(draft(message = "짧아요")) { result -> actual = result }

        assertEquals("문의 내용을 10자 이상 입력해 주세요", actual?.exceptionOrNull()?.message)
    }

    @Test
    fun submit_rejectsMalformedEmail() {
        var actual: Result<Unit>? = null

        SubmitInquiryUseCase(RecordingInquiryRepository())(draft(email = "gayadi.example.com")) { result ->
            actual = result
        }

        assertEquals("답변받을 이메일 주소를 정확히 입력해 주세요", actual?.exceptionOrNull()?.message)
    }

    private class RecordingInquiryRepository : InquiryRepository {
        var submitted: InquiryDraft? = null

        override fun submit(draft: InquiryDraft, callback: (Result<Unit>) -> Unit) {
            submitted = draft
            callback(Result.success(Unit))
        }
    }

    private fun draft(
        title: String = "일정 메모 기능 제안",
        message: String = "일정마다 메모를 남기고 싶어요",
        email: String = "traveler@example.com",
    ) = InquiryDraft(InquiryCategory.FEATURE, title, message, email)
}

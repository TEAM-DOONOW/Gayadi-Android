package com.gayadi.android.domain.usecase

import com.gayadi.android.domain.model.InquiryDraft
import com.gayadi.android.domain.repository.InquiryRepository

class SubmitInquiryUseCase(
    private val repository: InquiryRepository,
) {
    operator fun invoke(draft: InquiryDraft, callback: (Result<Unit>) -> Unit) {
        val trimmed = draft.copy(
            title = draft.title.trim(),
            message = draft.message.trim(),
            contactEmail = draft.contactEmail.trim(),
        )
        validate(trimmed)?.let { message ->
            callback(Result.failure(IllegalArgumentException(message)))
            return
        }
        repository.submit(trimmed, callback)
    }

    private fun validate(draft: InquiryDraft): String? = when {
        draft.title.isBlank() -> "문의 제목을 입력해 주세요"
        draft.title.length > TITLE_MAX_LENGTH -> "제목은 ${TITLE_MAX_LENGTH}자까지 입력할 수 있어요"
        draft.message.length < MESSAGE_MIN_LENGTH -> "문의 내용을 ${MESSAGE_MIN_LENGTH}자 이상 입력해 주세요"
        draft.message.length > MESSAGE_MAX_LENGTH -> "내용은 ${MESSAGE_MAX_LENGTH}자까지 입력할 수 있어요"
        !draft.contactEmail.matches(EMAIL_PATTERN) -> "답변받을 이메일 주소를 정확히 입력해 주세요"
        else -> null
    }

    private companion object {
        const val TITLE_MAX_LENGTH = 50
        const val MESSAGE_MIN_LENGTH = 10
        const val MESSAGE_MAX_LENGTH = 1000
        val EMAIL_PATTERN = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]{2,}$")
    }
}

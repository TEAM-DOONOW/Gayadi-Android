package com.gayadi.android.data.repository

import com.gayadi.android.data.datasource.InquiryDataSource
import com.gayadi.android.data.mapper.toDto
import com.gayadi.android.domain.model.InquiryDraft
import com.gayadi.android.domain.repository.InquiryRepository

class DefaultInquiryRepository(
    private val dataSource: InquiryDataSource,
) : InquiryRepository {
    override fun submit(draft: InquiryDraft, callback: (Result<Unit>) -> Unit) {
        dataSource.submit(draft.toDto()) { result ->
            callback(result.withUserFacingMessage(SUBMIT_FAILURE_MESSAGE))
        }
    }

    private companion object {
        const val SUBMIT_FAILURE_MESSAGE = "문의를 보내지 못했어요. 잠시 후 다시 시도해 주세요."
    }
}

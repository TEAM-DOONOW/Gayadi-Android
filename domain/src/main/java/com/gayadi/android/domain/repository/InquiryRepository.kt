package com.gayadi.android.domain.repository

import com.gayadi.android.domain.model.InquiryDraft

interface InquiryRepository {
    fun submit(draft: InquiryDraft, callback: (Result<Unit>) -> Unit)
}

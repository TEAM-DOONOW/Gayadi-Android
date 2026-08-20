package com.gayadi.android.data.datasource

import com.gayadi.android.data.model.InquiryDto

interface InquiryDataSource {
    fun submit(inquiry: InquiryDto, callback: (Result<Unit>) -> Unit)
}

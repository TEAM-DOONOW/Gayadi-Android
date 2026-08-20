package com.gayadi.android.data.mapper

import com.gayadi.android.data.model.InquiryDto
import com.gayadi.android.domain.model.InquiryDraft

fun InquiryDraft.toDto(): InquiryDto = InquiryDto(
    category = category.id,
    title = title,
    message = message,
    contactEmail = contactEmail,
)

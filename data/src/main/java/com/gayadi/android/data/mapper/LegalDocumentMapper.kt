package com.gayadi.android.data.mapper

import com.gayadi.android.data.model.LegalDocumentDto
import com.gayadi.android.data.model.LegalDocumentSectionDto
import com.gayadi.android.domain.model.LegalDocument
import com.gayadi.android.domain.model.LegalDocumentSection

fun LegalDocumentDto.toDomain(): LegalDocument = LegalDocument(
    id = id,
    title = title,
    version = version,
    effectiveDate = effectiveDate,
    summary = summary,
    sections = sections.map(LegalDocumentSectionDto::toDomain),
    reviewNotice = reviewNotice,
)

private fun LegalDocumentSectionDto.toDomain() = LegalDocumentSection(title, body)

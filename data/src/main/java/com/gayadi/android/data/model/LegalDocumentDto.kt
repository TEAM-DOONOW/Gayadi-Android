package com.gayadi.android.data.model

data class LegalDocumentSectionDto(
    val title: String,
    val body: String,
)

data class LegalDocumentDto(
    val id: String,
    val title: String,
    val version: String,
    val effectiveDate: String,
    val summary: String,
    val sections: List<LegalDocumentSectionDto>,
    val reviewNotice: String?,
)

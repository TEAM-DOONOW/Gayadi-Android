package com.gayadi.android.domain.model

enum class LegalDocumentType(val documentId: String, val displayName: String) {
    TERMS_OF_SERVICE("terms-of-service", "이용약관"),
    PRIVACY_POLICY("privacy-policy", "개인정보처리방침"),
    ;

    companion object {
        fun fromDocumentId(documentId: String): LegalDocumentType? =
            entries.firstOrNull { it.documentId == documentId }
    }
}

data class LegalDocumentSection(
    val title: String,
    val body: String,
)

data class LegalDocument(
    val id: String,
    val title: String,
    val version: String,
    val effectiveDate: String,
    val summary: String,
    val sections: List<LegalDocumentSection>,
    val reviewNotice: String?,
)

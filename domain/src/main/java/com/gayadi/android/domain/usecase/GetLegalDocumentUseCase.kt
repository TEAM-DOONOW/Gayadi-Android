package com.gayadi.android.domain.usecase

import com.gayadi.android.domain.model.LegalDocument
import com.gayadi.android.domain.model.LegalDocumentType
import com.gayadi.android.domain.repository.LegalDocumentRepository

class GetLegalDocumentUseCase(
    private val repository: LegalDocumentRepository,
) {
    operator fun invoke(type: LegalDocumentType, callback: (Result<LegalDocument>) -> Unit) {
        repository.loadDocument(type, callback)
    }
}

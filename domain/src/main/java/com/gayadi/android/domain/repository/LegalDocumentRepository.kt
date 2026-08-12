package com.gayadi.android.domain.repository

import com.gayadi.android.domain.model.LegalDocument
import com.gayadi.android.domain.model.LegalDocumentType

interface LegalDocumentRepository {
    fun loadDocument(type: LegalDocumentType, callback: (Result<LegalDocument>) -> Unit)
}

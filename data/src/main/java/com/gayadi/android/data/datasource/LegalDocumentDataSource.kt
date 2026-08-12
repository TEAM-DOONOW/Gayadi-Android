package com.gayadi.android.data.datasource

import com.gayadi.android.data.model.LegalDocumentDto

interface LegalDocumentDataSource {
    fun loadDocument(documentId: String, callback: (Result<LegalDocumentDto>) -> Unit)
}

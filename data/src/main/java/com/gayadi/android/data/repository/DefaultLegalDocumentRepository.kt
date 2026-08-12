package com.gayadi.android.data.repository

import com.gayadi.android.data.datasource.LegalDocumentDataSource
import com.gayadi.android.data.mapper.toDomain
import com.gayadi.android.domain.model.LegalDocument
import com.gayadi.android.domain.model.LegalDocumentType
import com.gayadi.android.domain.repository.LegalDocumentRepository

class DefaultLegalDocumentRepository(
    private val dataSource: LegalDocumentDataSource,
) : LegalDocumentRepository {
    override fun loadDocument(type: LegalDocumentType, callback: (Result<LegalDocument>) -> Unit) {
        dataSource.loadDocument(type.documentId) { result ->
            callback(result.mapCatching { it.toDomain() })
        }
    }
}

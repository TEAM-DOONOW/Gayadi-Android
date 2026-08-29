package com.gayadi.android.data.datasource

import com.gayadi.android.data.model.LegalDocumentDto
import com.gayadi.android.data.remote.GayadiHttpClient
import kotlinx.coroutines.CoroutineScope

class RemoteLegalDocumentDataSource internal constructor(
    private val client: RemoteJsonClient,
    private val coroutineScope: CoroutineScope,
) : LegalDocumentDataSource {
    constructor(
        httpClient: GayadiHttpClient,
        coroutineScope: CoroutineScope = defaultRemoteDataSourceScope(),
    ) : this(GayadiRemoteJsonClient(httpClient), coroutineScope)

    override fun loadDocument(documentId: String, callback: (Result<LegalDocumentDto>) -> Unit) {
        coroutineScope.launchRemoteRequest(callback) {
            require(documentId.matches(DOCUMENT_ID_PATTERN)) { "법적 문서 식별자가 올바르지 않습니다." }
            client.getObject("$LEGAL_DOCUMENTS_PATH/$documentId", authenticated = false).toLegalDocumentDto()
        }
    }

    private companion object {
        const val LEGAL_DOCUMENTS_PATH = "/api/v1/legal-documents"
        val DOCUMENT_ID_PATTERN = Regex("[a-z0-9-]{1,50}")
    }
}

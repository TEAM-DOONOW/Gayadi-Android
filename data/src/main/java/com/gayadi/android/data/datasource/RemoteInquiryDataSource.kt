package com.gayadi.android.data.datasource

import com.gayadi.android.data.model.InquiryDto
import com.gayadi.android.data.remote.GayadiHttpClient
import kotlinx.coroutines.CoroutineScope
import org.json.JSONObject

class RemoteInquiryDataSource internal constructor(
    private val client: RemoteJsonClient,
    private val coroutineScope: CoroutineScope,
) : InquiryDataSource {
    constructor(
        httpClient: GayadiHttpClient,
        coroutineScope: CoroutineScope = defaultRemoteDataSourceScope(),
    ) : this(GayadiRemoteJsonClient(httpClient), coroutineScope)

    override fun submit(inquiry: InquiryDto, callback: (Result<Unit>) -> Unit) {
        coroutineScope.launchRemoteRequest(callback) {
            client.postObject(
                path = INQUIRIES_PATH,
                body = JSONObject().apply {
                    put("category", inquiry.category)
                    put("title", inquiry.title)
                    put("message", inquiry.message)
                    put("contactEmail", inquiry.contactEmail)
                },
                authenticated = true,
            )
            Unit
        }
    }

    private companion object {
        const val INQUIRIES_PATH = "/api/v1/inquiries"
    }
}

package com.gayadi.android.data.datasource

import com.gayadi.android.data.model.InquiryDto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.json.JSONObject

class RestInquiryDataSource(
    private val api: GayadiApiClient,
    private val scope: CoroutineScope,
) : InquiryDataSource {
    override fun submit(inquiry: InquiryDto, callback: (Result<Unit>) -> Unit) {
        scope.launch {
            callback(apiResult {
                api.request("POST", "/api/v1/inquiries", JSONObject()
                    .put("category", inquiry.category)
                    .put("title", inquiry.title)
                    .put("message", inquiry.message)
                    .put("contactEmail", inquiry.contactEmail).toString())
                Unit
            })
        }
    }
}

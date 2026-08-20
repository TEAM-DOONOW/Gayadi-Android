package com.gayadi.android.data.datasource

import com.gayadi.android.data.model.InquiryDto
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class FirestoreInquiryDataSource(
    private val firestore: FirebaseFirestore,
    private val installationId: String,
    private val appVersion: String,
) : InquiryDataSource {
    override fun submit(inquiry: InquiryDto, callback: (Result<Unit>) -> Unit) {
        firestore.collection(COLLECTION)
            .add(
                mapOf(
                    CATEGORY_FIELD to inquiry.category,
                    TITLE_FIELD to inquiry.title,
                    MESSAGE_FIELD to inquiry.message,
                    CONTACT_EMAIL_FIELD to inquiry.contactEmail,
                    INSTALLATION_ID_FIELD to installationId,
                    APP_VERSION_FIELD to appVersion,
                    STATUS_FIELD to RECEIVED_STATUS,
                    CREATED_AT_FIELD to FieldValue.serverTimestamp(),
                ),
            )
            .addOnSuccessListener { callback(Result.success(Unit)) }
            .addOnFailureListener { callback(Result.failure(it)) }
    }

    private companion object {
        const val COLLECTION = "inquiries"
        const val CATEGORY_FIELD = "category"
        const val TITLE_FIELD = "title"
        const val MESSAGE_FIELD = "message"
        const val CONTACT_EMAIL_FIELD = "contactEmail"
        const val INSTALLATION_ID_FIELD = "installationId"
        const val APP_VERSION_FIELD = "appVersion"
        const val STATUS_FIELD = "status"
        const val CREATED_AT_FIELD = "createdAt"
        const val RECEIVED_STATUS = "RECEIVED"
    }
}

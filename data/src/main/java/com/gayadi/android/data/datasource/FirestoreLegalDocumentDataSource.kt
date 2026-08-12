package com.gayadi.android.data.datasource

import com.gayadi.android.data.model.LegalDocumentDto
import com.gayadi.android.data.model.LegalDocumentSectionDto
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore

class FirestoreLegalDocumentDataSource(
    private val firestore: FirebaseFirestore,
) : LegalDocumentDataSource {
    override fun loadDocument(documentId: String, callback: (Result<LegalDocumentDto>) -> Unit) {
        firestore.collection(COLLECTION).document(documentId).get()
            .addOnSuccessListener { document ->
                callback(runCatching { document.toDto(documentId) })
            }
            .addOnFailureListener { callback(Result.failure(it)) }
    }

    private fun DocumentSnapshot.toDto(expectedId: String): LegalDocumentDto {
        require(exists()) { "법적 문서를 찾을 수 없습니다: $expectedId" }
        val sections = (get(SECTIONS_FIELD) as? List<*>)?.mapIndexed { index, raw ->
            val section = raw as? Map<*, *>
                ?: error("${reference}의 ${index + 1}번째 섹션이 올바르지 않습니다.")
            LegalDocumentSectionDto(
                title = section.requiredString(TITLE_FIELD),
                body = section.requiredString(BODY_FIELD),
            )
        }.orEmpty()
        require(sections.isNotEmpty()) { "${reference}의 본문 섹션이 없습니다." }
        return LegalDocumentDto(
            id = id,
            title = requiredString(TITLE_FIELD),
            version = requiredString(VERSION_FIELD),
            effectiveDate = requiredString(EFFECTIVE_DATE_FIELD),
            summary = requiredString(SUMMARY_FIELD),
            sections = sections,
            reviewNotice = getString(REVIEW_NOTICE_FIELD)?.takeIf(String::isNotBlank),
        )
    }

    private fun DocumentSnapshot.requiredString(field: String): String =
        getString(field)?.takeIf(String::isNotBlank)
            ?: error("${reference}의 $field 값이 없습니다.")

    private fun Map<*, *>.requiredString(field: String): String =
        (get(field) as? String)?.takeIf(String::isNotBlank)
            ?: error("법적 문서 섹션의 $field 값이 없습니다.")

    private companion object {
        const val COLLECTION = "legalDocuments"
        const val TITLE_FIELD = "title"
        const val BODY_FIELD = "body"
        const val VERSION_FIELD = "version"
        const val EFFECTIVE_DATE_FIELD = "effectiveDate"
        const val SUMMARY_FIELD = "summary"
        const val SECTIONS_FIELD = "sections"
        const val REVIEW_NOTICE_FIELD = "reviewNotice"
    }
}

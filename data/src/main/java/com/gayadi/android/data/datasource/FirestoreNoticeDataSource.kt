package com.gayadi.android.data.datasource

import com.gayadi.android.data.model.NoticeDto
import com.gayadi.android.data.model.NoticeSectionDto
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore

class FirestoreNoticeDataSource(
    private val firestore: FirebaseFirestore,
) : NoticeDataSource {
    override fun loadNotices(callback: (Result<List<NoticeDto>>) -> Unit) {
        firestore.collection(COLLECTION).get()
            .addOnSuccessListener { snapshot ->
                callback(runCatching { snapshot.documents.map { it.toDto(it.id) } })
            }
            .addOnFailureListener { callback(Result.failure(it)) }
    }

    override fun loadNotice(noticeId: String, callback: (Result<NoticeDto>) -> Unit) {
        firestore.collection(COLLECTION).document(noticeId).get()
            .addOnSuccessListener { document ->
                callback(runCatching { document.toDto(noticeId) })
            }
            .addOnFailureListener { callback(Result.failure(it)) }
    }

    private fun DocumentSnapshot.toDto(expectedId: String): NoticeDto {
        require(exists()) { "업데이트 소식을 찾을 수 없습니다: $expectedId" }
        val sections = (get(SECTIONS_FIELD) as? List<*>)?.mapIndexed { index, raw ->
            val section = raw as? Map<*, *>
                ?: error("${reference}의 ${index + 1}번째 섹션이 올바르지 않습니다.")
            NoticeSectionDto(
                title = section.requiredString(TITLE_FIELD),
                body = section.requiredString(BODY_FIELD),
            )
        }.orEmpty()
        require(sections.isNotEmpty()) { "${reference}의 본문 섹션이 없습니다." }
        return NoticeDto(
            id = id,
            title = requiredString(TITLE_FIELD),
            category = getString(CATEGORY_FIELD),
            version = getString(VERSION_FIELD)?.takeIf(String::isNotBlank),
            publishedAt = requiredString(PUBLISHED_AT_FIELD),
            summary = requiredString(SUMMARY_FIELD),
            sections = sections,
            isPinned = getBoolean(PINNED_FIELD) ?: false,
        )
    }

    private fun DocumentSnapshot.requiredString(field: String): String =
        getString(field)?.takeIf(String::isNotBlank)
            ?: error("${reference}의 $field 값이 없습니다.")

    private fun Map<*, *>.requiredString(field: String): String =
        (get(field) as? String)?.takeIf(String::isNotBlank)
            ?: error("업데이트 소식 섹션의 $field 값이 없습니다.")

    private companion object {
        const val COLLECTION = "notices"
        const val TITLE_FIELD = "title"
        const val BODY_FIELD = "body"
        const val CATEGORY_FIELD = "category"
        const val VERSION_FIELD = "version"
        const val PUBLISHED_AT_FIELD = "publishedAt"
        const val SUMMARY_FIELD = "summary"
        const val SECTIONS_FIELD = "sections"
        const val PINNED_FIELD = "pinned"
    }
}

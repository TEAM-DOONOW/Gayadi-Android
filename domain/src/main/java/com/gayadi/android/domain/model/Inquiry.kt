package com.gayadi.android.domain.model

enum class InquiryCategory(val id: String, val label: String) {
    BUG("bug", "오류 신고"),
    FEATURE("feature", "기능 제안"),
    ACCOUNT("account", "계정 문의"),
    ETC("etc", "기타"),
    ;

    companion object {
        fun fromId(id: String?): InquiryCategory =
            entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: ETC
    }
}

data class InquiryDraft(
    val category: InquiryCategory,
    val title: String,
    val message: String,
    val contactEmail: String,
)

package com.gayadi.android.domain.model

enum class NoticeCategory(val id: String, val label: String) {
    UPDATE("update", "업데이트"),
    NOTICE("notice", "공지"),
    EVENT("event", "이벤트"),
    ;

    companion object {
        fun fromId(id: String?): NoticeCategory =
            entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: UPDATE
    }
}

data class NoticeSection(
    val title: String,
    val body: String,
)

data class Notice(
    val id: String,
    val title: String,
    val category: NoticeCategory,
    val version: String?,
    val publishedAt: String,
    val summary: String,
    val sections: List<NoticeSection>,
    val isPinned: Boolean,
)

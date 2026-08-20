package com.gayadi.android.data.model

data class NoticeSectionDto(
    val title: String,
    val body: String,
)

data class NoticeDto(
    val id: String,
    val title: String,
    val category: String?,
    val version: String?,
    val publishedAt: String,
    val summary: String,
    val sections: List<NoticeSectionDto>,
    val isPinned: Boolean,
)

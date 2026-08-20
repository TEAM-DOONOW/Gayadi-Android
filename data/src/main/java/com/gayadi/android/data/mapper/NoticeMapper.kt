package com.gayadi.android.data.mapper

import com.gayadi.android.data.model.NoticeDto
import com.gayadi.android.data.model.NoticeSectionDto
import com.gayadi.android.domain.model.Notice
import com.gayadi.android.domain.model.NoticeCategory
import com.gayadi.android.domain.model.NoticeSection

fun NoticeDto.toDomain(): Notice = Notice(
    id = id,
    title = title,
    category = NoticeCategory.fromId(category),
    version = version,
    publishedAt = publishedAt,
    summary = summary,
    sections = sections.map(NoticeSectionDto::toDomain),
    isPinned = isPinned,
)

private fun NoticeSectionDto.toDomain() = NoticeSection(title, body)

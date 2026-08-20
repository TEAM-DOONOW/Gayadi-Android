package com.gayadi.android.domain.usecase

import com.gayadi.android.domain.model.Notice
import com.gayadi.android.domain.repository.NoticeRepository

class GetNoticesUseCase(
    private val repository: NoticeRepository,
) {
    operator fun invoke(callback: (Result<List<Notice>>) -> Unit) {
        repository.loadNotices { result ->
            callback(result.map { notices -> notices.sortedWith(DISPLAY_ORDER) })
        }
    }

    private companion object {
        /** 고정 공지를 먼저, 그다음 최신 발행일 순으로 노출한다. */
        val DISPLAY_ORDER = compareByDescending<Notice> { it.isPinned }
            .thenByDescending { it.publishedAt }
    }
}

class GetNoticeUseCase(
    private val repository: NoticeRepository,
) {
    operator fun invoke(noticeId: String, callback: (Result<Notice>) -> Unit) {
        repository.loadNotice(noticeId, callback)
    }
}

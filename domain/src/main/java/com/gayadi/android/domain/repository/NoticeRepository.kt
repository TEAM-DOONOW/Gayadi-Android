package com.gayadi.android.domain.repository

import com.gayadi.android.domain.model.Notice

interface NoticeRepository {
    fun loadNotices(callback: (Result<List<Notice>>) -> Unit)

    fun loadNotice(noticeId: String, callback: (Result<Notice>) -> Unit)
}

package com.gayadi.android.data.datasource

import com.gayadi.android.data.model.NoticeDto

interface NoticeDataSource {
    fun loadNotices(callback: (Result<List<NoticeDto>>) -> Unit)

    fun loadNotice(noticeId: String, callback: (Result<NoticeDto>) -> Unit)
}

package com.gayadi.android.data.repository

import com.gayadi.android.data.datasource.NoticeDataSource
import com.gayadi.android.data.mapper.toDomain
import com.gayadi.android.domain.model.Notice
import com.gayadi.android.domain.repository.NoticeRepository

class DefaultNoticeRepository(
    private val dataSource: NoticeDataSource,
) : NoticeRepository {
    override fun loadNotices(callback: (Result<List<Notice>>) -> Unit) {
        dataSource.loadNotices { result ->
            callback(
                result.mapCatching { notices -> notices.map { it.toDomain() } }
                    .withUserFacingMessage(LOAD_FAILURE_MESSAGE),
            )
        }
    }

    override fun loadNotice(noticeId: String, callback: (Result<Notice>) -> Unit) {
        dataSource.loadNotice(noticeId) { result ->
            callback(result.mapCatching { it.toDomain() }.withUserFacingMessage(LOAD_FAILURE_MESSAGE))
        }
    }

    private companion object {
        const val LOAD_FAILURE_MESSAGE = "업데이트 소식을 불러오지 못했어요. 잠시 후 다시 시도해 주세요."
    }
}

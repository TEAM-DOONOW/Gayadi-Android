package com.gayadi.android.data.datasource

import com.gayadi.android.data.model.NoticeDto
import com.gayadi.android.data.remote.GayadiHttpClient
import kotlinx.coroutines.CoroutineScope

class RemoteNoticeDataSource internal constructor(
    private val client: RemoteJsonClient,
    private val coroutineScope: CoroutineScope,
) : NoticeDataSource {
    constructor(
        httpClient: GayadiHttpClient,
        coroutineScope: CoroutineScope = defaultRemoteDataSourceScope(),
    ) : this(GayadiRemoteJsonClient(httpClient), coroutineScope)

    override fun loadNotices(callback: (Result<List<NoticeDto>>) -> Unit) {
        coroutineScope.launchRemoteRequest(callback) {
            buildList {
                var offset = 0
                do {
                    val response = client.getArray(
                        path = NOTICES_PATH,
                        query = mapOf("limit" to PAGE_SIZE.toString(), "offset" to offset.toString()),
                        authenticated = false,
                    )
                    repeat(response.length()) { index -> add(response.getJSONObject(index).toNoticeDto()) }
                    offset += response.length()
                } while (response.length() == PAGE_SIZE)
            }
        }
    }

    override fun loadNotice(noticeId: String, callback: (Result<NoticeDto>) -> Unit) {
        coroutineScope.launchRemoteRequest(callback) {
            require(noticeId.matches(NOTICE_ID_PATTERN)) { "공지 식별자가 올바르지 않습니다." }
            client.getObject("$NOTICES_PATH/$noticeId", authenticated = false).toNoticeDto()
        }
    }

    private companion object {
        const val NOTICES_PATH = "/api/v1/notices"
        const val PAGE_SIZE = 100
        val NOTICE_ID_PATTERN = Regex("[a-zA-Z0-9-]{1,50}")
    }
}

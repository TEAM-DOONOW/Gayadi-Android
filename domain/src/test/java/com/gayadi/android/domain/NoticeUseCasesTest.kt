package com.gayadi.android.domain

import com.gayadi.android.domain.model.Notice
import com.gayadi.android.domain.model.NoticeCategory
import com.gayadi.android.domain.model.NoticeSection
import com.gayadi.android.domain.repository.NoticeRepository
import com.gayadi.android.domain.usecase.GetNoticeUseCase
import com.gayadi.android.domain.usecase.GetNoticesUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NoticeUseCasesTest {
    @Test
    fun getNotices_ordersPinnedFirstThenNewest() {
        val pinnedOld = notice(id = "pinned-old", publishedAt = "2026-07-01", isPinned = true)
        val newest = notice(id = "newest", publishedAt = "2026-08-20")
        val older = notice(id = "older", publishedAt = "2026-08-01")
        var actual: List<Notice>? = null

        GetNoticesUseCase(FakeNoticeRepository(Result.success(listOf(older, newest, pinnedOld)))).invoke { result ->
            actual = result.getOrThrow()
        }

        assertEquals(listOf("pinned-old", "newest", "older"), actual?.map(Notice::id))
    }

    @Test
    fun getNotices_propagatesFailure() {
        var actual: Result<List<Notice>>? = null

        GetNoticesUseCase(FakeNoticeRepository(Result.failure(IllegalStateException("연결 실패")))).invoke { result ->
            actual = result
        }

        assertTrue(actual?.isFailure == true)
        assertEquals("연결 실패", actual?.exceptionOrNull()?.message)
    }

    @Test
    fun getNotice_returnsRequestedNotice() {
        val expected = notice(id = "1-2-0", publishedAt = "2026-08-20")
        var actual: Notice? = null

        GetNoticeUseCase(FakeNoticeRepository(single = Result.success(expected)))("1-2-0") { result ->
            actual = result.getOrThrow()
        }

        assertEquals(expected, actual)
    }

    private class FakeNoticeRepository(
        private val list: Result<List<Notice>> = Result.success(emptyList()),
        private val single: Result<Notice> = Result.failure(IllegalStateException("사용하지 않음")),
    ) : NoticeRepository {
        override fun loadNotices(callback: (Result<List<Notice>>) -> Unit) = callback(list)

        override fun loadNotice(noticeId: String, callback: (Result<Notice>) -> Unit) = callback(single)
    }

    private fun notice(id: String, publishedAt: String, isPinned: Boolean = false) = Notice(
        id = id,
        title = "업데이트 $id",
        category = NoticeCategory.UPDATE,
        version = "1.2.0",
        publishedAt = publishedAt,
        summary = "요약",
        sections = listOf(NoticeSection("달라진 점", "본문")),
        isPinned = isPinned,
    )
}

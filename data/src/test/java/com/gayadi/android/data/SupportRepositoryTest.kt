package com.gayadi.android.data

import com.gayadi.android.data.datasource.InquiryDataSource
import com.gayadi.android.data.datasource.NoticeDataSource
import com.gayadi.android.data.model.InquiryDto
import com.gayadi.android.data.model.NoticeDto
import com.gayadi.android.data.model.NoticeSectionDto
import com.gayadi.android.data.repository.DefaultInquiryRepository
import com.gayadi.android.data.repository.DefaultNoticeRepository
import com.gayadi.android.domain.model.InquiryCategory
import com.gayadi.android.domain.model.InquiryDraft
import com.gayadi.android.domain.model.Notice
import com.gayadi.android.domain.model.NoticeCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SupportRepositoryTest {
    @Test
    fun loadNotices_mapsUnknownCategoryToUpdate() {
        var actual: List<Notice>? = null

        DefaultNoticeRepository(FakeNoticeDataSource(list = Result.success(listOf(dto(category = "sale")))))
            .loadNotices { result -> actual = result.getOrThrow() }

        assertEquals(NoticeCategory.UPDATE, actual?.single()?.category)
    }

    @Test
    fun loadNotices_replacesRemoteErrorWithKoreanMessage() {
        var actual: Result<List<Notice>>? = null

        DefaultNoticeRepository(
            FakeNoticeDataSource(list = Result.failure(RuntimeException("PERMISSION_DENIED: Missing permissions."))),
        ).loadNotices { result -> actual = result }

        assertEquals(
            "업데이트 소식을 불러오지 못했어요. 잠시 후 다시 시도해 주세요.",
            actual?.exceptionOrNull()?.message,
        )
    }

    @Test
    fun loadNotice_keepsDataSourceValidationMessage() {
        var actual: Result<Notice>? = null

        DefaultNoticeRepository(
            FakeNoticeDataSource(single = Result.failure(IllegalArgumentException("업데이트 소식을 찾을 수 없습니다: x"))),
        ).loadNotice("x") { result -> actual = result }

        assertEquals("업데이트 소식을 찾을 수 없습니다: x", actual?.exceptionOrNull()?.message)
    }

    @Test
    fun submitInquiry_sendsCategoryIdAndHidesRemoteError() {
        val dataSource = FakeInquiryDataSource(Result.failure(RuntimeException("UNAVAILABLE: backend unreachable")))
        var actual: Result<Unit>? = null

        DefaultInquiryRepository(dataSource).submit(
            InquiryDraft(InquiryCategory.BUG, "지도가 안 보여요", "장소 검색 화면이 비어 있어요", "a@b.co"),
        ) { result -> actual = result }

        assertEquals("bug", dataSource.submitted?.category)
        assertTrue(actual?.isFailure == true)
        assertEquals("문의를 보내지 못했어요. 잠시 후 다시 시도해 주세요.", actual?.exceptionOrNull()?.message)
    }

    private class FakeNoticeDataSource(
        private val list: Result<List<NoticeDto>> = Result.success(emptyList()),
        private val single: Result<NoticeDto> = Result.failure(IllegalStateException("사용하지 않음")),
    ) : NoticeDataSource {
        override fun loadNotices(callback: (Result<List<NoticeDto>>) -> Unit) = callback(list)

        override fun loadNotice(noticeId: String, callback: (Result<NoticeDto>) -> Unit) = callback(single)
    }

    private class FakeInquiryDataSource(private val result: Result<Unit>) : InquiryDataSource {
        var submitted: InquiryDto? = null

        override fun submit(inquiry: InquiryDto, callback: (Result<Unit>) -> Unit) {
            submitted = inquiry
            callback(result)
        }
    }

    private fun dto(category: String?) = NoticeDto(
        id = "1-2-0",
        title = "공지사항",
        category = category,
        version = "1.2.0",
        publishedAt = "2026-08-20",
        summary = "요약",
        sections = listOf(NoticeSectionDto("달라진 점", "본문")),
        isPinned = false,
    )
}

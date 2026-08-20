package com.gayadi.android.ui.screens

import com.gayadi.android.domain.model.Notice
import com.gayadi.android.domain.model.NoticeCategory
import com.gayadi.android.domain.model.NoticeSection
import com.gayadi.android.domain.repository.NoticeRepository
import com.gayadi.android.domain.usecase.GetNoticeUseCase
import com.gayadi.android.domain.usecase.GetNoticesUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NoticeViewModelTest {
    @Test
    fun listLoadSuccess_exposesPublishedNotices() {
        val expected = sampleNotice()
        val viewModel = NoticeListViewModel(
            GetNoticesUseCase(FakeNoticeRepository(list = Result.success(listOf(expected)))),
        )

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(listOf(expected), viewModel.uiState.value.notices)
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun listLoadSuccessWithoutNotices_marksStateEmpty() {
        val viewModel = NoticeListViewModel(
            GetNoticesUseCase(FakeNoticeRepository(list = Result.success(emptyList()))),
        )

        assertTrue(viewModel.uiState.value.isEmpty)
    }

    @Test
    fun listLoadFailure_exposesRecoverableError() {
        val viewModel = NoticeListViewModel(
            GetNoticesUseCase(FakeNoticeRepository(list = Result.failure(IllegalStateException("연결 실패")))),
        )

        assertFalse(viewModel.uiState.value.isLoading)
        assertFalse(viewModel.uiState.value.isEmpty)
        assertEquals("연결 실패", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun detailLoadSuccess_exposesRequestedNotice() {
        val expected = sampleNotice()
        val viewModel = NoticeDetailViewModel(
            expected.id,
            GetNoticeUseCase(FakeNoticeRepository(single = Result.success(expected))),
        )

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(expected, viewModel.uiState.value.notice)
    }

    @Test
    fun detailLoadFailure_exposesRecoverableError() {
        val viewModel = NoticeDetailViewModel(
            "missing",
            GetNoticeUseCase(FakeNoticeRepository(single = Result.failure(IllegalStateException("없는 소식")))),
        )

        assertNull(viewModel.uiState.value.notice)
        assertEquals("없는 소식", viewModel.uiState.value.errorMessage)
    }

    private class FakeNoticeRepository(
        private val list: Result<List<Notice>> = Result.success(emptyList()),
        private val single: Result<Notice> = Result.failure(IllegalStateException("사용하지 않음")),
    ) : NoticeRepository {
        override fun loadNotices(callback: (Result<List<Notice>>) -> Unit) = callback(list)

        override fun loadNotice(noticeId: String, callback: (Result<Notice>) -> Unit) = callback(single)
    }

    private fun sampleNotice() = Notice(
        id = "1-2-0",
        title = "여행 초대 코드로 바로 입장할 수 있어요",
        category = NoticeCategory.UPDATE,
        version = "1.2.0",
        publishedAt = "2026-08-20",
        summary = "초대 링크를 누르면 곧바로 여행에 참여합니다.",
        sections = listOf(NoticeSection("달라진 점", "초대 링크 지원")),
        isPinned = false,
    )
}

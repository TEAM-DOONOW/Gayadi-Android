package com.gayadi.android.ui.screens

import com.gayadi.android.domain.model.LegalDocument
import com.gayadi.android.domain.model.LegalDocumentSection
import com.gayadi.android.domain.model.LegalDocumentType
import com.gayadi.android.domain.repository.LegalDocumentRepository
import com.gayadi.android.domain.usecase.GetLegalDocumentUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class LegalDocumentViewModelTest {
    @Test
    fun loadSuccess_exposesPublishedDocument() {
        val expected = sampleDocument()
        val viewModel = LegalDocumentViewModel(
            LegalDocumentType.TERMS_OF_SERVICE,
            GetLegalDocumentUseCase(FakeLegalRepository(Result.success(expected))),
        )

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(expected, viewModel.uiState.value.document)
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun loadFailure_exposesRecoverableError() {
        val viewModel = LegalDocumentViewModel(
            LegalDocumentType.PRIVACY_POLICY,
            GetLegalDocumentUseCase(FakeLegalRepository(Result.failure(IllegalStateException("연결 실패")))),
        )

        assertFalse(viewModel.uiState.value.isLoading)
        assertNull(viewModel.uiState.value.document)
        assertEquals("연결 실패", viewModel.uiState.value.errorMessage)
    }

    private class FakeLegalRepository(
        private val result: Result<LegalDocument>,
    ) : LegalDocumentRepository {
        override fun loadDocument(
            type: LegalDocumentType,
            callback: (Result<LegalDocument>) -> Unit,
        ) = callback(result)
    }

    private fun sampleDocument() = LegalDocument(
        id = "terms-of-service",
        title = "가야디 이용약관",
        version = "1.0.0",
        effectiveDate = "2026-08-12",
        summary = "서비스 이용 조건",
        sections = listOf(LegalDocumentSection("제1조", "본문")),
        reviewNotice = null,
    )
}

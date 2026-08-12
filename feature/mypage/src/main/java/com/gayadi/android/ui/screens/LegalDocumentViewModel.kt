package com.gayadi.android.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.gayadi.android.domain.model.LegalDocument
import com.gayadi.android.domain.model.LegalDocumentType
import com.gayadi.android.domain.usecase.GetLegalDocumentUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class LegalDocumentUiState(
    val document: LegalDocument? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
)

class LegalDocumentViewModel(
    private val type: LegalDocumentType,
    private val getLegalDocument: GetLegalDocumentUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(LegalDocumentUiState())
    val uiState: StateFlow<LegalDocumentUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun retry() = load()

    private fun load() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        getLegalDocument(type) { result ->
            result.fold(
                onSuccess = { document ->
                    _uiState.value = LegalDocumentUiState(document = document, isLoading = false)
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "문서를 불러오지 못했습니다.",
                        )
                    }
                },
            )
        }
    }

    companion object {
        fun factory(type: LegalDocumentType, getLegalDocument: GetLegalDocumentUseCase) = viewModelFactory {
            initializer { LegalDocumentViewModel(type, getLegalDocument) }
        }
    }
}

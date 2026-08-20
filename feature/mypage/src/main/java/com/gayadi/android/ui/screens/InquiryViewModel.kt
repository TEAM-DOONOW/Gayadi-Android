package com.gayadi.android.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.gayadi.android.domain.model.InquiryCategory
import com.gayadi.android.domain.model.InquiryDraft
import com.gayadi.android.domain.usecase.SubmitInquiryUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class InquiryUiState(
    val category: InquiryCategory = InquiryCategory.BUG,
    val title: String = "",
    val message: String = "",
    val contactEmail: String = "",
    val isSubmitting: Boolean = false,
    val isSubmitted: Boolean = false,
    val errorMessage: String? = null,
) {
    val canSubmit: Boolean
        get() = !isSubmitting && !isSubmitted &&
            title.isNotBlank() && message.isNotBlank() && contactEmail.isNotBlank()
}

class InquiryViewModel(
    private val submitInquiry: SubmitInquiryUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(InquiryUiState())
    val uiState: StateFlow<InquiryUiState> = _uiState.asStateFlow()

    fun updateCategory(category: InquiryCategory) {
        _uiState.update { it.copy(category = category, errorMessage = null) }
    }

    fun updateTitle(title: String) {
        _uiState.update { it.copy(title = title.take(TITLE_MAX_LENGTH), errorMessage = null) }
    }

    fun updateMessage(message: String) {
        _uiState.update { it.copy(message = message.take(MESSAGE_MAX_LENGTH), errorMessage = null) }
    }

    fun updateContactEmail(email: String) {
        _uiState.update { it.copy(contactEmail = email.trim(), errorMessage = null) }
    }

    fun submit() {
        val state = _uiState.value
        if (!state.canSubmit) return
        _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }
        val draft = InquiryDraft(
            category = state.category,
            title = state.title,
            message = state.message,
            contactEmail = state.contactEmail,
        )
        submitInquiry(draft) { result ->
            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(isSubmitting = false, isSubmitted = true) }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            errorMessage = error.message ?: "문의를 보내지 못했어요",
                        )
                    }
                },
            )
        }
    }

    /** 전송 완료 화면에서 문의를 하나 더 작성할 때 입력값을 비운다. */
    fun reset() {
        _uiState.value = InquiryUiState()
    }

    companion object {
        const val TITLE_MAX_LENGTH = 50
        const val MESSAGE_MAX_LENGTH = 1000

        fun factory(submitInquiry: SubmitInquiryUseCase) = viewModelFactory {
            initializer { InquiryViewModel(submitInquiry) }
        }
    }
}

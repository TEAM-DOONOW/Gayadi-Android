package com.gayadi.android.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.gayadi.android.domain.model.Notice
import com.gayadi.android.domain.usecase.GetNoticeUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class NoticeDetailUiState(
    val notice: Notice? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
)

class NoticeDetailViewModel(
    private val noticeId: String,
    private val getNotice: GetNoticeUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(NoticeDetailUiState())
    val uiState: StateFlow<NoticeDetailUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun retry() = load()

    private fun load() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        getNotice(noticeId) { result ->
            result.fold(
                onSuccess = { notice ->
                    _uiState.value = NoticeDetailUiState(notice = notice, isLoading = false)
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "업데이트 소식을 불러오지 못했습니다.",
                        )
                    }
                },
            )
        }
    }

    companion object {
        fun factory(noticeId: String, getNotice: GetNoticeUseCase) = viewModelFactory {
            initializer { NoticeDetailViewModel(noticeId, getNotice) }
        }
    }
}

package com.gayadi.android.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.gayadi.android.domain.model.Notice
import com.gayadi.android.domain.usecase.GetNoticesUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class NoticeListUiState(
    val notices: List<Notice> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
) {
    val isEmpty: Boolean get() = !isLoading && errorMessage == null && notices.isEmpty()
}

class NoticeListViewModel(
    private val getNotices: GetNoticesUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(NoticeListUiState())
    val uiState: StateFlow<NoticeListUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun retry() = load()

    private fun load() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        getNotices { result ->
            result.fold(
                onSuccess = { notices ->
                    _uiState.value = NoticeListUiState(notices = notices, isLoading = false)
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
        fun factory(getNotices: GetNoticesUseCase) = viewModelFactory {
            initializer { NoticeListViewModel(getNotices) }
        }
    }
}

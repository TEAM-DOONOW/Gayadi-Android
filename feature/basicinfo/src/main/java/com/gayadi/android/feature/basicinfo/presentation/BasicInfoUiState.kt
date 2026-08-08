package com.gayadi.android.feature.basicinfo.presentation

/** Immutable UI state for the basic information form. */
data class BasicInfoUiState(
    val nickname: String = "",
    val introduction: String = "",
    val isSaving: Boolean = false,
    val saveCompleted: Boolean = false,
    val errorMessage: String? = null,
) {
    /** Whether both required inputs contain non-whitespace text. */
    val canSubmit: Boolean
        get() = nickname.isNotBlank() && introduction.isNotBlank() && !isSaving
}

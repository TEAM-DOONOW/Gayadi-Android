package com.gayadi.android.feature.basicinfo.presentation

data class BasicInfoUiState(
    val nickname: String = "",
    val introduction: String = "",
) {
    val canSubmit: Boolean
        get() = nickname.isNotBlank() && introduction.isNotBlank()
}

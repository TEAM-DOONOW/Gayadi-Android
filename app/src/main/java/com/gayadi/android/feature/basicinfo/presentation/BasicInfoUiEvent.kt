package com.gayadi.android.feature.basicinfo.presentation

/** User-originated events handled by the basic information screen. */
sealed interface BasicInfoUiEvent {
    /** Updates the nickname input. */
    data class NicknameChanged(val value: String) : BasicInfoUiEvent

    /** Updates the one-line introduction input. */
    data class IntroductionChanged(val value: String) : BasicInfoUiEvent

    /** Requests validation and persistence of the current form. */
    data object Submit : BasicInfoUiEvent
}

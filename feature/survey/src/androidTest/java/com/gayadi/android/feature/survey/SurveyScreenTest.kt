package com.gayadi.android.feature.survey

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.gayadi.android.feature.survey.presentation.SurveyScreen
import com.gayadi.android.feature.survey.presentation.SurveyUiState
import com.gayadi.android.ui.theme.GayadiTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Verifies the Compose representation of survey empty state. */
class SurveyScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    /** Empty data shows guidance and exposes a working retry action. */
    @Test
    fun emptyQuestions_showRetryState() {
        var retried = false
        composeRule.setContent {
            GayadiTheme {
                SurveyScreen(
                    uiState = SurveyUiState(
                        isLoading = false,
                        errorMessage = "잠시 후 다시 시도해주세요.",
                        hasStarted = true,
                    ),
                    onStart = {},
                    onOptionSelected = {},
                    onRetry = { retried = true },
                    onNext = {},
                )
            }
        }

        composeRule.onNodeWithText("설문을 불러오지 못했어요").assertIsDisplayed()
        composeRule.onNodeWithText("다시 시도").performClick()
        assertTrue(retried)
    }

    /** The intro is shown while the survey is still loading, so start owns the loading screen. */
    @Test
    fun loadingBeforeStart_showsIntroInsteadOfLoading() {
        composeRule.setContent {
            GayadiTheme {
                SurveyScreen(
                    uiState = SurveyUiState(isLoading = true),
                    onStart = {},
                    onOptionSelected = {},
                    onRetry = {},
                    onNext = {},
                )
            }
        }

        composeRule.onNodeWithText("테스트 시작하기").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("비행기를 타고 이동하는 가야디").assertDoesNotExist()
    }

    /** Starting before the content arrives shows the loading screen. */
    @Test
    fun loadingAfterStart_showsLoadingScreen() {
        composeRule.setContent {
            GayadiTheme {
                SurveyScreen(
                    uiState = SurveyUiState(isLoading = true, hasStarted = true),
                    onStart = {},
                    onOptionSelected = {},
                    onRetry = {},
                    onNext = {},
                )
            }
        }

        composeRule.onNodeWithContentDescription("비행기를 타고 이동하는 가야디").assertIsDisplayed()
    }
}

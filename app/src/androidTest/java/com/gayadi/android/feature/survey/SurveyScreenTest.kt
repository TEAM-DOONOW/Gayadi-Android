package com.gayadi.android.feature.survey

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
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
                    uiState = SurveyUiState(),
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
}

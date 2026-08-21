package com.gayadi.android.ui.screens

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gayadi.android.ui.theme.GayadiTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RealtimeHomeScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun routeButtonNavigatesWhenMapIsAvailable() {
        var navigated = false
        composeRule.setContent {
            GayadiTheme {
                RealtimeHomeScreen(
                    uiState = RealtimeHomeUiState(),
                    tripTitle = "제주 여행",
                    kakaoMapJavaScriptKey = "test-key",
                    onNavigateMyTrip = {},
                    onNavigateMyPage = {},
                    onNavigateLedger = {},
                    onNavigatePlaceSearch = {},
                    onNavigateParticipants = {},
                    onUpdateSchedule = { _, _, _ -> },
                    onAddScheduleExpense = { _, _, _ -> },
                    onScheduleDirections = { _, _, _ -> },
                    onNavigateRoutes = { navigated = true },
                )
            }
        }

        composeRule.onNodeWithText("전체 동선 보기").performScrollTo().performClick()

        assertTrue(navigated)
    }
}

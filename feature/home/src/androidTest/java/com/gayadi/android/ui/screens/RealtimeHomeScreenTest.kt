package com.gayadi.android.ui.screens

import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.compose.ui.unit.dp
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

    @Test
    fun primaryHomeActionsAreAccessibleAndLargeEnough() {
        var participantsOpened = false
        var placeSearchOpened = false
        composeRule.setContent {
            GayadiTheme {
                RealtimeHomeScreen(
                    uiState = RealtimeHomeUiState(),
                    tripTitle = "제주 여행",
                    participantCount = 3,
                    tripDays = listOf(HomeTripDay(1, "2026.08.21", "8월 21일")),
                    onNavigateMyTrip = {},
                    onNavigateMyPage = {},
                    onNavigateLedger = {},
                    onNavigatePlaceSearch = { placeSearchOpened = true },
                    onNavigateParticipants = { participantsOpened = true },
                    onUpdateSchedule = { _, _, _ -> },
                    onAddScheduleExpense = { _, _, _ -> },
                    onScheduleDirections = { _, _, _ -> },
                    onNavigateRoutes = {},
                )
            }
        }

        composeRule.onNodeWithContentDescription("함께하는 친구 3명 보기")
            .assertHasClickAction()
            .performClick()
        composeRule.onNodeWithText("장소 추가")
            .performScrollTo()
            .assertHeightIsAtLeast(48.dp)
            .performClick()

        assertTrue(participantsOpened)
        assertTrue(placeSearchOpened)
    }

}

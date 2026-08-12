package com.gayadi.android.ui.screens

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gayadi.android.ui.theme.GayadiTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RealtimeHomeScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun alertOpensSuggestionAndAcceptUpdatesState() {
        var state by mutableStateOf(RealtimeHomeUiState())
        composeRule.setContent {
            GayadiTheme {
                RealtimeHomeScreen(
                    uiState = state,
                    tripTitle = "제주 여행",
                    nextScheduleName = null,
                    onNavigateMyTrip = {},
                    onNavigateMyPage = {},
                    onNavigatePlaceSearch = {},
                    onNavigateFriendAdd = {},
                    onOpenReschedule = { state = state.copy(showRescheduleSheet = true) },
                    onDismissReschedule = { state = state.copy(showRescheduleSheet = false) },
                    onAcceptReschedule = {
                        state = state.copy(
                            showRescheduleSheet = false,
                            rescheduleDecision = RescheduleDecision.ACCEPTED,
                        )
                    },
                    onRejectReschedule = {},
                )
            }
        }

        composeRule.onNodeWithText("곧 비가 와요, 실내로 다시 바꿀까요?").performClick()
        composeRule.onNodeWithText("AI 추천대로 변경").performClick()

        assertEquals(RescheduleDecision.ACCEPTED, state.rescheduleDecision)
    }
}

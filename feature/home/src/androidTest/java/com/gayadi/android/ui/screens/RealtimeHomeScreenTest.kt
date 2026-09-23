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
    fun routeButtonIsNotShown() {
        var notificationsOpened = false
        composeRule.setContent {
            GayadiTheme {
                RealtimeHomeScreen(
                    uiState = RealtimeHomeUiState(),
                    tripTitle = "제주 여행",
                    onNavigateMyTrip = {},
                    onNavigateMyPage = {},
                    onNavigateLedger = {},
                    onNavigateNotifications = { notificationsOpened = true },
                    onNavigatePlaceSearch = { _ -> },
                    onNavigateParticipants = {},
                    onUpdateSchedule = { _, _, _ -> },
                    onAddScheduleExpense = { _, _, _ -> },
                )
            }
        }

        composeRule.onNodeWithText("전체 동선 보기").assertDoesNotExist()
        composeRule.onNodeWithContentDescription("알림 확인")
            .assertHeightIsAtLeast(48.dp)
            .performClick()
        assertTrue(notificationsOpened)
    }

    @Test
    fun schedulePhotoOpensExactPlaceWithoutSavingSchedule() {
        var opened: Pair<String, String>? = null
        var saved = false
        composeRule.setContent {
            GayadiTheme {
                RealtimeHomeScreen(
                    uiState = RealtimeHomeUiState(), tripTitle = "서울 여행",
                    travelPlans = listOf(HomeTravelPlan("schedule-1", "관광지", "2026.09.23", "10:00", "", false, placeId="209")),
                    tripDays = listOf(HomeTripDay(1,"2026.09.23","9월 23일")),
                    onNavigateMyTrip = {}, onNavigateMyPage = {}, onNavigatePlaceSearch = {}, onNavigateParticipants = {},
                    onNavigatePlaceDetail = { id,date -> opened=id to date },
                    onUpdateSchedule = { _,_,_ -> saved=true }, onAddScheduleExpense = { _,_,_ -> },
                )
            }
        }
        composeRule.onNodeWithText("관광지").performScrollTo().performClick()
        composeRule.onNodeWithContentDescription("관광지 상세 보기").assertHeightIsAtLeast(48.dp).performClick()
        assertTrue(opened == ("209" to "2026.09.23"))
        assertTrue(!saved)
    }

    @Test
    fun primaryHomeActionsAreAccessibleAndLargeEnough() {
        var participantsOpened = false
        var selectedDate: String? = null
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
                    onNavigatePlaceSearch = { selectedDate = it },
                    onNavigateParticipants = { participantsOpened = true },
                    onUpdateSchedule = { _, _, _ -> },
                    onAddScheduleExpense = { _, _, _ -> },
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
        assertTrue(selectedDate == "2026.08.21")
    }

}

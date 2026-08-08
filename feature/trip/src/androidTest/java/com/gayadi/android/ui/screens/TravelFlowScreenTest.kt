package com.gayadi.android.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gayadi.android.domain.model.TravelTrip
import com.gayadi.android.domain.model.TripStatus
import com.gayadi.android.domain.model.UserProfile
import com.gayadi.android.domain.model.TravelSchedule
import com.gayadi.android.domain.model.ScheduleType
import com.gayadi.android.ui.theme.GayadiTheme
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TravelFlowScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun detailReflectsNicknameCharacterContextAndTripActions() {
        var started = false
        var invitationOpened = false
        var scheduleOpened = false
        var routesOpened = false
        composeRule.setContent {
            GayadiTheme {
                TripDetailScreen(
                    trip = TravelTrip(
                        id = "trip-28",
                        name = "제주 여행",
                        startDate = "2026.08.08",
                        endDate = "2026.08.10",
                        cities = listOf("제주"),
                        status = TripStatus.PLANNING,
                    ),
                    participants = emptyList(),
                    profile = UserProfile("미르", "", characterKey = "character_pca"),
                    onBack = {},
                    onEdit = {},
                    onDelete = {},
                    onStart = { started = true },
                    onFinish = {},
                    onParticipants = {},
                    onInvitation = { invitationOpened = true },
                    onSchedule = { scheduleOpened = true },
                    onRoutes = { routesOpened = true },
                    onHome = {},
                )
            }
        }

        composeRule.onNodeWithText("미르 님의 제주 여행").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("미르 캐릭터").assertIsDisplayed()
        composeRule.onNodeWithText("여행 초대").performClick()
        composeRule.onNodeWithText("일정 관리").performClick()
        composeRule.onNodeWithText("경로 추천").performClick()
        composeRule.runOnIdle {
            assertTrue(invitationOpened)
            assertTrue(scheduleOpened)
            assertTrue(routesOpened)
        }
        composeRule.onNodeWithText("여행 시작").performScrollTo().performClick()
        composeRule.runOnIdle { assertTrue(started) }
    }

    @Test
    fun emptyScheduleCanOpenMainAlternativeEditor() {
        val saved = mutableListOf<TravelSchedule>()
        composeRule.setContent {
            GayadiTheme {
                ScheduleScreen(
                    tripId = "trip-28",
                    tripName = "제주 여행",
                    defaultDate = "2026.08.08",
                    schedules = emptyList(),
                    onBack = {},
                    onSave = { saved += it },
                    onDelete = {},
                    onMove = { _, _ -> },
                    onToggleVisited = {},
                    onRecommendRoute = {},
                )
            }
        }

        composeRule.onNodeWithText("아직 일정이 없어요").assertIsDisplayed()
        composeRule.onNodeWithText("일정 추가").performClick()
        composeRule.onNodeWithText("메인 일정").assertIsDisplayed()
        composeRule.onNodeWithText("대체 일정").assertIsDisplayed()
        composeRule.onNodeWithText("일정 이름").performTextInput("성산일출봉")
        composeRule.onNodeWithText("저장").performClick()
        composeRule.runOnIdle {
            assertEquals("trip-28", saved.single().tripId)
            assertEquals("2026.08.08", saved.single().date)
            assertEquals(ScheduleType.MAIN, saved.single().type)
        }

        composeRule.onNodeWithText("일정 추가").performClick()
        composeRule.onNodeWithText("일정 이름").performTextInput("우도")
        composeRule.onNodeWithText("대체 일정").performClick()
        composeRule.onNodeWithText("저장").performClick()
        composeRule.runOnIdle { assertEquals(ScheduleType.ALTERNATIVE, saved.last().type) }
    }
}

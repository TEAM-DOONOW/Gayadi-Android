package com.gayadi.android.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import com.gayadi.android.domain.model.RouteTransportMode
import com.gayadi.android.domain.repository.RecommendedItinerary
import com.gayadi.android.domain.repository.RecommendedItineraryStop
import com.gayadi.android.ui.theme.GayadiTheme
import org.junit.Rule
import org.junit.Test

class ItineraryRouteScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun routeShowsLockedStopsStayTimeAndWholeRouteActions() {
        composeRule.setContent {
            GayadiTheme {
                ItineraryRouteScreen(
                    uiState = ItineraryRouteUiState(
                        date = "2026.10.03",
                        recommendation = RecommendedItinerary(
                            date = "2026.10.03",
                            startTime = "10:00",
                            endTime = "18:00",
                            transportMode = RouteTransportMode.PUBLIC_TRANSIT,
                            variation = 0,
                            estimated = true,
                            totalTravelMinutes = 18,
                            totalStayMinutes = 140,
                            summary = "동선을 맞춰어요.",
                            stops = listOf(
                                stop(1, "경복궁", "10:00", "11:20", 80, 0),
                                stop(2, "광화문", "11:38", "12:38", 60, 18),
                            ),
                        ),
                    ),
                    onBack = {},
                    onTransportModeSelected = {},
                    onTimeRangeSelected = { _, _ -> },
                    onNewRoute = {},
                    onRetry = {},
                    onApply = {},
                )
            }
        }

        composeRule.onNodeWithText("시작").assertIsDisplayed()
        composeRule.onNodeWithText("종료").assertIsDisplayed()
        composeRule.onNodeWithText("10:00").performClick()
        composeRule.onNodeWithText("확인").assertIsDisplayed().performClick()
        composeRule.onNodeWithText("3~12시간 범위에서 고르면 루트를 자동으로 다시 맞춰요.")
            .assertIsDisplayed()

        val routeList = composeRule.onNode(hasScrollAction())
        routeList.performScrollToNode(hasText("새 루트 받기"))
        composeRule.onNodeWithText("새 루트 받기").assertIsDisplayed()
        routeList.performScrollToNode(hasText("머무는 시간 1시간 20분"))
        composeRule.onNodeWithText("머무는 시간 1시간 20분").assertIsDisplayed()
        routeList.performScrollToNode(hasText("대중교통 1.8km · 약 18분"))
        composeRule.onNodeWithText("대중교통 1.8km · 약 18분").assertIsDisplayed()
        composeRule.onAllNodesWithContentDescription("루트에 고정된 장소", useUnmergedTree = true)
            .assertCountEquals(2)
        composeRule.onNodeWithText("이 루트로 일정 바꾸기").assertIsDisplayed()
    }

    @Test
    fun applyFailureKeepsReviewedRouteAndRetryActionVisible() {
        composeRule.setContent {
            GayadiTheme {
                ItineraryRouteScreen(
                    uiState = ItineraryRouteUiState(
                        date = "2026.10.03",
                        applyErrorMessage = "정보가 변경되었어요. 새 루트를 받은 뒤 다시 시도해 주세요.",
                        recommendation = RecommendedItinerary(
                            date = "2026.10.03",
                            startTime = "10:00",
                            endTime = "18:00",
                            transportMode = RouteTransportMode.PUBLIC_TRANSIT,
                            variation = 0,
                            estimated = true,
                            totalTravelMinutes = 18,
                            totalStayMinutes = 140,
                            summary = "동선을 맞췄어요.",
                            stops = listOf(
                                stop(1, "경복궁", "10:00", "11:20", 80, 0),
                                stop(2, "광화문", "11:38", "12:38", 60, 18),
                            ),
                        ),
                    ),
                    onBack = {},
                    onTransportModeSelected = {},
                    onTimeRangeSelected = { _, _ -> },
                    onNewRoute = {},
                    onRetry = {},
                    onApply = {},
                )
            }
        }

        composeRule.onNodeWithText("가야디가 맞춰본 하루 루트").assertIsDisplayed()
        composeRule.onNode(hasScrollAction()).performScrollToNode(
            hasText("정보가 변경되었어요. 새 루트를 받은 뒤 다시 시도해 주세요."),
        )
        composeRule.onNodeWithText("정보가 변경되었어요. 새 루트를 받은 뒤 다시 시도해 주세요.")
            .assertIsDisplayed()
        composeRule.onNodeWithText("이 루트로 일정 바꾸기").assertIsDisplayed()
    }

    private fun stop(
        order: Int,
        name: String,
        arrival: String,
        departure: String,
        stay: Int,
        travel: Int,
    ) = RecommendedItineraryStop(
        order = order,
        placeId = order.toString(),
        name = name,
        category = "관광명소",
        imageUrl = "",
        latitude = 37.5,
        longitude = 126.9,
        arrivalTime = arrival,
        departureTime = departure,
        stayMinutes = stay,
        travelMinutesFromPrevious = travel,
        distanceMetersFromPrevious = travel * 100,
    )
}

package com.gayadi.android.ui.screens

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gayadi.android.domain.model.CongestionHourlyForecast
import com.gayadi.android.domain.model.CongestionHourlyPoint
import com.gayadi.android.ui.theme.GayadiTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlaceDetailScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun backButtonInvokesCallback() {
        var backInvoked = false
        val place = FakePlaceRepository().places().getOrThrow().first()

        composeRule.setContent {
            GayadiTheme {
                PlaceDetailScreen(
                    place = place,
                    isScheduled = false,
                    onBack = { backInvoked = true },
                    onAddToSchedule = { _, _ -> },
                )
            }
        }

        composeRule.onNodeWithContentDescription("뒤로").performClick()
        composeRule.runOnIdle { assertTrue(backInvoked) }
    }

    @Test
    fun hourlyCongestionForecastIsShown() {
        val place = FakePlaceRepository().places().getOrThrow().first()

        composeRule.setContent {
            GayadiTheme {
                PlaceDetailScreen(
                    place = place,
                    isScheduled = false,
                    onBack = {},
                    onAddToSchedule = { _, _ -> },
                    hourlyUiState = CongestionHourlyUiState(
                        forecast = CongestionHourlyForecast(
                            placeName = place.name,
                            points = listOf(
                                CongestionHourlyPoint(hour = 13, concentrationScore = 72, level = "혼잡"),
                            ),
                        ),
                    ),
                )
            }
        }

        composeRule.onNodeWithText("시간대별 혼잡 예상").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("13시").assertIsDisplayed()
        composeRule.onNodeWithText("72").assertIsDisplayed()
    }
}

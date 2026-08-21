package com.gayadi.android.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gayadi.android.ui.theme.GayadiTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TripCreationCompleteScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun compactScreenKeepsPrimaryActionVisible() {
        var tripStarted = false
        composeRule.setContent {
            GayadiTheme {
                Box(modifier = Modifier.size(width = 320.dp, height = 480.dp)) {
                    TripCreationCompleteScreen(
                        trip = sampleTrip(),
                        onStartTrip = { tripStarted = true },
                        onCoordinateDates = {},
                    )
                }
            }
        }

        composeRule.onNodeWithText("바로 여행 시작하기")
            .assertIsDisplayed()
            .assertHeightIsAtLeast(48.dp)
            .performClick()
        composeRule.onNodeWithText("새로운 여행이 만들어졌어요!")
            .performScrollTo()
            .assertIsDisplayed()

        assertTrue(tripStarted)
    }

    @Test
    fun largeFontKeepsContentScrollableAndActionsVisible() {
        composeRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 2f)) {
                GayadiTheme {
                    Box(modifier = Modifier.size(width = 360.dp, height = 640.dp)) {
                        TripCreationCompleteScreen(
                            trip = sampleTrip().copy(
                                name = "친구들과 함께 떠나는 아주 긴 여름 제주 여행",
                                isGroupTrip = true,
                            ),
                            onStartTrip = {},
                            onCoordinateDates = {},
                        )
                    }
                }
            }
        }

        composeRule.onNodeWithText("가능한 날짜 정하기")
            .assertIsDisplayed()
            .assertHeightIsAtLeast(48.dp)
        composeRule.onNodeWithText("초대코드 복사하기")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText("친구들과 함께 떠나는 아주 긴 여름 제주 여행")
            .performScrollTo()
            .assertIsDisplayed()
    }

    private fun sampleTrip() = TripSummary(
        id = "trip-responsive",
        name = "제주 여행",
        startDate = "2026.08.24",
        endDate = "2026.08.27",
        cities = listOf("제주"),
        coverImageResList = emptyList(),
        inviteCode = "A1B2C3",
    )
}

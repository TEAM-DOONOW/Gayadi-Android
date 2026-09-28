package com.gayadi.android.ui.screens

import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.ui.unit.dp

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.graphics.asAndroidBitmap
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
    fun weatherAndGraphRemainVisibleWhenDataIsMissing() {
        composeRule.setContent {
            GayadiTheme {
                PlaceDetailScreen(place = FakePlaceRepository().places().getOrThrow().first(),
                    isScheduled = false, onBack = {}, onAddToSchedule = { _, _ -> })
            }
        }
        composeRule.onNodeWithText("현재 날씨").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("정보 없음").assertIsDisplayed()
        composeRule.onNodeWithText("시간대별 혼잡 예상").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("혼잡 정보 없음 · 다시 확인").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun realWeatherAndGraphFitNarrowScreen() {
        composeRule.setContent {
            GayadiTheme {
                androidx.compose.foundation.layout.Box(androidx.compose.ui.Modifier.requiredWidth(320.dp)) {
                    PlaceDetailScreen(
                        place = FakePlaceRepository().places().getOrThrow().first().copy(name = "긴 이름을 가진 관광지와 주변 산책길"),
                        isScheduled = false, onBack = {}, onAddToSchedule = { _, _ -> },
                        weatherUiState = PlaceWeatherUiState(weather = com.gayadi.android.domain.repository.WeatherResult("20260928", "1400", "21.5", 0, condition = "맑음", precipitationProbability = 10)),
                        hourlyUiState = CongestionHourlyUiState(forecast = CongestionHourlyForecast(
                            targetDate = "2026-10-01", points = listOf(
                                CongestionHourlyPoint(9, 32, "여유"), CongestionHourlyPoint(11, 55, "보통"),
                                CongestionHourlyPoint(13, 72, "혼잡"), CongestionHourlyPoint(15, 65, "혼잡"),
                                CongestionHourlyPoint(17, 42, "보통"), CongestionHourlyPoint(19, 25, "여유"),
                            ),
                        )),
                    )
                }
            }
        }
        composeRule.onNodeWithText("21.5°").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("맑음 · 강수확률 10%").assertIsDisplayed()
        saveScreenshot("place-detail-weather.png")
        composeRule.onNodeWithText("시간대별 혼잡 예상").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithContentDescription("13시, 혼잡 점수 72, 혼잡").performScrollTo().assertIsDisplayed()
        saveScreenshot("place-detail-graph.png")
        composeRule.onNodeWithText("일정에 추가").assertIsDisplayed()
        saveScreenshot("place-detail-buttons.png")
    }

    @Test
    fun unavailableWeatherHidesReturnedValues() {
        composeRule.setContent {
            GayadiTheme {
                PlaceDetailScreen(
                    place = FakePlaceRepository().places().getOrThrow().first(),
                    isScheduled = false, onBack = {}, onAddToSchedule = { _, _ -> },
                    weatherUiState = PlaceWeatherUiState(weather = com.gayadi.android.domain.repository.WeatherResult(
                        "", "", "23", 0, available = false, condition = "비", precipitationProbability = 90,
                    )),
                )
            }
        }
        composeRule.onNodeWithText("현재 날씨").performScrollTo()
        composeRule.onNodeWithText("정보 없음").assertIsDisplayed()
        composeRule.onNodeWithText("23°").assertDoesNotExist()
        composeRule.onNodeWithText("비 · 강수확률 90%").assertDoesNotExist()
    }

    private fun saveScreenshot(name: String) {
        val instrumentation = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
        val file = java.io.File(instrumentation.targetContext.getExternalFilesDir(null), name)
        file.outputStream().use { stream -> composeRule.onRoot().captureToImage().asAndroidBitmap()
            .compress(android.graphics.Bitmap.CompressFormat.PNG, 100, stream) }
    }

    @Test
    fun calendarForecastShowsBasisAndDateOnNarrowScreen() {
        val place = FakePlaceRepository().places().getOrThrow().first()
        composeRule.setContent {
            GayadiTheme {
                androidx.compose.foundation.layout.Box(androidx.compose.ui.Modifier.requiredWidth(320.dp)) {
                    PlaceDetailScreen(
                        place = place, isScheduled = false, onBack = {}, onAddToSchedule = { _, _ -> },
                        forecastUiState = CongestionForecastUiState(
                            forecast = com.gayadi.android.domain.repository.CongestionResult(
                                "혼잡", 72, true, false, targetDate = "2026-09-28",
                                message = "관광공사 자료가 없어 달력으로 추정했어요. 실제 혼잡과 다를 수 있어요.",
                            ),
                        ),
                    )
                }
            }
        }
        composeRule.onNodeWithText("혼잡 점수 72 · 달력 추정").performScrollTo().assertIsDisplayed()
        val context = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext
        val file = java.io.File(context.getExternalFilesDir(null), "congestion-forecast.png")
        file.outputStream().use { stream ->
            androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
                .compress(android.graphics.Bitmap.CompressFormat.PNG, 100, stream)
        }
    }

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
        composeRule.onNodeWithContentDescription("13시, 혼잡 점수 72, 혼잡").performScrollTo().assertIsDisplayed()
        saveScreenshot("place-detail-normal.png")
    }
}

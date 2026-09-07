package com.gayadi.android.ui.screens

import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onRoot
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.click
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gayadi.android.ui.theme.GayadiTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlaceUsageGuideTest {
    @get:Rule val rule = createComposeRule()
    private val searchGuide = "가고 싶은 장소를 눌러\n상세 정보를 확인해 보세요"
    private val detailGuide = "마음에 드는 장소라면\n시간과 메모를 정해 일정에 추가하세요"

    private fun captureGuide(name: String) {
        val instrumentation = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
        val screenshot = rule.onRoot().captureToImage().asAndroidBitmap()
        java.io.File(instrumentation.targetContext.cacheDir, "$name.png").outputStream().use {
            screenshot.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
        }
        screenshot.recycle()
    }

    @Test
    fun searchGuideWaitsForResultsAndOpensHighlightedPlace() {
        val places = FakePlaceRepository().places().getOrThrow()
        val state = mutableStateOf(PlaceUiState(isLoading = true))
        var selected: String? = null
        var finished = 0
        rule.setContent {
            GayadiTheme {
                PlaceSearchScreen(
                    uiState = state.value, onBack = {}, onQueryChange = {},
                    onCategorySelected = {}, onPlaceClick = { selected = it }, onRetry = {},
                    showUsageGuide = true, onUsageGuideFinished = { finished++ },
                )
            }
        }
        rule.onNodeWithText(searchGuide).assertDoesNotExist()
        rule.runOnIdle { state.value = PlaceUiState(isLoading = false, errorMessage = "조회 실패") }
        rule.onNodeWithText(searchGuide).assertDoesNotExist()
        rule.runOnIdle { state.value = PlaceUiState(isLoading = false) }
        rule.onNodeWithText(searchGuide).assertDoesNotExist()
        rule.runOnIdle { state.value = PlaceUiState(isLoading = false, places = places) }
        rule.onNodeWithText(searchGuide).assertIsDisplayed()
        captureGuide("place-search-guide")
        rule.onNodeWithText(places.first().name).performTouchInput { click() }
        rule.runOnIdle {
            assertEquals(places.first().id, selected)
            assertEquals(1, finished)
        }
        rule.onNodeWithText(searchGuide).assertDoesNotExist()
    }

    @Test
    fun dismissSearchGuideKeepsPlaceSelectionUntouched() {
        var selected: String? = null
        var finished = 0
        rule.setContent {
            GayadiTheme {
                PlaceSearchScreen(
                    uiState = PlaceUiState(isLoading = false, places = FakePlaceRepository().places().getOrThrow()),
                    onBack = {}, onQueryChange = {}, onCategorySelected = {},
                    onPlaceClick = { selected = it }, onRetry = {},
                    showUsageGuide = true, onUsageGuideFinished = { finished++ },
                )
            }
        }
        rule.onNodeWithContentDescription("사용 안내 닫기").performClick()
        rule.onNodeWithText(searchGuide).assertDoesNotExist()
        rule.runOnIdle {
            assertEquals(null, selected)
            assertEquals(1, finished)
        }
    }

    @Test
    fun detailGuideOpensScheduleOptionsWithoutAddingImmediately() {
        var finished = 0
        var added = 0
        rule.setContent {
            GayadiTheme {
                PlaceDetailScreen(
                    place = FakePlaceRepository().places().getOrThrow().first(),
                    isScheduled = false, onBack = {}, onAddToSchedule = { _, _ -> added++ },
                    showUsageGuide = true, onUsageGuideFinished = { finished++ },
                )
            }
        }
        rule.onNodeWithText(detailGuide).assertIsDisplayed()
        captureGuide("place-detail-guide")
        rule.onNodeWithText("일정에 추가").performTouchInput { click() }
        rule.onNodeWithText(detailGuide).assertDoesNotExist()
        rule.onNodeWithText("메모 추가").assertIsDisplayed()
        rule.runOnIdle {
            assertEquals(1, finished)
            assertEquals(0, added)
        }
    }

    @Test
    fun scheduledPlaceDoesNotShowGuide() {
        rule.setContent {
            GayadiTheme {
                PlaceDetailScreen(
                    place = FakePlaceRepository().places().getOrThrow().first(),
                    isScheduled = true, onBack = {}, onAddToSchedule = { _, _ -> },
                    showUsageGuide = true,
                )
            }
        }
        rule.onNodeWithText(detailGuide).assertDoesNotExist()
    }
}

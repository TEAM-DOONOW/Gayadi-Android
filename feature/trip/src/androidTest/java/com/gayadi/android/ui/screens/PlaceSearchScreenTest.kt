package com.gayadi.android.ui.screens

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gayadi.android.ui.theme.GayadiTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlaceSearchScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun backButtonInvokesCallback() {
        var backInvoked = false
        composeRule.setContent {
            GayadiTheme {
                PlaceSearchScreen(
                    uiState = PlaceUiState(isLoading = false),
                    onBack = { backInvoked = true },
                    onQueryChange = {},
                    onCategorySelected = {},
                    onPlaceClick = {},
                    onRetry = {},
                )
            }
        }

        composeRule.onNodeWithContentDescription("뒤로").performClick()
        composeRule.runOnIdle { assertTrue(backInvoked) }
    }

    @Test
    fun searchDisplaysEmptyState() {
        var state by mutableStateOf(
            PlaceUiState(places = FakePlaceRepository().places().getOrThrow(), isLoading = false),
        )
        composeRule.setContent {
            GayadiTheme {
                PlaceSearchScreen(
                    uiState = state,
                    onBack = {},
                    onQueryChange = { state = state.copy(query = it) },
                    onCategorySelected = { state = state.copy(selectedCategory = it) },
                    onPlaceClick = {},
                    onRetry = {},
                )
            }
        }

        composeRule.onNodeWithText("맛집, 카페, 명소 검색").performTextInput("없는 장소")
        composeRule.onNodeWithText("조건에 맞는 장소가 없어요").assertIsDisplayed()
    }

    @Test
    fun displaysSelectedTripRegion() {
        val state = PlaceUiState(
            regionName = "서울",
            places = FakePlaceRepository().places("서울").getOrThrow(),
            isLoading = false,
        )
        composeRule.setContent {
            GayadiTheme {
                PlaceSearchScreen(
                    uiState = state,
                    onBack = {},
                    onQueryChange = {},
                    onCategorySelected = {},
                    onPlaceClick = {},
                    onRetry = {},
                )
            }
        }

        composeRule.onNodeWithText("서울 · 4곳").assertIsDisplayed()
        composeRule.onNodeWithText("광장시장").assertIsDisplayed()
    }

    @Test
    fun categoryChipsShowOnlyTheSelectedTourApiCategory() {
        var state by mutableStateOf(
            PlaceUiState(
                selectedCategory = "관광명소",
                places = listOf(
                    tourApiPlace("restaurant", "실데이터 맛집", "맛집", "🍲"),
                    tourApiPlace("cafe", "실데이터 카페", "카페", "☕"),
                    tourApiPlace("attraction", "실데이터 관광명소", "관광명소", "🏞️"),
                    tourApiPlace("stay", "실데이터 숙소", "숙소", "🏨"),
                ),
                isLoading = false,
            ),
        )
        composeRule.setContent {
            GayadiTheme {
                PlaceSearchScreen(
                    uiState = state,
                    onBack = {},
                    onQueryChange = { state = state.copy(query = it) },
                    onCategorySelected = { state = state.copy(selectedCategory = it) },
                    onPlaceClick = {},
                    onRetry = {},
                )
            }
        }

        composeRule.onNodeWithText("장소 추천 · 1곳").assertIsDisplayed()
        composeRule.onNodeWithText("실데이터 관광명소").assertIsDisplayed()

        composeRule.onNodeWithText("맛집").performClick()
        composeRule.onNodeWithText("실데이터 맛집").assertIsDisplayed()
        composeRule.onNodeWithText("실데이터 관광명소").assertDoesNotExist()

        composeRule.onNodeWithText("카페").performClick()
        composeRule.onNodeWithText("실데이터 카페").assertIsDisplayed()
        composeRule.onNodeWithText("실데이터 맛집").assertDoesNotExist()

        composeRule.onNodeWithText("숙소").performClick()
        composeRule.onNodeWithText("실데이터 숙소").assertIsDisplayed()
        composeRule.onNodeWithText("실데이터 카페").assertDoesNotExist()
    }
}

private fun tourApiPlace(
    id: String,
    name: String,
    category: String,
    emoji: String,
) = PlaceItem(
    id = id,
    name = name,
    category = category,
    rating = 0.0,
    reviews = 0,
    crowdLevel = CrowdLevel.NORMAL,
    emoji = emoji,
    description = "TourAPI 장소",
    hasRealtimeDetails = false,
)

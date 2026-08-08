package com.gayadi.android.ui.screens

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gayadi.android.ui.theme.GayadiTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlaceSearchScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun searchDisplaysEmptyState() {
        var state by mutableStateOf(
            PlaceUiState(places = FakePlaceRepository().getPlaces().getOrThrow(), isLoading = false),
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
}

package com.gayadi.android.ui.screens

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gayadi.android.ui.theme.GayadiTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FriendAddScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun searchAndAddRecommendedFriend() {
        var state by mutableStateOf(
            FriendAddUiState(friends = FakeFriendRepository().getFriends().getOrThrow(), isLoading = false),
        )
        composeRule.setContent {
            GayadiTheme {
                FriendAddScreen(
                    uiState = state,
                    onBack = {},
                    onQueryChange = { state = state.copy(query = it) },
                    onAddFriend = { id ->
                        state = state.copy(
                            friends = state.friends.map {
                                if (it.id == id) it.copy(status = FriendStatus.ADDED) else it
                            },
                        )
                    },
                    onRetry = {},
                )
            }
        }

        composeRule.onNodeWithText("이름, 아이디 검색").performTextInput("시연")
        composeRule.onNodeWithText("+ 추가").performClick()
        composeRule.onNodeWithText("추가됨").assertIsDisplayed()
    }
}

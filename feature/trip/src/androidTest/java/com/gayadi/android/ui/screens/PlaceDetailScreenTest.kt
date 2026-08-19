package com.gayadi.android.ui.screens

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
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
}

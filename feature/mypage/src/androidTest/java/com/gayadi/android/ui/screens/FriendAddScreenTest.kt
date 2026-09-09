package com.gayadi.android.ui.screens

import android.graphics.Bitmap
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.gayadi.android.domain.repository.*
import com.gayadi.android.ui.theme.GayadiTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class FriendAddScreenTest {
    @get:Rule val composeRule = createComposeRule()

    @Test fun incomingRequestCanBeAcceptedAndCaptured() {
        val friend = Friendship("1", FriendshipUser("2", "동행 친구"), "PENDING", false, true, 0)
        var accepted: String? = null
        composeRule.setContent {
            GayadiTheme {
                FriendAddScreen(FriendAddUiState(isLoading = false), {}, {}, onAddFriend = {}, onRetry = {},
                    friendshipContent = {
                        FriendshipPanel(FriendshipUiState(friends = listOf(friend)), {}, {},
                            { relationship, accept -> if(accept) accepted = relationship.id }, {}, {})
                    })
            }
        }
        composeRule.onNodeWithText("수락").performScrollTo().performClick()
        assertEquals("1", accepted)
        composeRule.waitForIdle()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val file = File(instrumentation.targetContext.getExternalFilesDir(null), "friends-small-fixture.png")
        val bitmap = requireNotNull(instrumentation.uiAutomation.takeScreenshot())
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
        instrumentation.sendStatus(0, android.os.Bundle().apply { putString("stream", "Screenshot: ${file.absolutePath}\n") })
    }
}

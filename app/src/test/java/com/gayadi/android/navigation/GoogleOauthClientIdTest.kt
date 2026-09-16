package com.gayadi.android.navigation

import org.junit.Assert.assertEquals
import org.junit.Test

class GoogleOauthClientIdTest {
    @Test
    fun usesConfiguredWebClientId() {
        assertEquals(
            "6035741280-j8ed9ka462jcvhoc14q7hb8vl26iqk0f.apps.googleusercontent.com",
            googleOauthServerClientId(
                "6035741280-j8ed9ka462jcvhoc14q7hb8vl26iqk0f.apps.googleusercontent.com",
            ),
        )
    }

    @Test(expected = IllegalStateException::class)
    fun rejectsPlaceholderClientId() {
        googleOauthServerClientId("your_dev_web_client_id.apps.googleusercontent.com")
    }

    @Test
    fun hidesStandaloneCoroutineCancellationFromLoginUi() {
        assertEquals(
            GOOGLE_LOGIN_CANCELLED_MESSAGE,
            googleLoginUserMessage(IllegalStateException("StandaloneCoroutine was cancelled")),
        )
        assertEquals(
            GOOGLE_LOGIN_FAILED_MESSAGE,
            googleLoginUserMessage(IllegalStateException("invalid audience")),
        )
    }
}

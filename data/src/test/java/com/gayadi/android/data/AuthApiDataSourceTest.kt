package com.gayadi.android.data

import com.gayadi.android.data.datasource.HttpAuthApiDataSource
import org.junit.Assert.assertEquals
import org.junit.Test

class AuthApiDataSourceTest {
    @Test
    fun parsesGoogleTokenExchangeResponse() {
        val session = HttpAuthApiDataSource("https://api.example.com").parseSession(
            """
            {
              "accessToken": "gayadi-token",
              "tokenType": "Bearer",
              "expiresIn": 7200,
              "refreshToken": "gayadi-refresh-token",
              "refreshExpiresIn": 2592000,
              "user": {
                "id": 12,
                "nickname": "가야디",
                "email": "traveler@example.com"
              }
            }
            """.trimIndent(),
        )

        assertEquals("gayadi-token", session.accessToken)
        assertEquals("Bearer", session.tokenType)
        assertEquals(7200L, session.expiresInSeconds)
        assertEquals("gayadi-refresh-token", session.refreshToken)
        assertEquals(2592000L, session.refreshExpiresInSeconds)
        assertEquals(12L, session.user.id)
        assertEquals("가야디", session.user.nickname)
        assertEquals("traveler@example.com", session.user.email)
    }
}

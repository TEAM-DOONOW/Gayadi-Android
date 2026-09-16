package com.gayadi.android.domain.error

import java.io.IOException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TransientApiTest {
    @Test
    fun retriesCancellationAndThenSucceeds() = runTest {
        var attempts = 0
        val value = retryTransientRequest(times = 3, initialDelayMs = 1) {
            attempts += 1
            if (attempts < 3) error("StandaloneCoroutine was cancelled")
            "ok"
        }
        assertEquals("ok", value)
        assertEquals(3, attempts)
    }

    @Test
    fun doesNotRetryClientErrors() = runTest {
        var attempts = 0
        val thrown = runCatching {
            retryTransientRequest(times = 4, initialDelayMs = 1) {
                attempts += 1
                error("요청한 정보를 찾을 수 없어요.")
            }
        }.exceptionOrNull()
        assertEquals("요청한 정보를 찾을 수 없어요.", thrown?.message)
        assertEquals(1, attempts)
    }

    @Test
    fun classifiesNetworkFailuresAsTransient() {
        assertTrue(IOException("서버에 연결하지 못했어요. 잠시 후 다시 시도해 주세요.").isTransientApiFailure())
        assertTrue(IllegalStateException("요청을 처리하지 못했어요. 잠시 후 다시 시도해 주세요. (HTTP 503)").isTransientApiFailure())
        assertFalse(IllegalStateException("로그인이 만료되었어요. 다시 로그인해 주세요.").isTransientApiFailure())
    }
}

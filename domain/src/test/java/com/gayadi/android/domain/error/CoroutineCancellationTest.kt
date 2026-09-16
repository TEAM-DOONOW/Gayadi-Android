package com.gayadi.android.domain.error

import kotlinx.coroutines.CancellationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class CoroutineCancellationTest {
    @Test
    fun detectsDirectAndWrappedCancellation() {
        val cancellation = CancellationException("StandaloneCoroutine was cancelled")

        assertTrue(cancellation.isCoroutineCancellation())
        assertTrue(IllegalStateException("wrapper", cancellation).isCoroutineCancellation())
        assertFalse(IllegalStateException("server failed").isCoroutineCancellation())
    }

    @Test
    fun detectsCredentialManagerStyleCancellationMessage() {
        val wrapped = IllegalStateException("StandaloneCoroutine was cancelled")

        assertTrue(wrapped.isCoroutineCancellation())
        assertTrue("StandaloneCoroutine was cancelled".isCoroutineCancellationMessage())
        assertFalse("Google 로그인이 취소되었습니다. 다시 시도해 주세요.".isCoroutineCancellationMessage())
    }

    @Test
    fun rethrowsOriginalCancellationFromWrapper() {
        val cancellation = CancellationException("StandaloneCoroutine was cancelled")
        val wrapped = IllegalStateException("wrapper", cancellation)

        val thrown = try {
            wrapped.rethrowCancellation()
            null
        } catch (error: CancellationException) {
            error
        }

        assertSame(cancellation, thrown)
    }

    @Test
    fun userFacingMessageHidesCoroutineCancellationText() {
        assertEquals(
            "Google 로그인에 실패했습니다.",
            IllegalStateException("StandaloneCoroutine was cancelled")
                .userFacingMessage("Google 로그인에 실패했습니다."),
        )
        assertEquals(
            "서버가 응답하지 않았어요",
            IllegalStateException("서버가 응답하지 않았어요")
                .userFacingMessage("Google 로그인에 실패했습니다."),
        )
    }

    @Test
    fun runCatchingPreservingCancellationDoesNotSwallowCancellation() {
        val cancellation = CancellationException("StandaloneCoroutine was cancelled")
        val thrown = try {
            runCatchingPreservingCancellation<Unit> { throw cancellation }
            null
        } catch (error: CancellationException) {
            error
        }

        assertSame(cancellation, thrown)
        assertTrue(runCatchingPreservingCancellation<Unit> { error("server failed") }.isFailure)
    }
}

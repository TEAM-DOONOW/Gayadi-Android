package com.gayadi.android.domain.error

import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.isActive

const val TRANSIENT_REQUEST_ATTEMPTS = 6
const val TRANSIENT_RETRY_INITIAL_DELAY_MS = 400L
const val TRANSIENT_RETRY_MAX_DELAY_MS = 8_000L

fun Throwable.isTransientApiFailure(): Boolean {
    if (isCoroutineCancellation()) return true
    val message = message.orEmpty()
    if ("로그인이 필요" in message || "로그인이 만료" in message) return false
    if ("권한이 없" in message || "찾을 수 없" in message) return false
    if (HTTP_CLIENT_ERROR.containsMatchIn(message) && !message.contains("HTTP 429")) return false
    if ("서버에 연결하지 못했어요" in message || "잠시 후 다시 시도" in message) return true
    if (HTTP_RETRYABLE.containsMatchIn(message)) return true
    val lower = message.lowercase()
    if ("timeout" in lower || "timed out" in lower || "failed to connect" in lower) return true
    return this is IOException && this::class.simpleName != "GayadiApiException"
}

suspend fun <T> retryTransientRequest(
    times: Int = TRANSIENT_REQUEST_ATTEMPTS,
    initialDelayMs: Long = TRANSIENT_RETRY_INITIAL_DELAY_MS,
    block: suspend () -> T,
): T {
    require(times >= 5) { "일시적 요청은 5회 이상 시도해야 합니다." }
    var lastError: Throwable? = null
    var delayMs = initialDelayMs
    repeat(times) { attempt ->
        try {
            return block()
        } catch (cancelled: CancellationException) {
            if (!coroutineContext.isActive) throw cancelled
            lastError = cancelled
        } catch (error: Exception) {
            error.rethrowCancellation()
            if (!error.isTransientApiFailure()) throw error
            lastError = error
        }
        if (attempt < times - 1) {
            delay(delayMs)
            delayMs = (delayMs * 2).coerceAtMost(TRANSIENT_RETRY_MAX_DELAY_MS)
        }
    }
    throw lastError ?: IllegalStateException("요청을 다시 시도하지 못했어요")
}

suspend fun <T> retryTransientResult(block: suspend () -> Result<T>): Result<T> = try {
    Result.success(
        retryTransientRequest {
            block().getOrElse { error ->
                error.rethrowCancellation()
                throw error
            }
        },
    )
} catch (cancelled: CancellationException) {
    if (!coroutineContext.isActive) throw cancelled
    Result.failure(cancelled)
} catch (error: Exception) {
    Result.failure(error)
}

private val HTTP_CLIENT_ERROR = Regex("HTTP 4\\d\\d")
private val HTTP_RETRYABLE = Regex("HTTP (429|5\\d\\d)")

package com.gayadi.android.domain.error

import kotlinx.coroutines.CancellationException

/** Returns a cancellation found anywhere in an exception chain. */
fun Throwable.cancellationExceptionOrNull(): CancellationException? {
    val visited = java.util.Collections.newSetFromMap(
        java.util.IdentityHashMap<Throwable, Boolean>(),
    )
    var current: Throwable? = this
    while (current != null && visited.add(current)) {
        if (current is CancellationException) return current
        current = current.cause
    }
    return null
}

/** Cancellation controls coroutine lifetime and must never become user-facing API text. */
fun Throwable.rethrowCancellation() {
    cancellationExceptionOrNull()?.let { throw it }
}

fun Throwable.isCoroutineCancellation(): Boolean {
    val visited = java.util.Collections.newSetFromMap(
        java.util.IdentityHashMap<Throwable, Boolean>(),
    )
    var current: Throwable? = this
    while (current != null && visited.add(current)) {
        if (current is CancellationException) return true
        if (current.message.orEmpty().isCoroutineCancellationMessage()) return true
        current = current.cause
    }
    return false
}

fun CharSequence.isCoroutineCancellationMessage(): Boolean {
    val normalized = toString().trim().lowercase()
    return normalized == "job was cancelled" ||
        normalized.endsWith("coroutine was cancelled")
}

/** Maps a failure to UI copy without leaking coroutine-cancellation internals. */
fun Throwable.userFacingMessage(fallback: String): String {
    if (isCoroutineCancellation()) return fallback
    return message?.trim()?.takeIf { it.isNotBlank() } ?: fallback
}

inline fun <T> runCatchingPreservingCancellation(block: () -> T): Result<T> = try {
    Result.success(block())
} catch (cancelled: CancellationException) {
    throw cancelled
} catch (error: Exception) {
    error.rethrowCancellation()
    Result.failure(error)
}

package com.gayadi.android.data.repository

/**
 * 원격 호출 실패를 화면에 그대로 보여줄 수 있는 오류로 바꾼다.
 *
 * DataSource가 `require`/`error`로 직접 만든 안내 문구는 이미 한국어이므로 그대로 두고,
 * Firebase SDK가 던지는 영어 원문 오류는 사용자용 문구로 감춘다.
 */
internal fun <T> Result<T>.withUserFacingMessage(fallbackMessage: String): Result<T> =
    recoverCatching { error ->
        throw when (error) {
            is IllegalArgumentException, is IllegalStateException -> error
            else -> IllegalStateException(fallbackMessage, error)
        }
    }

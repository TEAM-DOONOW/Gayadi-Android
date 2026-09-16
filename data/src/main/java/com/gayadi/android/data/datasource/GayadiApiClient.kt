package com.gayadi.android.data.datasource

import com.gayadi.android.domain.repository.AuthRepository
import com.gayadi.android.domain.error.rethrowCancellation
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject

/** Shared, cancellable backend transport. Only an explicit 401 causes one token refresh. */
class GayadiApiClient internal constructor(
    baseUrl: String,
    private val auth: AuthRepository?,
    private val client: OkHttpClient,
) {
    constructor(baseUrl: String, auth: AuthRepository? = null) : this(
        baseUrl, auth, OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .callTimeout(30, TimeUnit.SECONDS)
        .retryOnConnectionFailure(false)
        .followRedirects(false)
        .build(),
    )
    private val baseUrl = baseUrl.trimEnd('/')

    suspend fun request(
        method: String,
        path: String,
        body: String? = null,
        authenticated: Boolean = true,
    ): String {
        val token = if (authenticated) requireNotNull(auth) { "로그인이 필요해요." }.validAccessToken() else null
        return try {
            execute(method, path, body, token)
        } catch (error: GayadiApiException) {
            if (error.statusCode != 401 || !authenticated) throw error
            execute(method, path, body, requireNotNull(auth).refreshSession().accessToken)
        }
    }

    private suspend fun execute(method: String, path: String, body: String?, token: String?): String {
        require(path.startsWith("/api/v1/"))
        val requestBody = when {
            body != null -> body.toRequestBody("application/json; charset=utf-8".toMediaType())
            method in listOf("POST", "PUT", "PATCH") -> "".toRequestBody(null)
            else -> null
        }
        val request = Request.Builder().url(baseUrl + path)
            .header("Accept", "application/json")
            .apply { token?.let { header("Authorization", "Bearer $it") } }
            .method(method, requestBody).build()
        return suspendCancellableCoroutine { continuation ->
            val call = client.newCall(request)
            continuation.invokeOnCancellation { call.cancel() }
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, error: IOException) {
                    if (continuation.isActive) continuation.resumeWithException(
                        IOException("서버에 연결하지 못했어요. 잠시 후 다시 시도해 주세요.", error),
                    )
                }
                override fun onResponse(call: Call, response: Response) {
                    val result = runCatching {
                        response.use {
                            val body = it.body?.string().orEmpty()
                            if (!it.isSuccessful) throw GayadiApiException(it.code, body)
                            body
                        }
                    }
                    if (continuation.isActive) result.fold(continuation::resume, continuation::resumeWithException)
                }
            })
        }
    }
}

class GayadiApiException(
    val statusCode: Int,
    body: String = "",
) : IOException(gayadiApiErrorMessage(statusCode, body))

private fun gayadiApiErrorMessage(statusCode: Int, body: String): String {
    val serverMessage = runCatching {
        JSONObject(body).optString("message")
    }.getOrDefault("").trim()
    if (serverMessage.isNotBlank() && statusCode in 400..499) {
        return serverMessage
    }
    return when (statusCode) {
        401 -> "로그인이 만료되었어요. 다시 로그인해 주세요."
        403 -> "이 작업을 수행할 권한이 없어요."
        404 -> "요청한 정보를 찾을 수 없어요."
        409 -> "정보가 변경되었어요. 새로고침 후 다시 시도해 주세요."
        else -> "요청을 처리하지 못했어요. 잠시 후 다시 시도해 주세요. (HTTP $statusCode)"
    }
}

internal suspend fun <T> apiResult(block: suspend () -> T): Result<T> = try {
    Result.success(block())
} catch (cancelled: CancellationException) {
    throw cancelled
} catch (error: Exception) {
    error.rethrowCancellation()
    Result.failure(error)
}

package com.gayadi.android.navigation

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Base64
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialInterruptedException
import com.gayadi.android.BuildConfig
import com.gayadi.android.domain.error.isCoroutineCancellation
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

internal fun googleOauthServerClientId(webClientId: String): String {
    val web = webClientId.trim()
    check(isConfiguredGoogleClientId(web)) {
        "GOOGLE_WEB_CLIENT_ID에 Google 웹 클라이언트 ID를 설정해 주세요."
    }
    return web
}

internal fun googleLoginUserMessage(error: Throwable): String {
    if (
        error is GetCredentialCancellationException ||
        error is GetCredentialInterruptedException ||
        error.isCoroutineCancellation()
    ) {
        return GOOGLE_LOGIN_CANCELLED_MESSAGE
    }
    return GOOGLE_LOGIN_FAILED_MESSAGE
}

internal fun Context.findActivity(): Activity {
    var current: Context = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    error("Google 로그인은 Activity 화면에서만 시작할 수 있어요.")
}

internal suspend fun requestGoogleIdToken(
    context: Context,
    webClientId: String,
): String {
    val activity = context.findActivity()
    val serverClientId = googleOauthServerClientId(webClientId)
    val googleOption = GetSignInWithGoogleOption.Builder(serverClientId).build()
    val request = GetCredentialRequest.Builder()
        .addCredentialOption(googleOption)
        .build()
    val credential = CredentialManager.create(activity.applicationContext)
        .getCredential(context = activity, request = request)
        .credential
    check(
        credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL,
    ) {
        "Google 로그인 응답 형식이 올바르지 않습니다."
    }
    val idToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
    logGoogleIdTokenAudience(idToken)
    logGoogleTokenInfo(idToken)
    return idToken
}

internal fun logGoogleIdTokenAudience(idToken: String) {
    if (!BuildConfig.DEBUG) {
        return
    }
    val parts = idToken.split('.')
    val header = decodeJwtJson(parts.getOrNull(0))
    val payload = decodeJwtJson(parts.getOrNull(1)) ?: return
    Log.i(
        GOOGLE_AUTH_LOG_TAG,
        "Google token alg=${header?.opt("alg")} kid=${header?.opt("kid")} " +
            "iss=${payload.opt("iss")} aud=${payload.opt("aud")} azp=${payload.opt("azp")} " +
            "iat=${payload.opt("iat")} exp=${payload.opt("exp")}",
    )
}

private suspend fun logGoogleTokenInfo(idToken: String) {
    if (!BuildConfig.DEBUG) {
        return
    }
    withContext(Dispatchers.IO) {
        runCatching {
            val encoded = URLEncoder.encode(idToken, Charsets.UTF_8.name())
            val connection = (
                URL("https://oauth2.googleapis.com/tokeninfo?id_token=$encoded")
                    .openConnection() as HttpURLConnection
                ).apply {
                    connectTimeout = 8_000
                    readTimeout = 8_000
                }
            try {
                val statusCode = connection.responseCode
                val body = (
                    if (statusCode in 200..299) connection.inputStream else connection.errorStream
                    )?.bufferedReader()?.use { it.readText() }.orEmpty()
                val json = JSONObject(body.ifBlank { "{}" })
                Log.i(
                    GOOGLE_AUTH_LOG_TAG,
                    "Google tokeninfo HTTP $statusCode aud=${json.opt("aud")} " +
                        "azp=${json.opt("azp")} error=${json.opt("error")} " +
                        "error_description=${json.opt("error_description")}",
                )
            } finally {
                connection.disconnect()
            }
        }.onFailure { exception ->
            Log.w(GOOGLE_AUTH_LOG_TAG, "Google tokeninfo failed: ${exception.message}")
        }
    }
}

private fun decodeJwtJson(part: String?): JSONObject? {
    if (part.isNullOrBlank()) {
        return null
    }
    val padded = part + "=".repeat((4 - part.length % 4) % 4)
    return JSONObject(String(Base64.decode(padded, Base64.URL_SAFE or Base64.NO_WRAP)))
}

internal const val GOOGLE_LOGIN_CANCELLED_MESSAGE =
    "Google 로그인이 취소되었습니다. 다시 시도해 주세요."
internal const val GOOGLE_LOGIN_FAILED_MESSAGE = "Google 로그인에 실패했습니다."
private const val GOOGLE_AUTH_LOG_TAG = "GayadiGoogleAuth"

private fun isConfiguredGoogleClientId(value: String): Boolean =
    value.isNotBlank() &&
        !value.startsWith("your_") &&
        value.endsWith(".apps.googleusercontent.com")

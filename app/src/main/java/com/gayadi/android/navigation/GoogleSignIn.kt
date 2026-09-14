package com.gayadi.android.navigation

import android.content.Context
import android.util.Base64
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.gayadi.android.BuildConfig
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
        "GOOGLE_CLIENT_ID에 Google 웹 클라이언트 ID를 설정해 주세요."
    }
    return web
}

internal suspend fun requestGoogleIdToken(
    context: Context,
    webClientId: String,
): String {
    val serverClientId = googleOauthServerClientId(webClientId)
    val googleOption = GetSignInWithGoogleOption.Builder(serverClientId).build()
    val request = GetCredentialRequest.Builder()
        .addCredentialOption(googleOption)
        .build()
    val credential = CredentialManager.create(context)
        .getCredential(context = context, request = request)
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

private const val GOOGLE_AUTH_LOG_TAG = "GayadiGoogleAuth"

private fun isConfiguredGoogleClientId(value: String): Boolean =
    value.isNotBlank() &&
        !value.startsWith("your_") &&
        value.endsWith(".apps.googleusercontent.com")

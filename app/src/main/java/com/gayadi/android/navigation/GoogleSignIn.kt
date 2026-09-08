package com.gayadi.android.navigation

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

internal suspend fun requestGoogleIdToken(
    context: Context,
    webClientId: String,
): String {
    check(
        webClientId.isNotBlank() &&
            !webClientId.startsWith("your_"),
    ) {
        "GOOGLE_CLIENT_ID에 Google 웹 클라이언트 ID를 설정해 주세요."
    }

    val googleOption = GetSignInWithGoogleOption.Builder(webClientId).build()
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
    return GoogleIdTokenCredential.createFrom(credential.data).idToken
}

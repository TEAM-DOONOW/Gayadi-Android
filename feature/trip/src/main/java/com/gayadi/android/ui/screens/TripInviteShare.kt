package com.gayadi.android.ui.screens

import android.content.Context
import android.content.Intent
import android.util.Log
import com.kakao.sdk.share.ShareClient
import com.kakao.sdk.share.WebSharerClient

internal fun shareTripInviteToKakao(
    context: Context,
    tripName: String,
    cities: List<String>,
    inviteCode: String,
) {
    val templateArgs = mapOf(
        "tripName" to tripName,
        "cities" to cities.joinToString(" · "),
        "inviteCode" to inviteCode,
    )

    if (ShareClient.instance.isKakaoTalkSharingAvailable(context)) {
        ShareClient.instance.shareCustom(
            context = context,
            templateId = KAKAO_INVITE_TEMPLATE_ID,
            templateArgs = templateArgs,
        ) { sharingResult, error ->
            if (error != null) {
                Log.e(TAG, "KakaoTalk invite sharing failed", error)
                return@shareCustom
            }
            sharingResult?.let { context.startActivity(it.intent) }
        }
        return
    }

    val sharingUrl = WebSharerClient.instance.makeCustomUrl(
        templateId = KAKAO_INVITE_TEMPLATE_ID,
        templateArgs = templateArgs,
    )
    context.startActivity(Intent(Intent.ACTION_VIEW, sharingUrl))
}

private const val KAKAO_INVITE_TEMPLATE_ID = 136168L
private const val TAG = "TripInviteShare"

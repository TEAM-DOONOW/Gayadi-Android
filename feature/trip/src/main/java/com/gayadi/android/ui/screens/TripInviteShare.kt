package com.gayadi.android.ui.screens

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent

internal fun shareTripInviteToKakao(
    context: Context,
    tripName: String,
    cities: List<String>,
    inviteCode: String,
) {
    val message = buildString {
        appendLine("가야디 여행에 초대해요!")
        appendLine()
        appendLine("여행 이름: $tripName")
        appendLine("여행지: ${cities.joinToString(" · ")}")
        appendLine("초대 코드: $inviteCode")
        appendLine()
        append("가야디 앱에서 초대 코드를 입력해 참여해 주세요.")
    }
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "$tripName 여행 초대")
        putExtra(Intent.EXTRA_TEXT, message)
        setPackage("com.kakao.talk")
    }
    try {
        context.startActivity(shareIntent)
    } catch (_: ActivityNotFoundException) {
        shareIntent.setPackage(null)
        context.startActivity(Intent.createChooser(shareIntent, "여행 초대 공유하기"))
    }
}

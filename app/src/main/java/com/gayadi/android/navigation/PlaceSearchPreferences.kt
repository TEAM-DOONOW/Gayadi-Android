package com.gayadi.android.navigation

import android.content.Context

/** 장소찾기에서 사용자가 고른 여행지 연계 여부. 다음 방문에도 같은 방식으로 추천한다. */
internal object PlaceSearchPreferences {
    private const val PreferencesName = "gayadi-place-search"
    private const val LinkModeKey = "link-mode"

    fun linkMode(context: Context): Boolean =
        context.getSharedPreferences(PreferencesName, Context.MODE_PRIVATE).getBoolean(LinkModeKey, true)

    fun setLinkMode(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PreferencesName, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(LinkModeKey, enabled)
            .apply()
    }
}

package com.gayadi.android.navigation

import android.content.Context

internal object UsageGuidePreferences {
    private const val PreferencesName = "gayadi-usage-guide"
    const val PlaceSearch = "place-search-v1"
    const val PlaceDetail = "place-detail-v1"
    const val MyTrip = "my-trip-v2"
    const val GroupDate = "group-date-v1"
    const val TripHome = "trip-home-v1"
    const val ScheduleActions = "schedule-actions-v1"

    fun hasCompleted(context: Context, guideKey: String): Boolean =
        context.getSharedPreferences(PreferencesName, Context.MODE_PRIVATE)
            .getBoolean(guideKey, false)

    fun markCompleted(context: Context, guideKey: String) {
        context.getSharedPreferences(PreferencesName, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(guideKey, true)
            .apply()
    }
}

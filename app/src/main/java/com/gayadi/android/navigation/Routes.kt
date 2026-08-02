package com.gayadi.android.navigation

object Routes {
    const val LOGIN = "login"
    const val BASIC_INFO = "basic_info"
    const val SURVEY = "survey"
    const val SURVEY_RESULT = "survey_result/{resultCode}"
    const val FRIEND_ADD = "friend_add"
    const val PLACE_SEARCH = "place_search"
    const val PLACE_DETAIL = "place_detail/{placeId}"
    const val MY_TRIP = "my_trip"
    const val REALTIME_HOME = "realtime_home"
    const val MY_PAGE = "my_page"
    const val SETTINGS = "settings"

    fun placeDetail(placeId: String) = "place_detail/$placeId"
    fun surveyResult(resultCode: String) = "survey_result/$resultCode"
}

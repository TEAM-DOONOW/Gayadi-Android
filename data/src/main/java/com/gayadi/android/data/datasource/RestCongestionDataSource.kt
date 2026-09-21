package com.gayadi.android.data.datasource

import com.gayadi.android.domain.model.CongestionHourlyForecast
import com.gayadi.android.domain.model.CongestionHourlyPoint
import java.net.URLEncoder
import org.json.JSONObject

/** Reads the backend hourly congestion forecast. Existing single-shot forecast callers stay untouched. */
class RestCongestionDataSource(
    private val api: GayadiApiClient,
) {
    suspend fun getHourlyForecast(
        areaCode: String,
        districtCode: String,
        areaName: String = "",
        placeName: String = "",
        targetAt: String = "",
        hours: List<Int>? = null,
    ): CongestionHourlyForecast {
        require(areaCode.matches(AREA_CODE_PATTERN)) { "시도 코드 2자리가 필요합니다." }
        require(districtCode.matches(DISTRICT_CODE_PATTERN)) { "시군구 코드 3자리 또는 5자리가 필요합니다." }
        val query = buildString {
            append("?areaCode=").append(areaCode.urlEncoded())
            append("&districtCode=").append(districtCode.urlEncoded())
            if (areaName.isNotBlank()) append("&areaName=").append(areaName.urlEncoded())
            if (placeName.isNotBlank()) append("&placeName=").append(placeName.urlEncoded())
            if (targetAt.isNotBlank()) append("&targetAt=").append(targetAt.urlEncoded())
            hours?.takeIf(List<Int>::isNotEmpty)?.forEach { hour ->
                append("&hours=").append(hour)
            }
        }
        val root = JSONObject(api.request("GET", "$HOURLY_PATH$query"))
        val points = buildList {
            val items = root.optJSONArray("points") ?: return@buildList
            repeat(items.length()) { index ->
                val item = items.getJSONObject(index)
                add(
                    CongestionHourlyPoint(
                        hour = item.optInt("hour"),
                        concentrationScore = item.optInt("concentrationScore"),
                        level = item.optString("level"),
                    ),
                )
            }
        }
        return CongestionHourlyForecast(
            area = root.optString("area"),
            placeName = root.optString("placeName"),
            targetDate = root.optString("targetDate"),
            baseLevel = root.optString("baseLevel"),
            baseScore = root.optNullableInt("baseScore"),
            source = root.optString("source"),
            estimated = root.optBoolean("estimated", true),
            providerDataAvailable = root.optBoolean("providerDataAvailable"),
            confidence = root.optString("confidence"),
            message = root.optString("message"),
            points = points,
        )
    }

    private fun JSONObject.optNullableInt(key: String): Int? =
        if (!has(key) || isNull(key)) null else optInt(key)

    private fun String.urlEncoded(): String =
        URLEncoder.encode(this, Charsets.UTF_8.name())

    private companion object {
        const val HOURLY_PATH = "/api/v1/congestion/forecast/hourly"
        val AREA_CODE_PATTERN = Regex("\\d{2}")
        val DISTRICT_CODE_PATTERN = Regex("\\d{3}|\\d{5}")
    }
}

package com.gayadi.android.data.mapper

import com.gayadi.android.domain.repository.WeatherResult
import org.json.JSONObject

internal fun JSONObject.toWeatherResult(): WeatherResult {
    val value = optJSONObject("weather") ?: this
    fun text(key: String): String? = if (value.isNull(key)) null else
        value.optString(key).trim().takeIf { it.isNotEmpty() && it != "null" }
    // 장소 요약 weather는 baseDate/baseTime 대신 observedAt(ISO-8601)을 줍니다.
    val observedAt = text("observedAt")?.takeIf { it.length >= 16 }
    return WeatherResult(
        baseDate = text("baseDate") ?: observedAt?.substring(0, 10)?.replace("-", "").orEmpty(),
        baseTime = text("baseTime") ?: observedAt?.substring(11, 16)?.replace(":", "").orEmpty(),
        temperature = (text("temperatureCelsius") ?: text("temperature"))
            ?.takeIf { it.toDoubleOrNull()?.isFinite() == true },
        forecastSlotCount = value.optJSONArray("forecast")?.length() ?: 0,
        available = value.optBoolean("available", true),
        condition = text("condition"),
        precipitationProbability = text("precipitationProbability")?.toIntOrNull()?.takeIf { it in 0..100 },
    )
}

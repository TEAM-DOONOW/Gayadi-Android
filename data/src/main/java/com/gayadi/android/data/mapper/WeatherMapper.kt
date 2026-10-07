package com.gayadi.android.data.mapper

import com.gayadi.android.domain.repository.WeatherResult
import org.json.JSONObject

internal fun JSONObject.toWeatherResult(): WeatherResult {
    val value = optJSONObject("weather") ?: this
    fun text(key: String): String? = if (value.isNull(key)) null else
        value.optString(key).trim().takeIf { it.isNotEmpty() && it != "null" }
    return WeatherResult(
        baseDate = text("baseDate").orEmpty(),
        baseTime = text("baseTime").orEmpty(),
        temperature = (text("temperatureCelsius") ?: text("temperature"))
            ?.takeIf { it.toDoubleOrNull()?.isFinite() == true },
        forecastSlotCount = value.optJSONArray("forecast")?.length() ?: 0,
        available = value.optBoolean("available", true),
        condition = text("condition"),
        precipitationProbability = text("precipitationProbability")?.toIntOrNull()?.takeIf { it in 0..100 },
    )
}

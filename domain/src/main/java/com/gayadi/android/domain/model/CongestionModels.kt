package com.gayadi.android.domain.model

import com.gayadi.android.domain.repository.CongestionResult
import com.gayadi.android.domain.repository.WeatherResult

data class CongestionHourlyPoint(
    val hour: Int,
    val concentrationScore: Int,
    val level: String = "",
)

/** 저장 장소 번호 하나로 받은 현재 날씨와 시간대 혼잡입니다. */
data class PlaceCongestionContext(
    val weather: WeatherResult? = null,
    val hourly: CongestionHourlyForecast? = null,
    val daily: CongestionResult? = null,
)

data class CongestionHourlyForecast(
    val area: String = "",
    val placeName: String = "",
    val targetDate: String = "",
    val baseLevel: String = "",
    val baseScore: Int? = null,
    val source: String = "",
    val estimated: Boolean = true,
    val providerDataAvailable: Boolean = false,
    val confidence: String = "",
    val message: String = "",
    val points: List<CongestionHourlyPoint> = emptyList(),
)

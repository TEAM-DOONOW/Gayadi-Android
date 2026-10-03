package com.gayadi.android.domain.model

data class CongestionHourlyPoint(
    val hour: Int,
    val concentrationScore: Int,
    val level: String = "",
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

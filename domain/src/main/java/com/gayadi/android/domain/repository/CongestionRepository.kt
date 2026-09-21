package com.gayadi.android.domain.repository

import com.gayadi.android.domain.model.CongestionHourlyForecast

interface CongestionRepository {
    suspend fun getHourlyForecast(
        areaCode: String,
        districtCode: String,
        areaName: String = "",
        placeName: String = "",
        targetAt: String = "",
        hours: List<Int>? = null,
    ): Result<CongestionHourlyForecast>
}

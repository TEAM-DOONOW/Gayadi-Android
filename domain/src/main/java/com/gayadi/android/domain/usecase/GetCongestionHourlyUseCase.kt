package com.gayadi.android.domain.usecase

import com.gayadi.android.domain.model.CongestionHourlyForecast
import com.gayadi.android.domain.repository.CongestionRepository

class GetCongestionHourlyUseCase(
    private val repository: CongestionRepository,
) {
    suspend operator fun invoke(
        areaCode: String,
        districtCode: String,
        areaName: String = "",
        placeName: String = "",
        targetAt: String = "",
        hours: List<Int>? = null,
    ): Result<CongestionHourlyForecast> = repository.getHourlyForecast(
        areaCode = areaCode,
        districtCode = districtCode,
        areaName = areaName,
        placeName = placeName,
        targetAt = targetAt,
        hours = hours,
    )
}

package com.gayadi.android.data.repository

import com.gayadi.android.data.datasource.RestCongestionDataSource
import com.gayadi.android.data.datasource.apiResult
import com.gayadi.android.domain.model.CongestionHourlyForecast
import com.gayadi.android.domain.repository.CongestionRepository
import kotlinx.coroutines.CancellationException

class DefaultCongestionRepository(
    private val dataSource: RestCongestionDataSource,
) : CongestionRepository {
    override suspend fun getHourlyForecast(
        areaCode: String,
        districtCode: String,
        areaName: String,
        placeName: String,
        targetAt: String,
        hours: List<Int>?,
    ): Result<CongestionHourlyForecast> = try {
        apiResult {
            dataSource.getHourlyForecast(
                areaCode = areaCode,
                districtCode = districtCode,
                areaName = areaName,
                placeName = placeName,
                targetAt = targetAt,
                hours = hours,
            )
        }
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (error: Exception) {
        Result.failure(error)
    }
}

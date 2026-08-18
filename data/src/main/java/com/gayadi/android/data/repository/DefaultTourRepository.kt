package com.gayadi.android.data.repository

import com.gayadi.android.data.datasource.TourApiDataSource
import com.gayadi.android.data.mapper.toDomain
import com.gayadi.android.domain.model.TourPlace
import com.gayadi.android.domain.repository.TourRepository

class DefaultTourRepository(
    private val dataSource: TourApiDataSource,
) : TourRepository {
    private val cache = mutableMapOf<Pair<Int, Int>, List<TourPlace>>()

    override suspend fun getPlaces(
        numOfRows: Int,
        contentTypeId: Int,
    ): Result<List<TourPlace>> = runCatching {
        val cacheKey = numOfRows to contentTypeId
        cache[cacheKey] ?: dataSource.getPlaces(numOfRows, contentTypeId)
            .map { it.toDomain() }
            .also { cache[cacheKey] = it }
    }
}

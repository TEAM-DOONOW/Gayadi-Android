package com.gayadi.android.data.repository

import com.gayadi.android.data.datasource.TourApiDataSource
import com.gayadi.android.data.mapper.toDomain
import com.gayadi.android.domain.model.TourPlace
import com.gayadi.android.domain.repository.TourRepository
import kotlinx.coroutines.CancellationException

class DefaultTourRepository(
    private val dataSource: TourApiDataSource,
) : TourRepository {
    private val cache = mutableMapOf<TourPlacesCacheKey, List<TourPlace>>()

    override suspend fun getPlaces(
        numOfRows: Int,
        contentTypeId: Int,
        lclsSystm1: String?,
        lclsSystm2: String?,
        lclsSystm3: String?,
        maxPages: Int?,
    ): Result<List<TourPlace>> = try {
        val cacheKey = TourPlacesCacheKey(
            numOfRows = numOfRows,
            contentTypeId = contentTypeId,
            lclsSystm1 = lclsSystm1,
            lclsSystm2 = lclsSystm2,
            lclsSystm3 = lclsSystm3,
            maxPages = maxPages,
        )
        Result.success(
            cache[cacheKey] ?: dataSource.getPlaces(
                numOfRows = numOfRows,
                contentTypeId = contentTypeId,
                lclsSystm1 = lclsSystm1,
                lclsSystm2 = lclsSystm2,
                lclsSystm3 = lclsSystm3,
                maxPages = maxPages,
            ).map { it.toDomain() }
                .also { cache[cacheKey] = it },
        )
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (error: Throwable) {
        Result.failure(error)
    }
}

private data class TourPlacesCacheKey(
    val numOfRows: Int,
    val contentTypeId: Int,
    val lclsSystm1: String?,
    val lclsSystm2: String?,
    val lclsSystm3: String?,
    val maxPages: Int?,
)

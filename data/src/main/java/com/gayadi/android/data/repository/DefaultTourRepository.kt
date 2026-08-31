package com.gayadi.android.data.repository

import com.gayadi.android.data.datasource.TourApiDataSource
import com.gayadi.android.data.mapper.toDomain
import com.gayadi.android.domain.model.TourPlace
import com.gayadi.android.domain.repository.TourRepository
import com.gayadi.android.domain.repository.NearbyTourRepository
import com.gayadi.android.domain.repository.KeywordTourRepository
import kotlinx.coroutines.CancellationException

class DefaultTourRepository(
    private val dataSource: TourApiDataSource,
) : TourRepository, NearbyTourRepository, KeywordTourRepository {
    private val cache = mutableMapOf<TourPlacesCacheKey, List<TourPlace>>()

    override suspend fun getPlaces(
        pageSize: Int,
        contentTypeId: Int,
        lclsSystm1: String?,
        lclsSystm2: String?,
        lclsSystm3: String?,
        maxPages: Int?,
    ): Result<List<TourPlace>> = try {
        val cacheKey = TourPlacesCacheKey(
            pageSize = pageSize,
            contentTypeId = contentTypeId,
            lclsSystm1 = lclsSystm1,
            lclsSystm2 = lclsSystm2,
            lclsSystm3 = lclsSystm3,
            maxPages = maxPages,
        )
        Result.success(
            cache[cacheKey] ?: dataSource.getPlaces(
                pageSize = pageSize,
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

    override suspend fun getNearbyPlaces(
        pageSize: Int,
        mapX: String,
        mapY: String,
        radius: Int,
        arrange: String,
        contentTypeId: String?,
        maxPages: Int?,
    ): Result<List<TourPlace>> = try {
        Result.success(
            dataSource.getNearbyPlaces(
                pageSize = pageSize,
                mapX = mapX,
                mapY = mapY,
                radius = radius,
                arrange = arrange,
                contentTypeId = contentTypeId,
                maxPages = maxPages,
            ).map { it.toDomain() },
        )
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (error: Throwable) {
        Result.failure(error)
    }

    override suspend fun searchPlaces(
        pageSize: Int,
        keyword: String,
        arrange: String,
        lDongRegnCd: String?,
        lDongSignguCd: String?,
        lclsSystm1: String?,
        lclsSystm2: String?,
        lclsSystm3: String?,
        maxPages: Int?,
    ): Result<List<TourPlace>> = try {
        Result.success(
            dataSource.searchPlaces(
                pageSize = pageSize,
                keyword = keyword,
                arrange = arrange,
                lDongRegnCd = lDongRegnCd,
                lDongSignguCd = lDongSignguCd,
                lclsSystm1 = lclsSystm1,
                lclsSystm2 = lclsSystm2,
                lclsSystm3 = lclsSystm3,
                maxPages = maxPages,
            ).map { it.toDomain() },
        )
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (error: Throwable) {
        Result.failure(error)
    }
}

private data class TourPlacesCacheKey(
    val pageSize: Int,
    val contentTypeId: Int,
    val lclsSystm1: String?,
    val lclsSystm2: String?,
    val lclsSystm3: String?,
    val maxPages: Int?,
)

package com.gayadi.android.data.datasource

import com.gayadi.android.data.model.TourPlaceDto

/**
 * Loads the initial regional catalogue from the live TourAPI-backed endpoint.
 * Keyword and nearby queries use the canonical place catalogue populated by that discovery call,
 * so every ID returned to schedule and favorite flows is a Gayadi server place ID.
 */
class DiscoveryPlaceApiDataSource(
    private val discovery: TourApiDataSource,
    private val canonical: TourApiDataSource,
) : TourApiDataSource {
    override suspend fun getPlaces(
        pageSize: Int,
        contentTypeId: Int,
        lclsSystm1: String?,
        lclsSystm2: String?,
        lclsSystm3: String?,
        maxPages: Int?,
        regionName: String?,
    ): List<TourPlaceDto> = discovery.getPlaces(
        pageSize, contentTypeId, lclsSystm1, lclsSystm2, lclsSystm3, maxPages, regionName,
    )

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
    ): List<TourPlaceDto> = canonical.searchPlaces(
        pageSize, keyword, arrange, lDongRegnCd, lDongSignguCd,
        lclsSystm1, lclsSystm2, lclsSystm3, maxPages,
    )

    override suspend fun getNearbyPlaces(
        pageSize: Int,
        mapX: String,
        mapY: String,
        radius: Int,
        arrange: String,
        contentTypeId: String?,
        maxPages: Int?,
    ): List<TourPlaceDto> = canonical.getNearbyPlaces(
        pageSize, mapX, mapY, radius, arrange, contentTypeId, maxPages,
    )
}

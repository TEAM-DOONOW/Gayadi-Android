package com.gayadi.android.domain.repository

import com.gayadi.android.domain.model.TourPlace

interface NearbyTourRepository {
    suspend fun getNearbyPlaces(
        pageSize: Int,
        mapX: String,
        mapY: String,
        radius: Int,
        arrange: String = "E",
        contentTypeId: String? = null,
        maxPages: Int? = null,
    ): Result<List<TourPlace>>
}

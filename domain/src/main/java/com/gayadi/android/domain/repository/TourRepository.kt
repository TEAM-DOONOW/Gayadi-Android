package com.gayadi.android.domain.repository

import com.gayadi.android.domain.model.TourPlace

interface TourRepository {
    suspend fun getPlaces(
        pageSize: Int,
        contentTypeId: Int,
        lclsSystm1: String? = null,
        lclsSystm2: String? = null,
        lclsSystm3: String? = null,
        maxPages: Int? = null,
    ): Result<List<TourPlace>>
}

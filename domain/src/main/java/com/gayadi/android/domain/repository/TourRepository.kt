package com.gayadi.android.domain.repository

import com.gayadi.android.domain.model.TourPlace

interface TourRepository {
    suspend fun getPlaces(pageSize: Int, contentTypeId: Int): Result<List<TourPlace>>
}

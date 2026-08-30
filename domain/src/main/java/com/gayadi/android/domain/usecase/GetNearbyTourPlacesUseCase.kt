package com.gayadi.android.domain.usecase

import com.gayadi.android.domain.model.TourPlace
import com.gayadi.android.domain.repository.NearbyTourRepository

class GetNearbyTourPlacesUseCase(
    private val repository: NearbyTourRepository,
) {
    suspend operator fun invoke(
        pageSize: Int = 10,
        mapX: String,
        mapY: String,
        radius: Int = 2_000,
        arrange: String = "E",
        contentTypeId: String? = null,
        maxPages: Int? = 1,
    ): Result<List<TourPlace>> = repository.getNearbyPlaces(
        pageSize = pageSize,
        mapX = mapX,
        mapY = mapY,
        radius = radius,
        arrange = arrange,
        contentTypeId = contentTypeId,
        maxPages = maxPages,
    )
}

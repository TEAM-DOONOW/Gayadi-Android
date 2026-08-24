package com.gayadi.android.domain.usecase

import com.gayadi.android.domain.model.TourPlace
import com.gayadi.android.domain.repository.TourRepository

class GetTourPlacesUseCase(
    private val repository: TourRepository,
) {
    suspend operator fun invoke(
        pageSize: Int = 100,
        contentTypeId: Int = 12,
        lclsSystm1: String? = null,
        lclsSystm2: String? = null,
        lclsSystm3: String? = null,
        maxPages: Int? = null,
    ): Result<List<TourPlace>> = repository.getPlaces(
        pageSize = pageSize,
        contentTypeId = contentTypeId,
        lclsSystm1 = lclsSystm1,
        lclsSystm2 = lclsSystm2,
        lclsSystm3 = lclsSystm3,
        maxPages = maxPages,
    )
}

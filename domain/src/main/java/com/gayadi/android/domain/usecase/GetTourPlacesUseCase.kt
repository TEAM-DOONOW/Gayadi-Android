package com.gayadi.android.domain.usecase

import com.gayadi.android.domain.model.TourPlace
import com.gayadi.android.domain.repository.TourRepository

class GetTourPlacesUseCase(
    private val repository: TourRepository,
) {
    suspend operator fun invoke(
        numOfRows: Int = 100,
        contentTypeId: Int = 12,
    ): Result<List<TourPlace>> = repository.getPlaces(numOfRows, contentTypeId)
}

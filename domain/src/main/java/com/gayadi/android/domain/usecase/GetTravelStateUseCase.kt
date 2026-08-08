package com.gayadi.android.domain.usecase

import com.gayadi.android.domain.repository.TravelRepository

class GetTravelStateUseCase(private val repository: TravelRepository) {
    suspend operator fun invoke() = repository.getTravelState()
}

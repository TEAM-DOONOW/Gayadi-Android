package com.gayadi.android.domain.usecase

import com.gayadi.android.domain.model.TravelState
import com.gayadi.android.domain.repository.TravelRepository

class SaveTravelStateUseCase(private val repository: TravelRepository) {
    suspend operator fun invoke(state: TravelState) = repository.saveTravelState(state)
}

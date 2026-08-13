package com.gayadi.android.domain.usecase

import com.gayadi.android.domain.model.TravelState
import com.gayadi.android.domain.repository.TravelRepository

/** Atomically applies one transformation to the locally persisted travel aggregate. */
class UpdateTravelStateUseCase(
    private val repository: TravelRepository,
) {
    suspend operator fun invoke(
        transform: (TravelState) -> TravelState,
    ): Result<TravelState> = repository.updateTravelState(transform)
}

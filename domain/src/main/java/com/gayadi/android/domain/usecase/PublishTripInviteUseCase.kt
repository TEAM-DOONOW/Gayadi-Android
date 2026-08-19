package com.gayadi.android.domain.usecase

import com.gayadi.android.domain.model.TravelParticipant
import com.gayadi.android.domain.model.TravelTrip
import com.gayadi.android.domain.repository.TripInviteRepository

class PublishTripInviteUseCase(
    private val repository: TripInviteRepository,
) {
    suspend operator fun invoke(trip: TravelTrip, owner: TravelParticipant): Result<Unit> =
        repository.publish(trip, owner)
}

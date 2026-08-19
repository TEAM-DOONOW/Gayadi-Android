package com.gayadi.android.domain.repository

import com.gayadi.android.domain.model.SharedTripInvite
import com.gayadi.android.domain.model.TravelParticipant
import com.gayadi.android.domain.model.TravelTrip

interface TripInviteRepository {
    suspend fun publish(trip: TravelTrip, owner: TravelParticipant): Result<Unit>
    suspend fun join(inviteCode: String, participant: TravelParticipant): Result<SharedTripInvite>
}

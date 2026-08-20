package com.gayadi.android.domain.repository

import com.gayadi.android.domain.model.SharedTripInvite
import com.gayadi.android.domain.model.TravelParticipant
import com.gayadi.android.domain.model.TravelTrip
import kotlinx.coroutines.flow.Flow

interface TripInviteRepository {
    suspend fun publish(trip: TravelTrip, owner: TravelParticipant): Result<Unit>
    suspend fun join(inviteCode: String, participant: TravelParticipant): Result<SharedTripInvite>
    fun observe(inviteCode: String): Flow<Result<SharedTripInvite>>
    suspend fun submitAvailability(inviteCode: String, dates: List<String>): Result<Unit>
    suspend fun finalizeDates(inviteCode: String, startDate: String, endDate: String): Result<Unit>
}

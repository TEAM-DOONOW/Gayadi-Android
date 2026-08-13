package com.gayadi.android.domain.usecase

import com.gayadi.android.domain.model.TravelParticipant
import com.gayadi.android.domain.model.TravelTrip
import com.gayadi.android.domain.repository.TravelRepository

class JoinTripByInviteCodeUseCase(
    private val repository: TravelRepository,
) {
    suspend operator fun invoke(code: String, participant: TravelParticipant): Result<TravelTrip> {
        val normalizedCode = code.trim()
        return repository.updateTravelState { state ->
            val trip = state.trips.firstOrNull {
                it.inviteCode.equals(normalizedCode, ignoreCase = true)
            } ?: throw IllegalArgumentException("유효하지 않은 초대 코드예요")
            val updatedTrip = trip.copy(
                participantIds = (trip.participantIds + participant.id).distinct(),
            )
            state.copy(
                trips = state.trips.map { if (it.id == trip.id) updatedTrip else it },
                participants = (state.participants + participant).distinctBy(TravelParticipant::id),
            )
        }.mapCatching { state ->
            state.trips.first {
                it.inviteCode.equals(normalizedCode, ignoreCase = true) &&
                    participant.id in it.participantIds
            }
        }
    }
}

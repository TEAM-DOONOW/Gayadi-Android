package com.gayadi.android.domain.usecase

import com.gayadi.android.domain.model.TravelParticipant
import com.gayadi.android.domain.model.TravelTrip
import com.gayadi.android.domain.repository.TravelRepository

class JoinTripByInviteCodeUseCase(
    private val repository: TravelRepository,
) {
    suspend operator fun invoke(code: String, participant: TravelParticipant): Result<TravelTrip> {
        val state = repository.getTravelState().getOrElse { return Result.failure(it) }
        val trip = state.trips.firstOrNull { it.inviteCode.equals(code, ignoreCase = true) }
            ?: return Result.failure(IllegalArgumentException("유효하지 않은 초대 코드예요"))
        val updatedTrip = trip.copy(participantIds = (trip.participantIds + participant.id).distinct())
        val updatedState = state.copy(
            trips = state.trips.map { if (it.id == trip.id) updatedTrip else it },
            participants = (state.participants + participant).distinctBy(TravelParticipant::id),
        )
        return repository.saveTravelState(updatedState).map { updatedTrip }
    }
}

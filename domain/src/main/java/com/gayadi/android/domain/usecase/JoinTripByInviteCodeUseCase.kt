package com.gayadi.android.domain.usecase

import com.gayadi.android.domain.model.TravelParticipant
import com.gayadi.android.domain.model.TravelTrip
import com.gayadi.android.domain.repository.TripInviteRepository
import com.gayadi.android.domain.repository.TravelRepository
import com.gayadi.android.domain.repository.TravelGateway

class JoinTripByInviteCodeUseCase(
    private val repository: TravelRepository,
    private val remoteInvites: TripInviteRepository? = null,
    private val travelGateway: TravelGateway? = null,
) {
    suspend operator fun invoke(code: String, participant: TravelParticipant): Result<TravelTrip> {
        val normalizedCode = code.trim()
        if (travelGateway != null) {
            return try {
                val membership = travelGateway.joinTrip(normalizedCode)
                val joinedTrip = membership.trip
                repository.updateTravelState { state ->
                    state.copy(
                        trips = state.trips.filterNot { it.id == joinedTrip.id } + joinedTrip,
                        participants = (state.participants + membership.participant)
                            .distinctBy(TravelParticipant::id),
                        selectedTripId = joinedTrip.id,
                    )
                }.getOrThrow()
                Result.success(joinedTrip)
            } catch (cancellation: kotlinx.coroutines.CancellationException) {
                throw cancellation
            } catch (error: Throwable) {
                Result.failure(error)
            }
        }
        if (remoteInvites != null) {
            return remoteInvites.join(normalizedCode, participant).mapCatching { shared ->
                val joinedTrip = shared.trip.copy(
                    participantIds = shared.participants.map(TravelParticipant::id),
                )
                repository.updateTravelState { state ->
                    state.copy(
                        trips = state.trips.filterNot { it.id == joinedTrip.id } + joinedTrip,
                        participants = (state.participants + shared.participants).distinctBy(TravelParticipant::id),
                        selectedTripId = joinedTrip.id,
                    )
                }.getOrThrow()
                joinedTrip
            }
        }
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

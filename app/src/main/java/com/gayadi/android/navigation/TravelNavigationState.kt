package com.gayadi.android.navigation

import com.gayadi.android.domain.model.TravelParticipant
import com.gayadi.android.domain.model.TravelState

internal fun TravelState.trip(tripId: String) = trips.find { it.id == tripId }

internal fun TravelState.schedulesForTrip(tripId: String) =
    schedules.filter { it.tripId == tripId }.sortedBy { it.order }

internal fun TravelState.participantsForTrip(
    tripId: String,
    candidates: List<TravelParticipant>,
): List<TravelParticipant> {
    val participantIds = trip(tripId)?.participantIds.orEmpty()
    return (participants + candidates)
        .distinctBy(TravelParticipant::id)
        .filter { it.id in participantIds }
}

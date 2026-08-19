package com.gayadi.android.domain.model

data class SharedTripInvite(
    val trip: TravelTrip,
    val participants: List<TravelParticipant>,
)

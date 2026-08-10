package com.gayadi.android.domain.model

enum class TripStatus { PLANNING, ONGOING, COMPLETED }

data class TravelTrip(
    val id: String,
    val name: String,
    val startDate: String,
    val endDate: String,
    val cities: List<String>,
    val coverImageResList: List<Int> = emptyList(),
    val status: TripStatus = TripStatus.PLANNING,
    val participantIds: List<String> = emptyList(),
    val inviteCode: String = "",
)

data class TravelParticipant(
    val id: String,
    val nickname: String,
    val characterKey: String? = null,
)

enum class InvitationStatus { PENDING, ACCEPTED, DECLINED, CANCELLED }

data class TravelInvitation(
    val id: String,
    val tripId: String,
    val code: String,
    val inviteeId: String,
    val status: InvitationStatus = InvitationStatus.PENDING,
)

enum class ScheduleType { MAIN, ALTERNATIVE }

data class TravelSchedule(
    val id: String,
    val tripId: String,
    val title: String,
    val placeId: String? = null,
    val date: String,
    val time: String,
    val type: ScheduleType = ScheduleType.MAIN,
    val order: Int,
    val isVisited: Boolean = false,
)

data class TravelState(
    val trips: List<TravelTrip> = emptyList(),
    val participants: List<TravelParticipant> = emptyList(),
    val invitations: List<TravelInvitation> = emptyList(),
    val schedules: List<TravelSchedule> = emptyList(),
    val favoritePlaceIds: Set<String> = emptySet(),
    val appliedRouteIds: Map<String, String> = emptyMap(),
    val selectedTripId: String? = null,
)

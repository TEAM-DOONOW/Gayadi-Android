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
    val isGroupTrip: Boolean = false,
    val dateAvailability: Map<String, List<String>> = emptyMap(),
    val ownerId: String = "",
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
    val endTime: String? = null,
    val memo: String = "",
)

/** One manually entered expense associated with a travel schedule. */
enum class ExpenseCategory { TOUR, MUSEUM, ACTIVITY, SHOPPING, FOOD, LODGING, TRANSPORT, FLIGHT, OTHER }

enum class ExpensePaymentSource { PERSONAL, SHARED_FUND }

data class TravelExpense(
    val id: String,
    val tripId: String,
    val scheduleId: String,
    val title: String,
    val memo: String = "",
    val amount: Long,
    val payerId: String,
    val participantIds: List<String>,
    val date: String,
    val time: String,
    val category: ExpenseCategory = ExpenseCategory.OTHER,
    val paymentSource: ExpensePaymentSource = ExpensePaymentSource.PERSONAL,
    val receiptImageUri: String? = null,
)

/** Per-person totals used by the trip ledger. Positive net amounts are receivable. */
data class ParticipantExpenseBalance(
    val participantId: String,
    val paidAmount: Long,
    val owedAmount: Long,
    val netAmount: Long,
)

/** One suggested payment from an original debtor to an original creditor. */
data class SettlementTransfer(
    val fromParticipantId: String,
    val toParticipantId: String,
    val amount: Long,
)

data class ExpenseSettlementSummary(
    val totalAmount: Long,
    val balances: List<ParticipantExpenseBalance>,
    val transfers: List<SettlementTransfer>,
)

/** Stable identifier used for the device-local user until server authentication provides one. */
const val LOCAL_CURRENT_USER_ID = "local-user"

data class TravelState(
    val trips: List<TravelTrip> = emptyList(),
    val participants: List<TravelParticipant> = emptyList(),
    val invitations: List<TravelInvitation> = emptyList(),
    val schedules: List<TravelSchedule> = emptyList(),
    val favoritePlaceIds: Set<String> = emptySet(),
    val appliedRouteIds: Map<String, String> = emptyMap(),
    val selectedTripId: String? = null,
    val expenses: List<TravelExpense> = emptyList(),
    val sharedFundAmounts: Map<String, Long> = emptyMap(),
    val currentUserId: String = LOCAL_CURRENT_USER_ID,
)

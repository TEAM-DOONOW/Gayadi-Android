package com.gayadi.android.domain.repository

import com.gayadi.android.domain.model.ExpenseSettlementSummary
import com.gayadi.android.domain.model.ScheduleType
import com.gayadi.android.domain.model.TravelExpense
import com.gayadi.android.domain.model.TravelInvitation
import com.gayadi.android.domain.model.TravelParticipant
import com.gayadi.android.domain.model.TravelSchedule
import com.gayadi.android.domain.model.TravelTrip
import com.gayadi.android.domain.model.TripStatus
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** Server-backed operations for the travel aggregate. */
interface TravelGateway {
    suspend fun listTrips(status: TripStatus? = null, limit: Int = 30, offset: Int = 0): List<TravelTrip>
    suspend fun createTrip(command: CreateTripCommand): TravelTrip
    suspend fun getTrip(tripId: String): TravelTrip
    suspend fun updateTrip(tripId: String, command: UpdateTripCommand): TravelTrip
    suspend fun deleteTrip(tripId: String)
    suspend fun updateTripStatus(tripId: String, status: TripStatus): TravelTrip

    suspend fun listParticipants(tripId: String): List<TravelParticipant>
    suspend fun addParticipant(
        tripId: String,
        participantUserId: String,
        settings: ParticipantSettings = ParticipantSettings(),
    ): TravelParticipant
    suspend fun removeParticipant(tripId: String, participantUserId: String)

    suspend fun listInvitations(tripId: String, limit: Int = 30, offset: Int = 0): List<TravelInvitation>
    suspend fun createInvitation(
        tripId: String,
        inviteeUserId: String,
        expiresAt: String? = null,
    ): TravelInvitation
    suspend fun updateInvitationStatus(
        tripId: String,
        invitationId: String,
        decision: InvitationDecision,
    ): TravelInvitation
    suspend fun joinTrip(
        inviteCode: String,
        settings: ParticipantSettings = ParticipantSettings(),
    ): TripMembership

    suspend fun getDateCoordination(tripId: String): DateCoordinationSnapshot
    suspend fun submitDateAvailability(tripId: String, dates: List<String>): DateCoordinationSnapshot
    suspend fun finalizeTripDates(tripId: String, startDate: String, endDate: String): DateCoordinationSnapshot

    suspend fun listSchedules(tripId: String): List<TravelSchedule>
    suspend fun createSchedule(tripId: String, schedule: TravelSchedule): TravelSchedule
    suspend fun updateSchedule(tripId: String, scheduleId: String, patch: SchedulePatch): TravelSchedule
    suspend fun deleteSchedule(tripId: String, scheduleId: String)
    suspend fun reorderSchedules(tripId: String, scheduleIds: List<String>): List<TravelSchedule>

    suspend fun listExpenses(tripId: String): List<TravelExpense>
    suspend fun createExpense(tripId: String, expense: TravelExpense): TravelExpense
    suspend fun updateExpense(tripId: String, expense: TravelExpense): TravelExpense
    suspend fun deleteExpense(tripId: String, expenseId: String)
    suspend fun getExpenseSettlement(tripId: String): ExpenseSettlementSummary
    suspend fun getSharedFund(tripId: String): SharedFundSnapshot
    suspend fun contributeSharedFund(tripId: String, amount: Long): SharedFundSnapshot

    suspend fun listFavoritePlaceIds(limit: Int = 100, offset: Int = 0): Set<String>
    suspend fun saveFavoritePlace(placeId: String)
    suspend fun deleteFavoritePlace(placeId: String)
}

data class CreateTripCommand(
    val name: String,
    val startDate: String,
    val endDate: String,
    val cities: List<String>,
) {
    init {
        validateTripFields(name, startDate, endDate, cities)
    }
}

data class UpdateTripCommand(
    val name: String,
    val startDate: String,
    val endDate: String,
    val cities: List<String>,
    val version: Int,
) {
    init {
        validateTripFields(name, startDate, endDate, cities)
        require(version >= 0) { "version must not be negative" }
    }
}

data class ParticipantSettings(
    val departurePlaceId: String? = null,
    val returnPlaceId: String? = null,
)

enum class InvitationDecision { DECLINED, CANCELLED }

data class TripMembership(
    val invitationId: String?,
    val trip: TravelTrip,
    val participant: TravelParticipant,
)

data class DateCoordinationSnapshot(
    val tripId: String,
    val startDate: String,
    val endDate: String,
    val tripVersion: Int,
    val canFinalize: Boolean,
    val commonDates: List<String>,
    val participants: List<ParticipantDateAvailability>,
)

data class ParticipantDateAvailability(
    val participant: TravelParticipant,
    val submitted: Boolean,
    val dates: List<String>,
)

data class SchedulePatch(
    val title: String? = null,
    val date: String? = null,
    val time: String? = null,
    val endTime: String? = null,
    val clearEndTime: Boolean = false,
    val memo: String? = null,
    val type: ScheduleType? = null,
    val placeId: String? = null,
    val clearPlaceId: Boolean = false,
    val isVisited: Boolean? = null,
) {
    init {
        require(!(endTime != null && clearEndTime)) {
            "endTime and clearEndTime cannot be supplied together"
        }
        require(!(placeId != null && clearPlaceId)) {
            "placeId and clearPlaceId cannot be supplied together"
        }
    }
}

data class SharedFundSnapshot(
    val tripId: String,
    val contributedAmount: Long,
    val spentAmount: Long,
    val balance: Long,
)

private fun validateTripFields(
    name: String,
    startDate: String,
    endDate: String,
    cities: List<String>,
) {
    require(name.isNotBlank() && name.trim().length <= 100) { "여행 이름은 1~100자여야 합니다." }
    require(cities.isNotEmpty() && cities.size <= 10 && cities.all { it.isNotBlank() && it.length <= 50 }) {
        "도시는 1~10개까지 선택할 수 있습니다."
    }
    val start = startDate.toServerLocalDate()
    val end = endDate.toServerLocalDate()
    require(!end.isBefore(start)) { "여행 종료일은 시작일보다 빠를 수 없습니다." }
    require(ChronoUnit.DAYS.between(start, end) <= 30L) { "여행 기간은 31일까지 설정할 수 있습니다." }
}

private fun String.toServerLocalDate(): LocalDate {
    require(matches(Regex("\\d{4}[.-]\\d{2}[.-]\\d{2}"))) { "여행 날짜 형식이 올바르지 않습니다." }
    return runCatching { LocalDate.parse(replace('.', '-')) }
        .getOrElse { throw IllegalArgumentException("여행 날짜 형식이 올바르지 않습니다.", it) }
}

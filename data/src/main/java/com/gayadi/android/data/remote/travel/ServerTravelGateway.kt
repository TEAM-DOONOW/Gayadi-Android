package com.gayadi.android.data.remote.travel

import com.gayadi.android.data.mapper.TravelRemoteMapper
import com.gayadi.android.data.remote.GayadiHttpClient
import com.gayadi.android.domain.model.ExpensePaymentSource
import com.gayadi.android.domain.model.TravelExpense
import com.gayadi.android.domain.model.TravelInvitation
import com.gayadi.android.domain.model.TravelParticipant
import com.gayadi.android.domain.model.TravelSchedule
import com.gayadi.android.domain.model.TravelTrip
import com.gayadi.android.domain.model.TripStatus
import com.gayadi.android.domain.repository.CreateTripCommand
import com.gayadi.android.domain.repository.DateCoordinationSnapshot
import com.gayadi.android.domain.repository.InvitationDecision
import com.gayadi.android.domain.repository.ParticipantSettings
import com.gayadi.android.domain.repository.SchedulePatch
import com.gayadi.android.domain.repository.SharedFundSnapshot
import com.gayadi.android.domain.repository.TravelGateway
import com.gayadi.android.domain.repository.TripMembership
import com.gayadi.android.domain.repository.UpdateTripCommand
import org.json.JSONArray
import org.json.JSONObject

class ServerTravelGateway(
    private val http: TravelJsonTransport,
) : TravelGateway {
    constructor(client: GayadiHttpClient) : this(GayadiTravelJsonTransport(client))

    override suspend fun listTrips(status: TripStatus?, limit: Int, offset: Int): List<TravelTrip> {
        require(limit in 1..100) { "limit must be between 1 and 100" }
        require(offset >= 0) { "offset must not be negative" }
        val query = buildMap<String, String?> {
            status?.let { put("status", it.name) }
            put("limit", limit.toString())
            put("offset", offset.toString())
        }
        return http.getArray(TRIPS, query).mapObjects(TravelRemoteMapper::trip)
    }

    override suspend fun createTrip(command: CreateTripCommand): TravelTrip =
        TravelRemoteMapper.trip(http.postObject(TRIPS, command.toJson()))

    override suspend fun getTrip(tripId: String): TravelTrip =
        TravelRemoteMapper.trip(http.getObject(tripPath(tripId)))

    override suspend fun updateTrip(tripId: String, command: UpdateTripCommand): TravelTrip =
        TravelRemoteMapper.trip(http.patchObject(tripPath(tripId), command.toJson()))

    override suspend fun deleteTrip(tripId: String) = http.delete(tripPath(tripId))

    override suspend fun updateTripStatus(tripId: String, status: TripStatus): TravelTrip =
        TravelRemoteMapper.trip(
            http.patchObject("${tripPath(tripId)}/status", JSONObject().put("status", status.name)),
        )

    override suspend fun listParticipants(tripId: String): List<TravelParticipant> =
        http.getArray("${tripPath(tripId)}/participants")
            .mapObjects(TravelRemoteMapper::participant)

    override suspend fun addParticipant(
        tripId: String,
        participantUserId: String,
        settings: ParticipantSettings,
    ): TravelParticipant = TravelRemoteMapper.participant(
        http.putObject(
            "${tripPath(tripId)}/participants/${participantUserId.serverId("participantUserId")}",
            settings.toJson(),
        ),
    )

    override suspend fun removeParticipant(tripId: String, participantUserId: String) =
        http.delete(
            "${tripPath(tripId)}/participants/${participantUserId.serverId("participantUserId")}",
        )

    override suspend fun listInvitations(
        tripId: String,
        limit: Int,
        offset: Int,
    ): List<TravelInvitation> {
        require(limit in 1..100) { "limit must be between 1 and 100" }
        require(offset >= 0) { "offset must not be negative" }
        return http.getArray(
            "${tripPath(tripId)}/invitations",
            mapOf("limit" to limit.toString(), "offset" to offset.toString()),
        ).mapObjects(TravelRemoteMapper::invitation)
    }

    override suspend fun createInvitation(
        tripId: String,
        inviteeUserId: String,
        expiresAt: String?,
    ): TravelInvitation {
        val body = JSONObject().put("inviteeUserId", inviteeUserId.serverId("inviteeUserId"))
        expiresAt?.let { body.put("expiresAt", it) }
        return TravelRemoteMapper.invitation(
            http.postObject("${tripPath(tripId)}/invitations", body),
        )
    }

    override suspend fun updateInvitationStatus(
        tripId: String,
        invitationId: String,
        decision: InvitationDecision,
    ): TravelInvitation = TravelRemoteMapper.invitation(
        http.patchObject(
            "${tripPath(tripId)}/invitations/${invitationId.serverId("invitationId")}",
            JSONObject().put("status", decision.name),
        ),
    )

    override suspend fun joinTrip(
        inviteCode: String,
        settings: ParticipantSettings,
    ): TripMembership {
        val body = settings.toJson().put("inviteCode", inviteCode.trim().uppercase())
        return TravelRemoteMapper.membership(http.postObject("/api/v1/trip-memberships", body))
    }

    override suspend fun getDateCoordination(tripId: String): DateCoordinationSnapshot =
        TravelRemoteMapper.dateCoordination(http.getObject(dateCoordinationPath(tripId)))

    override suspend fun submitDateAvailability(
        tripId: String,
        dates: List<String>,
    ): DateCoordinationSnapshot {
        require(dates.isNotEmpty() && dates.size <= 366 && dates.all(String::isNotBlank)) {
            "dates must contain between 1 and 366 non-blank dates"
        }
        return TravelRemoteMapper.dateCoordination(
            http.putObject(
                "${dateCoordinationPath(tripId)}/availability/current",
                JSONObject().put("dates", JSONArray(dates)),
            ),
        )
    }

    override suspend fun finalizeTripDates(
        tripId: String,
        startDate: String,
        endDate: String,
    ): DateCoordinationSnapshot = TravelRemoteMapper.dateCoordination(
        http.putObject(
            "${dateCoordinationPath(tripId)}/finalized-dates",
            JSONObject().put("startDate", startDate).put("endDate", endDate),
        ),
    )

    override suspend fun listSchedules(tripId: String): List<TravelSchedule> =
        http.getArray("${tripPath(tripId)}/schedules")
            .mapObjects(TravelRemoteMapper::schedule)

    override suspend fun createSchedule(
        tripId: String,
        schedule: TravelSchedule,
    ): TravelSchedule = TravelRemoteMapper.schedule(
        http.postObject("${tripPath(tripId)}/schedules", schedule.toCreateJson()),
    )

    override suspend fun updateSchedule(
        tripId: String,
        scheduleId: String,
        patch: SchedulePatch,
    ): TravelSchedule = TravelRemoteMapper.schedule(
        http.patchObject(
            "${tripPath(tripId)}/schedules/${scheduleId.serverId("scheduleId")}",
            patch.toJson(),
        ),
    )

    override suspend fun deleteSchedule(tripId: String, scheduleId: String) =
        http.delete("${tripPath(tripId)}/schedules/${scheduleId.serverId("scheduleId")}")

    override suspend fun reorderSchedules(
        tripId: String,
        scheduleIds: List<String>,
    ): List<TravelSchedule> {
        require(scheduleIds.isNotEmpty()) { "scheduleIds must not be empty" }
        val ids = scheduleIds.map { it.serverId("scheduleId") }
        return http.patchArray(
            "${tripPath(tripId)}/schedule-orders",
            JSONObject().put("scheduleIds", JSONArray(ids)),
        ).mapObjects(TravelRemoteMapper::schedule)
    }

    override suspend fun listExpenses(tripId: String): List<TravelExpense> =
        http.getArray("${tripPath(tripId)}/expenses")
            .mapObjects(TravelRemoteMapper::expense)

    override suspend fun createExpense(tripId: String, expense: TravelExpense): TravelExpense =
        TravelRemoteMapper.expense(
            http.postObject("${tripPath(tripId)}/expenses", expense.toRequestJson()),
        )

    override suspend fun updateExpense(tripId: String, expense: TravelExpense): TravelExpense =
        TravelRemoteMapper.expense(
            http.patchObject(
                "${tripPath(tripId)}/expenses/${expense.id.serverId("expenseId")}",
                expense.toRequestJson(),
            ),
        )

    override suspend fun deleteExpense(tripId: String, expenseId: String) =
        http.delete("${tripPath(tripId)}/expenses/${expenseId.serverId("expenseId")}")

    override suspend fun getExpenseSettlement(tripId: String) =
        TravelRemoteMapper.settlement(http.getObject("${tripPath(tripId)}/expense-settlement"))

    override suspend fun getSharedFund(tripId: String): SharedFundSnapshot =
        TravelRemoteMapper.sharedFund(http.getObject("${tripPath(tripId)}/shared-fund"))

    override suspend fun contributeSharedFund(tripId: String, amount: Long): SharedFundSnapshot {
        require(amount in 1..MAX_EXPENSE_AMOUNT) {
            "amount must be between 1 and $MAX_EXPENSE_AMOUNT"
        }
        return TravelRemoteMapper.sharedFund(
            http.postObject(
                "${tripPath(tripId)}/shared-fund/contributions",
                JSONObject().put("amount", amount),
            ),
        )
    }

    override suspend fun listFavoritePlaceIds(limit: Int, offset: Int): Set<String> {
        require(limit in 1..100) { "limit must be between 1 and 100" }
        require(offset >= 0) { "offset must not be negative" }
        val response = http.getArray(
            FAVORITES,
            mapOf("limit" to limit.toString(), "offset" to offset.toString()),
        )
        return buildSet(response.length()) {
            repeat(response.length()) { index -> add(response.getJSONObject(index).getLong("id").toString()) }
        }
    }

    override suspend fun saveFavoritePlace(placeId: String) {
        http.putObject("$FAVORITES/${placeId.serverId("placeId")}", JSONObject())
    }

    override suspend fun deleteFavoritePlace(placeId: String) =
        http.delete("$FAVORITES/${placeId.serverId("placeId")}")

    private fun tripPath(tripId: String) = "$TRIPS/${tripId.serverId("tripId")}"

    private fun dateCoordinationPath(tripId: String) = "${tripPath(tripId)}/date-coordination"

    private companion object {
        const val TRIPS = "/api/v1/trips"
        const val FAVORITES = "/api/v1/users/current/favorite-places"
        const val MAX_EXPENSE_AMOUNT = 1_000_000_000_000L
    }
}

private fun CreateTripCommand.toJson() = JSONObject()
    .put("name", name)
    .put("startDate", startDate)
    .put("endDate", endDate)
    .put("cities", JSONArray(cities))

private fun UpdateTripCommand.toJson() = JSONObject()
    .put("name", name)
    .put("startDate", startDate)
    .put("endDate", endDate)
    .put("cities", JSONArray(cities))
    .put("version", version)

private fun ParticipantSettings.toJson() = JSONObject()
    .putNullableLong("departurePlaceId", departurePlaceId)
    .putNullableLong("returnPlaceId", returnPlaceId)

private fun TravelSchedule.toCreateJson() = JSONObject()
    .put("title", title)
    .put("date", date)
    .put("time", time)
    .putNullable("endTime", endTime)
    .put("memo", memo)
    .put("type", type.name)
    .putNullableLong("placeId", placeId)

private fun SchedulePatch.toJson() = JSONObject().apply {
    title?.let { put("title", it) }
    date?.let { put("date", it) }
    time?.let { put("time", it) }
    when {
        endTime != null -> put("endTime", endTime)
        clearEndTime -> put("endTime", JSONObject.NULL)
    }
    memo?.let { put("memo", it) }
    type?.let { put("type", it.name) }
    val serverPlaceId = placeId
    when {
        serverPlaceId != null -> put("placeId", serverPlaceId.serverId("placeId"))
        clearPlaceId -> put("placeId", JSONObject.NULL)
    }
    isVisited?.let { put("isVisited", it) }
}

private fun TravelExpense.toRequestJson() = JSONObject()
    .putNullableLong("scheduleId", scheduleId.takeIf(String::isNotBlank))
    .put("title", title)
    .put("memo", memo)
    .put("amount", amount)
    .putNullableLong(
        "payerId",
        payerId.takeIf { paymentSource == ExpensePaymentSource.PERSONAL && it.isNotBlank() },
    )
    .put("participantIds", JSONArray(participantIds.map { it.serverId("participantId") }))
    .put("date", date)
    .put("time", time)
    .put("category", category.name)
    .put("paymentSource", paymentSource.name)
    .putNullable("receiptImageUri", receiptImageUri)

private fun JSONObject.putNullableLong(key: String, value: String?): JSONObject =
    put(key, value?.serverId(key) ?: JSONObject.NULL)

private fun JSONObject.putNullable(key: String, value: String?): JSONObject =
    put(key, value ?: JSONObject.NULL)

private fun String.serverId(field: String): Long =
    toLongOrNull()?.takeIf { it > 0 }
        ?: throw IllegalArgumentException("$field must be a positive server ID")

private fun <T> JSONArray.mapObjects(transform: (JSONObject) -> T): List<T> =
    List(length()) { transform(getJSONObject(it)) }

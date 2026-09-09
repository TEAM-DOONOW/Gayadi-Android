package com.gayadi.android.data.mapper

import com.gayadi.android.domain.repository.DateCoordinationSnapshot
import com.gayadi.android.domain.repository.ParticipantDateAvailability
import com.gayadi.android.domain.repository.SharedFundSnapshot
import com.gayadi.android.domain.repository.TripMembership
import com.gayadi.android.domain.model.ExpenseSettlementSummary
import com.gayadi.android.domain.model.ParticipantExpenseBalance
import com.gayadi.android.domain.model.SettlementTransfer
import com.gayadi.android.domain.model.TravelExpense
import com.gayadi.android.domain.model.TravelInvitation
import com.gayadi.android.domain.model.TravelParticipant
import com.gayadi.android.domain.model.TravelSchedule
import com.gayadi.android.domain.model.TravelTrip
import org.json.JSONArray
import org.json.JSONObject

object TravelRemoteMapper {
    fun trip(json: JSONObject) = TravelTrip(
        id = json.getLong("id").toString(),
        name = json.getString("name"),
        startDate = json.getString("startDate"),
        endDate = json.getString("endDate"),
        cities = json.getJSONArray("cities").strings(),
        status = enumValue(json, "status"),
        participantIds = json.getJSONArray("participantIds").longStrings(),
        inviteCode = json.nullableString("inviteCode").orEmpty(),
        ownerId = json.getLong("ownerId").toString(),
        version = json.getInt("version"),
    )

    fun participant(json: JSONObject) = TravelParticipant(
        id = json.getLong("userId").toString(),
        nickname = json.getString("nickname"),
        characterKey = json.nullableString("characterKey"),
    )

    fun invitation(json: JSONObject) = TravelInvitation(
        id = json.getLong("id").toString(),
        tripId = json.getLong("tripId").toString(),
        code = json.getString("code"),
        inviteeId = json.nullableLong("inviteeId")?.toString().orEmpty(),
        status = enumValue(json, "status"),
    )

    fun membership(json: JSONObject) = TripMembership(
        invitationId = json.nullableLong("invitationId")?.toString(),
        trip = trip(json.getJSONObject("trip")),
        participant = participant(json.getJSONObject("participant")),
    )

    fun dateCoordination(json: JSONObject) = DateCoordinationSnapshot(
        tripId = json.getLong("tripId").toString(),
        startDate = json.getString("startDate"),
        endDate = json.getString("endDate"),
        tripVersion = json.getInt("tripVersion"),
        canFinalize = json.getBoolean("canFinalize"),
        commonDates = json.getJSONArray("commonDates").strings(),
        participants = json.getJSONArray("participants").objects().map { item ->
            ParticipantDateAvailability(
                participant = TravelParticipant(
                    id = item.getLong("userId").toString(),
                    nickname = item.getString("nickname"),
                    characterKey = item.nullableString("characterKey"),
                ),
                submitted = item.getBoolean("submitted"),
                dates = item.getJSONArray("dates").strings(),
            )
        },
    )

    fun schedule(json: JSONObject) = TravelSchedule(
        id = json.getLong("id").toString(),
        tripId = json.getLong("tripId").toString(),
        title = json.getString("title"),
        placeId = json.nullableLong("placeId")?.toString(),
        date = json.getString("date"),
        time = json.getString("time"),
        type = enumValue(json, "type"),
        order = json.getInt("order"),
        isVisited = json.getBoolean("isVisited"),
        endTime = json.nullableString("endTime"),
        memo = json.nullableString("memo").orEmpty(),
    )

    fun expense(json: JSONObject) = TravelExpense(
        id = json.getLong("id").toString(),
        tripId = json.getLong("tripId").toString(),
        scheduleId = json.nullableLong("scheduleId")?.toString().orEmpty(),
        title = json.getString("title"),
        memo = json.nullableString("memo").orEmpty(),
        amount = json.getLong("amount"),
        payerId = json.nullableLong("payerId")?.toString().orEmpty(),
        participantIds = json.getJSONArray("participantIds").longStrings(),
        date = json.getString("date"),
        time = json.getString("time"),
        category = enumValue(json, "category"),
        paymentSource = enumValue(json, "paymentSource"),
        receiptImageUri = json.nullableString("receiptImageUri"),
    )

    fun settlement(json: JSONObject) = ExpenseSettlementSummary(
        totalAmount = json.getLong("totalAmount"),
        balances = json.getJSONArray("balances").objects().map { item ->
            ParticipantExpenseBalance(
                participantId = item.getLong("participantId").toString(),
                paidAmount = item.getLong("paidAmount"),
                owedAmount = item.getLong("owedAmount"),
                netAmount = item.getLong("netAmount"),
            )
        },
        transfers = json.getJSONArray("transfers").objects().map { item ->
            SettlementTransfer(
                fromParticipantId = item.getLong("fromParticipantId").toString(),
                toParticipantId = item.getLong("toParticipantId").toString(),
                amount = item.getLong("amount"),
            )
        },
    )

    fun sharedFund(json: JSONObject) = SharedFundSnapshot(
        tripId = json.getLong("tripId").toString(),
        contributedAmount = json.getLong("contributedAmount"),
        spentAmount = json.getLong("spentAmount"),
        balance = json.getLong("balance"),
    )

    private inline fun <reified T : Enum<T>> enumValue(json: JSONObject, key: String): T =
        enumValueOf(json.getString(key))

    private fun JSONObject.nullableString(key: String): String? =
        if (isNull(key) || !has(key)) null else getString(key)

    private fun JSONObject.nullableLong(key: String): Long? =
        if (isNull(key) || !has(key)) null else getLong(key)

    private fun JSONArray.objects(): List<JSONObject> = List(length(), ::getJSONObject)
    private fun JSONArray.strings(): List<String> = List(length(), ::getString)
    private fun JSONArray.longStrings(): List<String> = List(length()) { getLong(it).toString() }
}

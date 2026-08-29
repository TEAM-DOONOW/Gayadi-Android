package com.gayadi.android.data

import com.gayadi.android.data.mapper.TravelRemoteMapper
import com.gayadi.android.domain.model.ExpenseCategory
import com.gayadi.android.domain.model.ExpensePaymentSource
import com.gayadi.android.domain.model.InvitationStatus
import com.gayadi.android.domain.model.ScheduleType
import com.gayadi.android.domain.model.TripStatus
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TravelRemoteMapperTest {
    @Test
    fun `trip mapper converts server ids and keeps optimistic lock version`() {
        val trip = TravelRemoteMapper.trip(tripJson())

        assertEquals("31", trip.id)
        assertEquals("12", trip.ownerId)
        assertEquals(listOf("12", "18"), trip.participantIds)
        assertEquals(listOf("제주", "서귀포"), trip.cities)
        assertEquals(TripStatus.PLANNING, trip.status)
        assertEquals(4, trip.version)
    }

    @Test
    fun `nullable schedule and expense ids map to existing empty string convention`() {
        val schedule = TravelRemoteMapper.schedule(scheduleJson(placeId = null, endTime = null))
        val expense = TravelRemoteMapper.expense(expenseJson(scheduleId = null, payerId = null))

        assertNull(schedule.placeId)
        assertNull(schedule.endTime)
        assertEquals(ScheduleType.MAIN, schedule.type)
        assertEquals("", expense.scheduleId)
        assertEquals("", expense.payerId)
        assertEquals(ExpenseCategory.FOOD, expense.category)
        assertEquals(ExpensePaymentSource.SHARED_FUND, expense.paymentSource)
    }

    @Test
    fun `invitation membership and coordination preserve server state`() {
        val invitation = TravelRemoteMapper.invitation(invitationJson())
        val membership = TravelRemoteMapper.membership(
            JSONObject()
                .put("invitationId", 77)
                .put("trip", tripJson())
                .put("participant", participantJson()),
        )
        val coordination = TravelRemoteMapper.dateCoordination(coordinationJson())

        assertEquals(InvitationStatus.PENDING, invitation.status)
        assertEquals("18", invitation.inviteeId)
        assertEquals("77", membership.invitationId)
        assertEquals("18", membership.participant.id)
        assertTrue(coordination.canFinalize)
        assertEquals(5, coordination.tripVersion)
        assertEquals(listOf("2026.09.10", "2026.09.11"), coordination.commonDates)
        assertTrue(coordination.participants.first().submitted)
        assertFalse(coordination.participants.last().submitted)
    }

    @Test
    fun `settlement and shared fund convert participant and trip ids`() {
        val settlement = TravelRemoteMapper.settlement(
            JSONObject()
                .put("totalAmount", 30_000)
                .put(
                    "balances",
                    JSONArray().put(
                        JSONObject()
                            .put("participantId", 12)
                            .put("paidAmount", 30_000)
                            .put("owedAmount", 15_000)
                            .put("netAmount", 15_000),
                    ),
                )
                .put(
                    "transfers",
                    JSONArray().put(
                        JSONObject()
                            .put("fromParticipantId", 18)
                            .put("toParticipantId", 12)
                            .put("amount", 15_000),
                    ),
                ),
        )
        val sharedFund = TravelRemoteMapper.sharedFund(
            JSONObject()
                .put("tripId", 31)
                .put("contributedAmount", 100_000)
                .put("spentAmount", 25_000)
                .put("balance", 75_000),
        )

        assertEquals("12", settlement.balances.single().participantId)
        assertEquals("18", settlement.transfers.single().fromParticipantId)
        assertEquals("31", sharedFund.tripId)
        assertEquals(75_000, sharedFund.balance)
    }
}

internal fun tripJson() = JSONObject()
    .put("id", 31)
    .put("name", "제주 우정 여행")
    .put("startDate", "2026.09.10")
    .put("endDate", "2026.09.12")
    .put("cities", JSONArray(listOf("제주", "서귀포")))
    .put("status", "PLANNING")
    .put("ownerId", 12)
    .put("participantIds", JSONArray(listOf(12L, 18L)))
    .put("inviteCode", "A1B2C3")
    .put("version", 4)

internal fun participantJson() = JSONObject()
    .put("id", 18)
    .put("userId", 18)
    .put("participantId", 45)
    .put("nickname", "여행자")
    .put("characterKey", "character_pnr")
    .put("role", "MEMBER")
    .put("status", "JOINED")

internal fun invitationJson() = JSONObject()
    .put("id", 77)
    .put("tripId", 31)
    .put("inviterId", 12)
    .put("inviteeId", 18)
    .put("code", "A1B2C3D4")
    .put("status", "PENDING")

internal fun coordinationJson() = JSONObject()
    .put("tripId", 31)
    .put("startDate", "2026.09.10")
    .put("endDate", "2026.09.12")
    .put("tripVersion", 5)
    .put("canFinalize", true)
    .put("commonDates", JSONArray(listOf("2026.09.10", "2026.09.11")))
    .put(
        "participants",
        JSONArray()
            .put(
                JSONObject()
                    .put("userId", 12)
                    .put("nickname", "소유자")
                    .put("characterKey", JSONObject.NULL)
                    .put("submitted", true)
                    .put("dates", JSONArray(listOf("2026.09.10", "2026.09.11"))),
            )
            .put(
                JSONObject()
                    .put("userId", 18)
                    .put("nickname", "여행자")
                    .put("characterKey", "character_pnr")
                    .put("submitted", false)
                    .put("dates", JSONArray()),
            ),
    )

internal fun scheduleJson(placeId: Long? = 101, endTime: String? = "11:00") = JSONObject()
    .put("id", 81)
    .put("tripId", 31)
    .put("title", "성산일출봉")
    .put("placeId", placeId ?: JSONObject.NULL)
    .put("placeName", "성산일출봉")
    .put("date", "2026.09.10")
    .put("time", "09:30")
    .put("endTime", endTime ?: JSONObject.NULL)
    .put("memo", "일찍 출발")
    .put("type", "MAIN")
    .put("order", 0)
    .put("isVisited", false)

internal fun expenseJson(scheduleId: Long? = 81, payerId: Long? = 12) = JSONObject()
    .put("id", 91)
    .put("tripId", 31)
    .put("scheduleId", scheduleId ?: JSONObject.NULL)
    .put("title", "점심")
    .put("memo", "")
    .put("amount", 30_000)
    .put("payerId", payerId ?: JSONObject.NULL)
    .put("participantIds", JSONArray(listOf(12L, 18L)))
    .put("date", "2026.09.10")
    .put("time", "12:30")
    .put("category", "FOOD")
    .put("paymentSource", if (payerId == null) "SHARED_FUND" else "PERSONAL")
    .put("receiptImageUri", JSONObject.NULL)

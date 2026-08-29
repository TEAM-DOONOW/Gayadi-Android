package com.gayadi.android.data

import com.gayadi.android.data.remote.travel.ServerTravelGateway
import com.gayadi.android.data.remote.travel.TravelJsonTransport
import com.gayadi.android.domain.repository.CreateTripCommand
import com.gayadi.android.domain.repository.ParticipantSettings
import com.gayadi.android.domain.repository.SchedulePatch
import com.gayadi.android.domain.repository.UpdateTripCommand
import com.gayadi.android.domain.model.ExpenseCategory
import com.gayadi.android.domain.model.ExpensePaymentSource
import com.gayadi.android.domain.model.ScheduleType
import com.gayadi.android.domain.model.TravelExpense
import kotlinx.coroutines.test.runTest
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class ServerTravelGatewayTest {
    private val http = RecordingTravelJsonTransport()
    private val gateway = ServerTravelGateway(http)

    @Test
    fun `create and update trip send server required dates and version`() = runTest {
        http.objectResponse = tripJson()

        gateway.createTrip(
            CreateTripCommand(
                name = "제주 우정 여행",
                startDate = "2026.09.10",
                endDate = "2026.09.12",
                cities = listOf("제주", "서귀포"),
            ),
        )

        assertEquals("POST", http.lastMethod)
        assertEquals("/api/v1/trips", http.lastPath)
        assertEquals("2026.09.10", http.lastBody.getString("startDate"))
        assertEquals(2, http.lastBody.getJSONArray("cities").length())

        gateway.updateTrip(
            tripId = "31",
            command = UpdateTripCommand(
                name = "수정한 여행",
                startDate = "2026.09.11",
                endDate = "2026.09.13",
                cities = listOf("제주"),
                version = 4,
            ),
        )

        assertEquals("PATCH_OBJECT", http.lastMethod)
        assertEquals("/api/v1/trips/31", http.lastPath)
        assertEquals(4, http.lastBody.getInt("version"))
    }

    @Test
    fun `participant invitation and membership use numeric ids and normalized code`() = runTest {
        http.objectResponse = participantJson()
        gateway.addParticipant(
            tripId = "31",
            participantUserId = "18",
            settings = ParticipantSettings(departurePlaceId = "101"),
        )

        assertEquals("PUT", http.lastMethod)
        assertEquals("/api/v1/trips/31/participants/18", http.lastPath)
        assertEquals(101, http.lastBody.getLong("departurePlaceId"))
        assertTrue(http.lastBody.isNull("returnPlaceId"))

        http.objectResponse = JSONObject()
            .put("invitationId", 77)
            .put("trip", tripJson())
            .put("participant", participantJson())
        gateway.joinTrip(" a1b2c3 ")

        assertEquals("POST", http.lastMethod)
        assertEquals("/api/v1/trip-memberships", http.lastPath)
        assertEquals("A1B2C3", http.lastBody.getString("inviteCode"))
    }

    @Test
    fun `date availability and finalization call REST coordination endpoints`() = runTest {
        http.objectResponse = coordinationJson()

        gateway.submitDateAvailability("31", listOf("2026.09.10", "2026.09.11"))

        assertEquals("PUT", http.lastMethod)
        assertEquals(
            "/api/v1/trips/31/date-coordination/availability/current",
            http.lastPath,
        )
        assertEquals(2, http.lastBody.getJSONArray("dates").length())

        gateway.finalizeTripDates("31", "2026.09.10", "2026.09.12")

        assertEquals("/api/v1/trips/31/date-coordination/finalized-dates", http.lastPath)
        assertEquals("2026.09.12", http.lastBody.getString("endDate"))
    }

    @Test
    fun `schedule patch can explicitly clear nullable values and reorder uses array response`() = runTest {
        http.objectResponse = scheduleJson(placeId = null, endTime = null)

        gateway.updateSchedule(
            tripId = "31",
            scheduleId = "81",
            patch = SchedulePatch(
                clearPlaceId = true,
                clearEndTime = true,
                isVisited = true,
            ),
        )

        assertEquals("PATCH_OBJECT", http.lastMethod)
        assertTrue(http.lastBody.isNull("placeId"))
        assertTrue(http.lastBody.isNull("endTime"))
        assertTrue(http.lastBody.getBoolean("isVisited"))

        http.arrayResponse = JSONArray().put(scheduleJson())
        val reordered = gateway.reorderSchedules("31", listOf("81"))

        assertEquals("PATCH_ARRAY", http.lastMethod)
        assertEquals("/api/v1/trips/31/schedule-orders", http.lastPath)
        assertEquals(81, http.lastBody.getJSONArray("scheduleIds").getLong(0))
        assertEquals("81", reordered.single().id)
    }

    @Test
    fun `shared fund expense sends null payer and nullable schedule id`() = runTest {
        http.objectResponse = expenseJson(scheduleId = null, payerId = null)
        val expense = TravelExpense(
            id = "local-draft",
            tripId = "31",
            scheduleId = "",
            title = "공동 점심",
            amount = 30_000,
            payerId = "12",
            participantIds = listOf("12", "18"),
            date = "2026.09.10",
            time = "12:30",
            category = ExpenseCategory.FOOD,
            paymentSource = ExpensePaymentSource.SHARED_FUND,
        )

        val created = gateway.createExpense("31", expense)

        assertEquals("POST", http.lastMethod)
        assertTrue(http.lastBody.isNull("scheduleId"))
        assertTrue(http.lastBody.isNull("payerId"))
        assertEquals(12, http.lastBody.getJSONArray("participantIds").getLong(0))
        assertEquals("", created.payerId)
    }

    @Test
    fun `local ids fail before a server request is made`() {
        assertThrows(IllegalArgumentException::class.java) {
            runTest { gateway.getTrip("trip-local") }
        }
        assertFalse(http.wasCalled)
    }

    @Test
    fun `schedule patch rejects contradictory clear commands`() {
        assertThrows(IllegalArgumentException::class.java) {
            SchedulePatch(placeId = "101", clearPlaceId = true, type = ScheduleType.MAIN)
        }
    }
}

private class RecordingTravelJsonTransport : TravelJsonTransport {
    var objectResponse: JSONObject = JSONObject()
    var arrayResponse: JSONArray = JSONArray()
    var lastMethod: String = ""
    var lastPath: String = ""
    var lastQuery: Map<String, String?> = emptyMap()
    var lastBody: JSONObject = JSONObject()
    var wasCalled: Boolean = false

    override suspend fun getObject(path: String, query: Map<String, String?>): JSONObject {
        record("GET_OBJECT", path, query)
        return objectResponse
    }

    override suspend fun getArray(path: String, query: Map<String, String?>): JSONArray {
        record("GET_ARRAY", path, query)
        return arrayResponse
    }

    override suspend fun postObject(path: String, body: JSONObject): JSONObject {
        record("POST", path, body = body)
        return objectResponse
    }

    override suspend fun putObject(path: String, body: JSONObject): JSONObject {
        record("PUT", path, body = body)
        return objectResponse
    }

    override suspend fun patchObject(path: String, body: JSONObject): JSONObject {
        record("PATCH_OBJECT", path, body = body)
        return objectResponse
    }

    override suspend fun patchArray(path: String, body: JSONObject): JSONArray {
        record("PATCH_ARRAY", path, body = body)
        return arrayResponse
    }

    override suspend fun delete(path: String) {
        record("DELETE", path)
    }

    private fun record(
        method: String,
        path: String,
        query: Map<String, String?> = emptyMap(),
        body: JSONObject = JSONObject(),
    ) {
        wasCalled = true
        lastMethod = method
        lastPath = path
        lastQuery = query
        lastBody = JSONObject(body.toString())
    }
}

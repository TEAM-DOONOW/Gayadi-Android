package com.gayadi.android.data

import com.gayadi.android.data.remote.travel.ServerTripSupportGateway
import com.gayadi.android.data.remote.travel.TravelJsonTransport
import com.gayadi.android.domain.model.RouteTransportMode
import kotlinx.coroutines.test.runTest
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test

class ItineraryRecommendationRequestTest {
    @Test
    fun `recommendation and apply send identical strict route inputs`() = runTest {
        val http = ItineraryRequestTransport()
        val gateway = ServerTripSupportGateway(http)

        gateway.recommendItinerary("42", "2026.10.03", "10:00", "18:00", RouteTransportMode.BICYCLE, 2)
        assertEquals("POST", http.method)
        assertEquals("/api/v1/trips/42/itinerary-recommendations", http.path)
        assertEquals("BICYCLE", http.body.getString("transportMode"))
        assertEquals(2, http.body.getInt("variation"))

        gateway.applyItinerary(
            "42", "2026.10.03", "10:00", "18:00", RouteTransportMode.BICYCLE, 2,
            expectedPlaceIds = listOf("11", "12"),
        )
        assertEquals("PUT", http.method)
        assertEquals("/api/v1/trips/42/itinerary-selections/2026-10-03", http.path)
        assertEquals(false, http.body.has("date"))
        assertEquals("10:00", http.body.getString("startTime"))
        assertEquals("18:00", http.body.getString("endTime"))
        assertEquals(listOf(11L, 12L), List(2) { http.body.getJSONArray("expectedPlaceIds").getLong(it) })
    }
}

private class ItineraryRequestTransport : TravelJsonTransport {
    var method = ""
    var path = ""
    var body = JSONObject()

    override suspend fun postObject(path: String, body: JSONObject): JSONObject {
        capture("POST", path, body)
        return response(body)
    }

    override suspend fun putObject(path: String, body: JSONObject): JSONObject {
        capture("PUT", path, body)
        return response(body)
    }

    private fun capture(method: String, path: String, body: JSONObject) {
        this.method = method
        this.path = path
        this.body = body
    }

    private fun response(body: JSONObject) = JSONObject()
        .put("date", body.optString("date", "2026.10.03"))
        .put("startTime", body.getString("startTime"))
        .put("endTime", body.getString("endTime"))
        .put("transportMode", body.getString("transportMode"))
        .put("variation", body.getInt("variation"))
        .put("estimated", true)
        .put("totalTravelMinutes", 20)
        .put("totalStayMinutes", 160)
        .put("summary", "demo")
        .put("stops", JSONArray())

    override suspend fun getObject(path: String, query: Map<String, String?>): JSONObject = error("Unexpected GET")
    override suspend fun getArray(path: String, query: Map<String, String?>): JSONArray = error("Unexpected GET")
    override suspend fun patchObject(path: String, body: JSONObject): JSONObject = error("Unexpected PATCH")
    override suspend fun patchArray(path: String, body: JSONObject): JSONArray = error("Unexpected PATCH")
    override suspend fun delete(path: String): Unit = error("Unexpected DELETE")
}

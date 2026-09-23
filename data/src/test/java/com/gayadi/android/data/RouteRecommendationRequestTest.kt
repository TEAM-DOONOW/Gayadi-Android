package com.gayadi.android.data

import com.gayadi.android.data.remote.travel.ServerTripSupportGateway
import com.gayadi.android.data.remote.travel.TravelJsonTransport
import com.gayadi.android.domain.model.RouteTransportMode
import kotlinx.coroutines.test.runTest
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class RouteRecommendationRequestTest {
    private val http = RouteRequestTransport()
    private val gateway = ServerTripSupportGateway(http)

    @Test fun `selected transport is sent explicitly for itinerary without user id`() = runTest {
        RouteTransportMode.entries.forEach { mode ->
            gateway.recommendRoutes("42", "ITINERARY", transportMode = mode)
            assertEquals("/api/v1/trips/42/route-recommendations", http.path)
            assertEquals("ITINERARY", http.body.getString("type"))
            assertEquals(mode.name, http.body.getString("transportMode"))
            assertFalse(http.body.has("userId"))
        }
    }

    @Test fun `existing callers default to public transit and preserve user id`() = runTest {
        gateway.recommendRoutes("42", "DEPARTURE", userId = "7")
        assertEquals("PUBLIC_TRANSIT", http.body.getString("transportMode"))
        assertEquals(7L, http.body.getLong("userId"))
    }
}

private class RouteRequestTransport : TravelJsonTransport {
    var path = ""
    var body = JSONObject()
    override suspend fun postObject(path: String, body: JSONObject): JSONObject {
        this.path = path
        this.body = body
        return JSONObject().put("options", JSONArray())
    }
    override suspend fun getObject(path: String, query: Map<String, String?>): JSONObject = error("Unexpected GET")
    override suspend fun getArray(path: String, query: Map<String, String?>): JSONArray = error("Unexpected GET")
    override suspend fun putObject(path: String, body: JSONObject): JSONObject = error("Unexpected PUT")
    override suspend fun patchObject(path: String, body: JSONObject): JSONObject = error("Unexpected PATCH")
    override suspend fun patchArray(path: String, body: JSONObject): JSONArray = error("Unexpected PATCH")
    override suspend fun delete(path: String): Unit = error("Unexpected DELETE")
}

package com.gayadi.android.data

import com.gayadi.android.data.remote.travel.ServerTripSupportGateway
import com.gayadi.android.data.remote.travel.TravelJsonTransport
import kotlinx.coroutines.test.runTest
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test

class TourSupportGatewayTest {
    @Test
    fun `tour searches send the arrange code for that search`() = runTest {
        val http = RecordingGetTransport()
        val gateway = ServerTripSupportGateway(http)

        gateway.getNearbyTourPlaces(126.97, 37.56, 1000, 10)
        assertEquals("/api/v1/tour/locations", http.path)
        assertEquals("E", http.query["arrange"])

        gateway.searchTourPlaces("시장", 10)
        assertEquals("/api/v1/tour/keywords", http.path)
        assertEquals("C", http.query["arrange"])
        assertEquals("시장", http.query["keyword"])

        gateway.getTourFestivals("20260901", "20261231", 10)
        assertEquals("/api/v1/tour/festivals", http.path)
        assertEquals("C", http.query["arrange"])
        assertEquals("20260901", http.query["eventStartDate"])

        gateway.getTourStays(10)
        assertEquals("/api/v1/tour/stays", http.path)
        assertEquals("C", http.query["arrange"])
    }
}

private class RecordingGetTransport : TravelJsonTransport {
    var path = ""
    var query: Map<String, String?> = emptyMap()

    override suspend fun getObject(path: String, query: Map<String, String?>): JSONObject {
        this.path = path
        this.query = query
        return JSONObject().put("items", JSONArray().put(JSONObject()
            .put("contentId", "1")
            .put("title", "장소")))
    }

    override suspend fun getArray(path: String, query: Map<String, String?>): JSONArray = error("Unexpected GET")
    override suspend fun postObject(path: String, body: JSONObject): JSONObject = error("Unexpected POST")
    override suspend fun putObject(path: String, body: JSONObject): JSONObject = error("Unexpected PUT")
    override suspend fun patchObject(path: String, body: JSONObject): JSONObject = error("Unexpected PATCH")
    override suspend fun patchArray(path: String, body: JSONObject): JSONArray = error("Unexpected PATCH")
    override suspend fun delete(path: String): Unit = error("Unexpected DELETE")
}

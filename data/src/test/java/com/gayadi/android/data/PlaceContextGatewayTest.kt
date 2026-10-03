package com.gayadi.android.data

import com.gayadi.android.data.remote.travel.ServerTripSupportGateway
import com.gayadi.android.data.remote.travel.TravelJsonTransport
import kotlinx.coroutines.test.runTest
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaceContextGatewayTest {
    @Test
    fun `saved place context maps daily and hourly congestion with weather from one call`() = runTest {
        val http = PlaceContextTransport(JSONObject("""
            {"place":{"id":3,"name":"광장시장"},
             "weather":{"available":true,"condition":"흐림","temperatureCelsius":22.6,
                        "precipitationProbability":30,"observedAt":"2026-09-30T20:00:00+09:00"},
             "congestion":{"available":true,"currentLevel":"NORMAL","currentScore":45,
                        "dataType":"FORECAST","source":"CALENDAR_HEURISTIC","targetDate":"2026-09-30",
                        "hourlySource":"CALENDAR_HEURISTIC","message":"추정치",
                        "hourly":[{"hour":9,"score":39,"level":"RELAXED"},{"hour":10,"score":null,"level":null},
                                  {"hour":13,"score":56,"level":"NORMAL"}]}}
        """.trimIndent()))

        val context = ServerTripSupportGateway(http).getPlaceContext("3")

        assertEquals("/api/v1/congestion/places/3", http.path)
        assertEquals(listOf(9, 13), context.hourly!!.points.map { it.hour })
        assertEquals("광장시장", context.hourly!!.placeName)
        assertEquals("NORMAL", context.daily!!.level)
        assertEquals(45, context.daily!!.score)
        assertTrue(context.daily!!.estimated)
        assertEquals("흐림", context.weather!!.condition)
        assertEquals("20260930", context.weather!!.baseDate)
        assertEquals("2000", context.weather!!.baseTime)
    }

    @Test
    fun `missing congestion yields no hourly or daily values`() = runTest {
        val context = ServerTripSupportGateway(PlaceContextTransport(JSONObject("""{"place":{"id":3}}""")))
            .getPlaceContext("3")
        assertNull(context.hourly)
        assertNull(context.daily)
        assertNull(context.weather)
    }
}

private class PlaceContextTransport(private val response: JSONObject) : TravelJsonTransport {
    var path = ""

    override suspend fun getObject(path: String, query: Map<String, String?>): JSONObject {
        this.path = path
        return response
    }

    override suspend fun getArray(path: String, query: Map<String, String?>): JSONArray = error("Unexpected GET")
    override suspend fun postObject(path: String, body: JSONObject): JSONObject = error("Unexpected POST")
    override suspend fun putObject(path: String, body: JSONObject): JSONObject = error("Unexpected PUT")
    override suspend fun patchObject(path: String, body: JSONObject): JSONObject = error("Unexpected PATCH")
    override suspend fun patchArray(path: String, body: JSONObject): JSONArray = error("Unexpected PATCH")
    override suspend fun delete(path: String): Unit = error("Unexpected DELETE")
}

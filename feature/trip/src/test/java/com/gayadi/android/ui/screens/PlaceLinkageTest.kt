package com.gayadi.android.ui.screens

import com.gayadi.android.domain.model.ScheduleType
import com.gayadi.android.domain.model.TravelSchedule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaceLinkageTest {
    // 경복궁 → 광화문 → 덕수궁 (서울 도심, 서로 1~2km)
    private val first = visit("1", 0, 37.5796, 126.9770)
    private val second = visit("2", 1, 37.5759, 126.9768)
    private val third = visit("3", 2, 37.5658, 126.9751)

    @Test fun `visits are ordered same day main place schedules only`() {
        val visits = sameDayPlaceVisits(
            listOf(
                third, first, second,
                visit("other-day", 3, 37.0, 127.0).copy(date = "2026.09.23"),
                visit("alt", 4, 37.0, 127.0).copy(type = ScheduleType.ALTERNATIVE),
                visit("memo", 5, 37.0, 127.0).copy(placeId = null),
            ),
            tripId = "trip", date = "2026.09.22",
        )
        assertEquals(listOf("1", "2", "3"), visits.map { it.id })
    }

    @Test fun `replacing the first visit links to the second visit`() {
        val neighbors = replacementNeighbors(listOf(first, second, third), "1")!!
        assertNull(neighbors.previous)
        assertEquals("2", neighbors.next?.id)
        assertEquals(GeoPoint(second.latitude!!, second.longitude!!), neighbors.linkedOrigin())
    }

    @Test fun `replacing a middle visit uses the midpoint of its neighbors`() {
        val origin = replacementNeighbors(listOf(first, second, third), "2")!!.linkedOrigin()!!
        assertEquals((first.latitude!! + third.latitude!!) / 2, origin.latitude, 1e-9)
        assertEquals((first.longitude!! + third.longitude!!) / 2, origin.longitude, 1e-9)
    }

    @Test fun `nearby replacement is linked and a distant one is not`() {
        val neighbors = replacementNeighbors(listOf(first, second, third), "1")!!
        assertTrue(neighbors.isLinked(GeoPoint(37.5826, 126.9831))) // 북촌, 약 1km
        assertFalse(neighbors.isLinked(GeoPoint(37.5112, 127.0981))) // 잠실, 약 15km
    }

    @Test fun `allowed distance scales with the original gap between visits`() {
        // 원래 이웃과 20km 떨어져 있던 장소라면 25km 떨어진 대안도 연계로 본다.
        val farFirst = visit("far", 0, 37.4000, 126.9768)
        val neighbors = replacementNeighbors(listOf(farFirst, second), "far")!!
        assertTrue(neighbors.isLinked(GeoPoint(37.3500, 126.9768)))
        assertFalse(neighbors.isLinked(GeoPoint(37.2000, 126.9768)))
    }

    @Test fun `unknown coordinates never trigger a warning`() {
        val neighbors = replacementNeighbors(listOf(first, second), "1")!!
        assertTrue(neighbors.isLinked(null))
        assertNull(replacementNeighbors(listOf(first, second), "missing"))
    }

    private fun visit(id: String, order: Int, latitude: Double, longitude: Double) = TravelSchedule(
        id = id, tripId = "trip", title = "장소$id", placeId = "place-$id", date = "2026.09.22",
        time = "10:00", order = order, latitude = latitude, longitude = longitude,
    )
}

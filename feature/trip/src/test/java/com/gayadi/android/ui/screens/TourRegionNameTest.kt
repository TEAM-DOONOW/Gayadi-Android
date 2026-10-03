package com.gayadi.android.ui.screens

import com.gayadi.android.domain.model.RouteTransportMode
import org.junit.Assert.assertEquals
import org.junit.Test

class TourRegionNameTest {
    @Test
    fun keepsTheFullCityNameTheServerUnderstands() {
        assertEquals("수원·용인", tourRegionName("수원·용인"))
        assertEquals("강릉·속초", tourRegionName(" 강릉·속초 "))
        assertEquals("제주 성산", tourRegionName("제주 성산"))
        assertEquals("서울", tourRegionName(" "))
    }

    @Test
    fun travelLabelShowsStraightLineDistanceWithoutChangingTheTimePhrase() {
        assertEquals(
            "대중교통 1.8km · 약 18분",
            itineraryTravelLabel(RouteTransportMode.PUBLIC_TRANSIT, 18, 1_800),
        )
        assertEquals(
            "도보 800m · 약 12분",
            itineraryTravelLabel(RouteTransportMode.WALK, 12, 800),
        )
        assertEquals(
            "자동차 약 10분",
            itineraryTravelLabel(RouteTransportMode.CAR, 10, 0),
        )
    }
}

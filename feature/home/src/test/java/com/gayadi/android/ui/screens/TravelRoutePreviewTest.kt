package com.gayadi.android.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Test

class TravelRoutePreviewTest {
    @Test
    fun keepsSelectedCoordinatesAndOrderForPlacesWithIdenticalNames() {
        val plans = listOf(
            HomeTravelPlan("1", "같은 이름", "2026.09.22", "10:00", "", false, 37.0, 127.0),
            HomeTravelPlan("2", "좌표 없음", "2026.09.22", "11:00", "", false),
            HomeTravelPlan("3", "같은 이름", "2026.09.22", "12:00", "", false, 35.0, 129.0),
        )
        val points = routeMapPoints(plans)
        assertEquals(listOf(1, 3), points.map { it.order })
        assertEquals(listOf(37.0, 35.0), points.map { it.latitude })
        assertEquals(listOf(127.0, 129.0), points.map { it.longitude })
    }

    @Test
    fun rejectsInvalidCoordinatesInsteadOfPlacingAnUnrelatedMarker() {
        val plan = HomeTravelPlan("1", "장소", "", "", "", false, Double.NaN, 127.0)
        assertEquals(emptyList<RouteMapPoint>(), routeMapPoints(listOf(plan, plan.copy(latitude = 91.0))))
    }
    @Test
    fun usesConfiguredHttpsDomainAsKakaoOrigin() {
        assertEquals(
            "https://doonow-dev.gayadi.site",
            kakaoWebViewOrigin("https://doonow-dev.gayadi.site"),
        )
    }

    @Test
    fun stripsPathQueryAndFragmentFromKakaoOrigin() {
        assertEquals(
            "https://doonow-dev.gayadi.site:8443",
            kakaoWebViewOrigin("https://doonow-dev.gayadi.site:8443/api?source=app#map"),
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsBaseUrlWithoutHttpScheme() {
        kakaoWebViewOrigin("doonow-dev.gayadi.site")
    }
}

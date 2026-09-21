package com.gayadi.android.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Test

class TravelRoutePreviewTest {
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

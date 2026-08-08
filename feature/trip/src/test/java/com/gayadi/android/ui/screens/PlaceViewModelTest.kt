package com.gayadi.android.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaceViewModelTest {
    @Test
    fun queryAndCategoryFilterPlaces() {
        val viewModel = PlaceViewModel()
        viewModel.selectCategory("카페")
        viewModel.updateQuery("글렌코")
        assertEquals(listOf("place-2"), viewModel.uiState.value.filteredPlaces.map(PlaceItem::id))
    }

    @Test
    fun detailNearbyWeatherAndCrowdComeFromRepository() {
        val viewModel = PlaceViewModel()
        val place = viewModel.findPlace("place-3")!!
        assertEquals("섭지코지", place.name)
        assertEquals("바람", place.weather)
        assertEquals(CrowdLevel.CROWDED, place.crowdLevel)
        val nearby = viewModel.nearbyPlaces("place-3")
        assertTrue(nearby.none { it.id == "place-3" })
        assertEquals(nearby.sortedBy(PlaceItem::distanceMeters), nearby)
    }
}

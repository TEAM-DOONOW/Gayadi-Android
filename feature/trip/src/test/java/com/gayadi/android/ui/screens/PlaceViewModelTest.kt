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

    @Test
    fun changingRegionLoadsTripSpecificPlacesAndResetsFilters() {
        val viewModel = PlaceViewModel()
        viewModel.selectCategory("카페")
        viewModel.updateQuery("글렌코")

        viewModel.setRegion("서울")

        assertEquals("서울", viewModel.uiState.value.regionName)
        assertEquals("", viewModel.uiState.value.query)
        assertEquals("전체", viewModel.uiState.value.selectedCategory)
        assertEquals(
            listOf("광장시장", "서울숲 카페거리", "경복궁", "서울역 스테이"),
            viewModel.uiState.value.places.map(PlaceItem::name),
        )
        assertTrue(viewModel.uiState.value.places.none { it.description.contains("제주") })
    }

    @Test
    fun changingRegionKeepsPreviouslyLoadedFavoriteDetailsAddressable() {
        val viewModel = PlaceViewModel()
        viewModel.setRegion("서울")
        viewModel.setRegion("부산")

        assertEquals("광장시장", viewModel.findPlace("seoul-place-1")?.name)
        assertTrue(viewModel.uiState.value.places.all { it.description.contains("부산") })
    }
}

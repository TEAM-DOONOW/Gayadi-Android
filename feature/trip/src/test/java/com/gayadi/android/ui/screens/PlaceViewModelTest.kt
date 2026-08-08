package com.gayadi.android.ui.screens

import androidx.lifecycle.SavedStateHandle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaceViewModelTest {
    @Test
    fun queryAndCategoryFilterPlaces() {
        val viewModel = PlaceViewModel(SavedStateHandle())

        viewModel.selectCategory("카페")
        viewModel.updateQuery("글렌코")

        assertEquals(listOf("place-2"), viewModel.uiState.value.filteredPlaces.map(PlaceItem::id))
    }

    @Test
    fun detailLookupAndScheduleStateUsePlaceId() {
        val state = SavedStateHandle()
        val viewModel = PlaceViewModel(state)

        assertEquals("섭지코지", viewModel.findPlace("place-3")?.name)
        viewModel.addPlaceToSchedule("trip-a", "place-3")

        assertTrue("place-3" in viewModel.scheduledPlaceIds("trip-a"))
        assertTrue("place-3" in PlaceViewModel(state).scheduledPlaceIds("trip-a"))
        assertTrue(PlaceViewModel(state).scheduledPlaceIds("trip-b").isEmpty())
    }
}

package com.gayadi.android.ui.screens

import androidx.lifecycle.SavedStateHandle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TripViewModelTest {
    @Test
    fun restoresLegacyJsonTrips() {
        val legacyJson = """[{"id":"legacy-1","name":"제주 여행","startDate":"2026.08.08","endDate":"2026.08.10","cities":["제주"],"coverImageResList":[1,2]}]"""
        val viewModel = TripViewModel(SavedStateHandle(mapOf("saved_trips" to legacyJson)))

        assertEquals("legacy-1", viewModel.trips.value.single().id)
        assertEquals(listOf("제주"), viewModel.trips.value.single().cities)
    }

    @Test
    fun selectingTripPersistsActualIdAndDeletionClearsIt() {
        val state = SavedStateHandle()
        val viewModel = TripViewModel(state)
        val trip = TripSummary(
            id = "trip-28",
            name = "제주 여행",
            startDate = "2026.08.08",
            endDate = "2026.08.10",
            cities = listOf("제주"),
            coverImageResList = emptyList(),
        )
        viewModel.addTrip(trip)

        viewModel.selectTrip("trip-28")

        assertEquals("trip-28", viewModel.selectedTripId.value)
        assertEquals("제주 여행", viewModel.tripById("trip-28")?.name)
        assertEquals("trip-28", TripViewModel(state).selectedTripId.value)

        viewModel.deleteTrip("trip-28")
        assertNull(viewModel.selectedTripId.value)
    }
}

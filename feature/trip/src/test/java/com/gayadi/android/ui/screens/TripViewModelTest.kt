package com.gayadi.android.ui.screens

import androidx.lifecycle.SavedStateHandle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TripViewModelTest {
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

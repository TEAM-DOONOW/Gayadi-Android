package com.gayadi.android.ui.screens

import androidx.lifecycle.SavedStateHandle
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TripCreateViewModelTest {
    @Test
    fun inputStateIsRestoredFromSavedStateHandle() {
        val savedStateHandle = SavedStateHandle()
        val viewModel = TripCreateViewModel(savedStateHandle)

        viewModel.selectTravelType(TripTravelType.SOLO)
        viewModel.showCityStep()
        viewModel.toggleCity("제주")
        viewModel.showDetailsStep()
        viewModel.updateName("여름 제주 여행")
        viewModel.selectDate(TripDateField.START, LocalDate.of(2026, 8, 24))
        viewModel.selectDate(TripDateField.END, LocalDate.of(2026, 8, 27))

        val restored = TripCreateViewModel(savedStateHandle).uiState.value

        assertEquals(TripCreateStep.DETAILS, restored.step)
        assertEquals(TripTravelType.SOLO, restored.travelType)
        assertEquals(listOf("제주"), restored.selectedCities)
        assertEquals("여름 제주 여행", restored.name)
        assertEquals(LocalDate.of(2026, 8, 24), restored.startDate)
        assertEquals(LocalDate.of(2026, 8, 27), restored.endDate)
        assertNull(restored.selectingDateField)
    }

    @Test
    fun selectingLaterStartDateClearsInvalidEndDate() {
        val viewModel = TripCreateViewModel(SavedStateHandle())
        viewModel.selectDate(TripDateField.START, LocalDate.of(2026, 8, 20))
        viewModel.selectDate(TripDateField.END, LocalDate.of(2026, 8, 22))

        viewModel.selectDate(TripDateField.START, LocalDate.of(2026, 8, 25))

        assertEquals(LocalDate.of(2026, 8, 25), viewModel.uiState.value.startDate)
        assertNull(viewModel.uiState.value.endDate)
    }

    @Test
    fun editStateUsesExistingTripValues() {
        val trip = TripSummary(
            id = "trip-1",
            name = "친구 여행",
            startDate = "",
            endDate = "",
            cities = listOf("서울", "인천"),
            coverImageResList = emptyList(),
            isGroupTrip = true,
        )

        val state = TripCreateViewModel(SavedStateHandle(), trip).uiState.value

        assertTrue(state.isEditing)
        assertTrue(state.isGroupTrip)
        assertEquals(TripCreateStep.DETAILS, state.step)
        assertEquals(listOf("서울", "인천"), state.selectedCities)
        assertEquals("친구 여행", state.name)
        assertFalse(state.isSubmitting)
    }
}

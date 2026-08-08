package com.gayadi.android.ui.screens

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.nio.charset.StandardCharsets
import java.util.Base64
import java.util.UUID

class TripViewModel(private val savedStateHandle: SavedStateHandle) : ViewModel() {
    private val _trips = MutableStateFlow(decodeTrips(savedStateHandle[TRIPS_KEY] ?: EMPTY_TRIPS_JSON))
    val trips = _trips.asStateFlow()
    private val _selectedTripId = MutableStateFlow<String?>(savedStateHandle[SELECTED_TRIP_ID_KEY])
    val selectedTripId = _selectedTripId.asStateFlow()

    fun addTrip(trip: TripSummary) {
        persistTrips(listOf(trip) + trips.value)
    }

    fun deleteTrip(tripId: String) {
        persistTrips(trips.value.filterNot { it.id == tripId })
        if (selectedTripId.value == tripId) {
            savedStateHandle[SELECTED_TRIP_ID_KEY] = null
            _selectedTripId.value = null
        }
    }

    fun selectTrip(tripId: String) {
        if (trips.value.any { it.id == tripId }) {
            savedStateHandle[SELECTED_TRIP_ID_KEY] = tripId
            _selectedTripId.value = tripId
        }
    }

    fun tripById(tripId: String): TripSummary? = trips.value.find { it.id == tripId }

    private fun persistTrips(value: List<TripSummary>) {
        savedStateHandle[TRIPS_KEY] = encodeTrips(value)
        _trips.value = value
    }

    private fun encodeTrips(trips: List<TripSummary>): String = trips.joinToString(RECORD_SEPARATOR) { trip ->
        listOf(
            trip.id,
            trip.name,
            trip.startDate,
            trip.endDate,
            trip.cities.joinToString(LIST_SEPARATOR),
            trip.coverImageResList.joinToString(LIST_SEPARATOR),
        ).joinToString(FIELD_SEPARATOR, transform = ::encodeField)
    }

    private fun decodeTrips(json: String): List<TripSummary> = runCatching {
        if (json.isBlank()) return@runCatching emptyList()
        json.split(RECORD_SEPARATOR).map { record ->
            val fields = record.split(FIELD_SEPARATOR).map(::decodeField)
            require(fields.size == FIELD_COUNT)
            TripSummary(
                id = fields[0].ifBlank { UUID.randomUUID().toString() },
                name = fields[1],
                startDate = fields[2],
                endDate = fields[3],
                cities = fields[4].split(LIST_SEPARATOR).filter(String::isNotBlank),
                coverImageResList = fields[5].split(LIST_SEPARATOR).mapNotNull(String::toIntOrNull),
            )
        }
    }.getOrDefault(emptyList())

    private fun encodeField(value: String): String =
        Base64.getUrlEncoder().withoutPadding().encodeToString(value.toByteArray(StandardCharsets.UTF_8))

    private fun decodeField(value: String): String =
        String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8)

    private companion object {
        const val TRIPS_KEY = "saved_trips"
        const val EMPTY_TRIPS_JSON = ""
        const val SELECTED_TRIP_ID_KEY = "selected_trip_id"
        const val RECORD_SEPARATOR = "\n"
        const val FIELD_SEPARATOR = "|"
        const val LIST_SEPARATOR = "\u001F"
        const val FIELD_COUNT = 6
    }
}

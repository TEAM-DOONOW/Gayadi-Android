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

    private fun decodeTrips(value: String): List<TripSummary> = decodeCurrentTrips(value)
        .recoverCatching { decodeLegacyTrips(value) }
        .getOrDefault(emptyList())

    private fun decodeCurrentTrips(value: String): Result<List<TripSummary>> = runCatching {
        if (value.isBlank()) return@runCatching emptyList()
        value.split(RECORD_SEPARATOR).map { record ->
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
    }

    private fun decodeLegacyTrips(value: String): List<TripSummary> {
        require(value.trim().startsWith("["))
        val content = value.trim().removePrefix("[").removeSuffix("]")
        if (content.isBlank()) return emptyList()
        return content.split("},{").map { rawItem ->
            val item = rawItem.removePrefix("{").removeSuffix("}")
            TripSummary(
                id = item.jsonString("id").ifBlank { UUID.randomUUID().toString() },
                name = item.jsonString("name"),
                startDate = item.jsonString("startDate"),
                endDate = item.jsonString("endDate"),
                cities = item.jsonArray("cities").map(::unescapeJson),
                coverImageResList = item.jsonArray("coverImageResList").map(String::toInt),
            )
        }
    }

    private fun String.jsonString(key: String): String {
        val encoded = Regex("\"${Regex.escape(key)}\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"")
            .find(this)?.groupValues?.get(1) ?: error("Missing $key")
        return unescapeJson(encoded)
    }

    private fun String.jsonArray(key: String): List<String> {
        val content = Regex("\"${Regex.escape(key)}\"\\s*:\\s*\\[([^\\]]*)\\]")
            .find(this)?.groupValues?.get(1) ?: error("Missing $key")
        if (content.isBlank()) return emptyList()
        return content.split(',').map { it.trim().removeSurrounding("\"") }
    }

    private fun unescapeJson(value: String): String = value
        .replace("\\\\\"", "\"")
        .replace("\\\\\\\\", "\\")

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

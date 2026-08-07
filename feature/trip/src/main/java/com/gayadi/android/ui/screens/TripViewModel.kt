package com.gayadi.android.ui.screens

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class TripViewModel(private val savedStateHandle: SavedStateHandle) : ViewModel() {
    private val storedTrips = savedStateHandle.getStateFlow(TRIPS_KEY, EMPTY_TRIPS_JSON)

    val trips = storedTrips
        .map(::decodeTrips)
        .stateIn(viewModelScope, SharingStarted.Eagerly, decodeTrips(storedTrips.value))

    fun addTrip(trip: TripSummary) {
        savedStateHandle[TRIPS_KEY] = encodeTrips(listOf(trip) + trips.value)
    }

    fun deleteTrip(tripId: String) {
        savedStateHandle[TRIPS_KEY] = encodeTrips(trips.value.filterNot { it.id == tripId })
    }

    private fun encodeTrips(trips: List<TripSummary>): String = JSONArray().apply {
        trips.forEach { trip ->
            put(JSONObject().apply {
                put("id", trip.id)
                put("name", trip.name)
                put("startDate", trip.startDate)
                put("endDate", trip.endDate)
                put("cities", JSONArray(trip.cities))
                put("coverImageResList", JSONArray(trip.coverImageResList))
            })
        }
    }.toString()

    private fun decodeTrips(json: String): List<TripSummary> = runCatching {
        val array = JSONArray(json)
        List(array.length()) { index ->
            val item = array.getJSONObject(index)
            TripSummary(
                id = item.optString("id").ifBlank { UUID.randomUUID().toString() },
                name = item.getString("name"),
                startDate = item.getString("startDate"),
                endDate = item.getString("endDate"),
                cities = item.getJSONArray("cities").toStringList(),
                coverImageResList = item.getJSONArray("coverImageResList").toIntList(),
            )
        }
    }.getOrDefault(emptyList())

    private fun JSONArray.toStringList(): List<String> =
        List(length()) { index -> getString(index) }

    private fun JSONArray.toIntList(): List<Int> =
        List(length()) { index -> getInt(index) }

    private companion object {
        const val TRIPS_KEY = "saved_trips"
        const val EMPTY_TRIPS_JSON = "[]"
    }
}

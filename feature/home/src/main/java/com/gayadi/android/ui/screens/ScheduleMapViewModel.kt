package com.gayadi.android.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.gayadi.android.domain.error.rethrowCancellation
import com.gayadi.android.domain.model.TourPlace
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PlaceCoordinates(val latitude: Double, val longitude: Double)

data class ScheduleMapUiState(
    val coordinates: Map<String, PlaceCoordinates> = emptyMap(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val places: Map<String, TourPlace> = emptyMap(),
)

class ScheduleMapViewModel(private val getPlace: suspend (String) -> TourPlace) : ViewModel() {
    private val _uiState = MutableStateFlow(ScheduleMapUiState())
    val uiState = _uiState.asStateFlow()
    private var job: Job? = null
    private var version = 0
    private val cache = mutableMapOf<String, PlaceCoordinates>()
    private val placeCache = mutableMapOf<String, TourPlace>()
    private var lastIds: List<String>? = null

    fun load(placeIds: List<String>, retry: Boolean = false) {
        val ids = placeIds.distinct()
        if (!retry && lastIds == ids) return
        lastIds = ids
        job?.cancel()
        val requestVersion = ++version
        val coordinates = cache.filterKeys { it in ids }.toMutableMap()
        _uiState.value = ScheduleMapUiState(coordinates.toMap(), isLoading = ids.any { it !in coordinates }, places = placeCache.filterKeys { it in ids })
        job = viewModelScope.launch {
            for (id in ids.filterNot(coordinates::containsKey)) {
                try {
                    val place = getPlace(id)
                    if (requestVersion != version) return@launch
                    if (place.contentId == id) placeCache[id] = place
                    val lat = place.latitude
                    val lng = place.longitude
                    if (place.contentId == id && lat != null && lng != null &&
                        lat.isFinite() && lng.isFinite() && lat in -90.0..90.0 && lng in -180.0..180.0
                    ) {
                        val point = PlaceCoordinates(lat, lng)
                        cache[id] = point
                        coordinates[id] = point
                    }
                } catch (error: Exception) {
                    error.rethrowCancellation()
                }
            }
            if (requestVersion == version) {
                _uiState.value = ScheduleMapUiState(
                    coordinates = coordinates.toMap(),
                    places = placeCache.filterKeys { it in ids },
                    errorMessage = if (coordinates.size < ids.size) "일부 장소의 위치를 불러오지 못했어요." else null,
                )
            }
        }
    }

    companion object {
        fun factory(getPlace: suspend (String) -> TourPlace) = viewModelFactory {
            initializer { ScheduleMapViewModel(getPlace) }
        }
    }
}

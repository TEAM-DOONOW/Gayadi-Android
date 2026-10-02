package com.gayadi.android.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.gayadi.android.domain.error.rethrowCancellation
import com.gayadi.android.domain.error.userFacingMessage
import com.gayadi.android.domain.model.RouteTransportMode
import com.gayadi.android.domain.repository.RecommendedItinerary
import com.gayadi.android.domain.repository.TripSupportGateway
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalTime

data class ItineraryRouteUiState(
    val date: String,
    val startTime: String = "10:00",
    val endTime: String = "18:00",
    val transportMode: RouteTransportMode = RouteTransportMode.PUBLIC_TRANSIT,
    val variation: Int = 0,
    val recommendation: RecommendedItinerary? = null,
    val isLoading: Boolean = false,
    val isApplying: Boolean = false,
    val errorMessage: String? = null,
    val applyErrorMessage: String? = null,
)

class ItineraryRouteViewModel(
    private val gateway: TripSupportGateway,
    private val tripId: String,
    date: String,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ItineraryRouteUiState(date = date))
    val uiState: StateFlow<ItineraryRouteUiState> = _uiState.asStateFlow()
    private var recommendationJob: Job? = null

    fun recommend(forceNewRoute: Boolean = false) {
        if (_uiState.value.isApplying) return
        if (forceNewRoute) _uiState.update { it.copy(variation = (it.variation + 1).coerceAtMost(50)) }
        val request = _uiState.value
        recommendationJob?.cancel()
        _uiState.update {
            it.copy(
                recommendation = null,
                isLoading = true,
                errorMessage = null,
                applyErrorMessage = null,
            )
        }
        recommendationJob = viewModelScope.launch(ioDispatcher) {
            runCatching {
                gateway.recommendItinerary(
                    tripId = tripId,
                    date = request.date,
                    startTime = request.startTime,
                    endTime = request.endTime,
                    transportMode = request.transportMode,
                    variation = request.variation,
                )
            }.fold(
                onSuccess = { route ->
                    _uiState.update {
                        it.copy(
                            recommendation = route,
                            isLoading = false,
                            errorMessage = null,
                            applyErrorMessage = null,
                        )
                    }
                },
                onFailure = { error ->
                    error.rethrowCancellation()
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.userFacingMessage("여행루트를 만들지 못했어요. 다시 시도해 주세요."),
                        )
                    }
                },
            )
        }
    }

    fun selectTransportMode(mode: RouteTransportMode) {
        if (_uiState.value.isApplying) return
        if (_uiState.value.transportMode == mode) return
        _uiState.update { it.copy(transportMode = mode, variation = 0) }
        recommend()
    }

    fun selectTimeRange(startTime: String, endTime: String) {
        if (_uiState.value.isApplying) return
        if (_uiState.value.startTime == startTime && _uiState.value.endTime == endTime) return
        if (!isValidTimeRange(startTime, endTime)) {
            recommendationJob?.cancel()
            _uiState.update {
                it.copy(
                    startTime = startTime,
                    endTime = endTime,
                    variation = 0,
                    recommendation = null,
                    isLoading = false,
                    errorMessage = "종료 시간은 시작 시간보다 3~12시간 뒤로 골라 주세요.",
                    applyErrorMessage = null,
                )
            }
            return
        }
        _uiState.update { it.copy(startTime = startTime, endTime = endTime, variation = 0) }
        recommend()
    }

    private fun isValidTimeRange(startTime: String, endTime: String): Boolean = runCatching {
        Duration.between(LocalTime.parse(startTime), LocalTime.parse(endTime)).toMinutes() in 180..720
    }.getOrDefault(false)

    fun apply(onApplied: () -> Unit) {
        val request = _uiState.value
        if (request.isLoading || request.isApplying || request.recommendation == null) return
        _uiState.update { it.copy(isApplying = true, applyErrorMessage = null) }
        viewModelScope.launch(ioDispatcher) {
            runCatching {
                gateway.applyItinerary(
                    tripId = tripId,
                    date = request.date,
                    startTime = request.startTime,
                    endTime = request.endTime,
                    transportMode = request.transportMode,
                    variation = request.variation,
                    expectedPlaceIds = request.recommendation.stops.map { it.placeId },
                )
            }.fold(
                onSuccess = {
                    _uiState.update { state ->
                        state.copy(
                            isApplying = false,
                            recommendation = it,
                            applyErrorMessage = null,
                        )
                    }
                    viewModelScope.launch { onApplied() }
                },
                onFailure = { error ->
                    error.rethrowCancellation()
                    _uiState.update {
                        it.copy(
                            isApplying = false,
                            applyErrorMessage = error.userFacingMessage("추천 루트를 일정에 반영하지 못했어요."),
                        )
                    }
                },
            )
        }
    }

    companion object {
        fun factory(gateway: TripSupportGateway, tripId: String, date: String): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    ItineraryRouteViewModel(gateway, tripId, date) as T
            }
    }
}

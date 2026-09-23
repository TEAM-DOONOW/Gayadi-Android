package com.gayadi.android.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.gayadi.android.domain.error.rethrowCancellation
import com.gayadi.android.domain.error.userFacingMessage
import com.gayadi.android.domain.model.RouteTransportMode
import com.gayadi.android.domain.model.ScheduleType
import com.gayadi.android.domain.model.TourPlace
import com.gayadi.android.domain.model.TravelSchedule
import com.gayadi.android.domain.repository.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CandidateSearchContext(
    val tripId: String,
    val date: String,
    val region: String,
    val previous: TravelSchedule?,
    val next: TravelSchedule?,
)

fun candidateSearchContext(
    tripId: String, date: String, region: String, schedules: List<TravelSchedule>, beforeScheduleId: String?,
): CandidateSearchContext {
    val visits = schedules.filter { it.tripId == tripId && it.date == date && it.type == ScheduleType.MAIN && it.placeId != null }
        .sortedBy { it.order }
    val index = visits.indexOfFirst { it.id == beforeScheduleId }.takeIf { it >= 0 } ?: visits.size
    return CandidateSearchContext(tripId, date, region, visits.getOrNull(index - 1), visits.getOrNull(index))
}

class PlaceCandidateViewModel(
    private val gateway: PlaceCandidateGateway,
    private val getPlace: suspend (String) -> TourPlace,
    initialMode: RouteTransportMode? = null,
) : ViewModel() {
    private val _uiState = MutableStateFlow(PlaceUiState(
        serverOrdered = true, transportMode = initialMode,
        sort = if (initialMode == null) PlaceSort.RECENT else PlaceSort.TRAVEL_TIME,
    ))
    val uiState = _uiState.asStateFlow()
    private var context: CandidateSearchContext? = null
    private var job: Job? = null
    private var version = 0
    private var cursor: String? = null
    private val coordinates = mutableMapOf<String, PlaceCoordinate>()

    fun configure(value: CandidateSearchContext) {
        if (context == value) return
        context = value
        _uiState.update { it.copy(regionName = value.region) }
        search()
    }

    fun updateQuery(query: String) { _uiState.update { it.copy(query = query) }; search(debounce = true) }
    fun selectCategory(category: String) { _uiState.update { it.copy(selectedCategory = category) }; search() }
    fun selectTransportMode(mode: RouteTransportMode?) {
        if (_uiState.value.transportMode == mode) return
        _uiState.update { it.copy(transportMode = mode, sort = if (mode == null) PlaceSort.RECENT else PlaceSort.TRAVEL_TIME) }
        search()
    }
    fun selectSort(sort: PlaceSort) {
        if (sort == PlaceSort.TRAVEL_TIME && _uiState.value.transportMode == null) return
        if (sort == _uiState.value.sort) return
        _uiState.update { it.copy(sort = sort) }
        search()
    }
    fun retry() = search()
    fun loadMore() {
        val state = _uiState.value
        if (state.sort == PlaceSort.RECENT && state.hasNext && !state.isLoading && !state.isLoadingMore && cursor != null) search(append = true)
    }

    private fun search(debounce: Boolean = false, append: Boolean = false) {
        val originContext = context ?: return
        job?.cancel()
        val requestVersion = ++version
        if (!append) cursor = null
        val state = _uiState.value
        _uiState.update {
            it.copy(isLoading = !append, isLoadingMore = append, errorMessage = null,
                places = if (append) it.places else emptyList(), limited = false, hasNext = false, originAvailable = null)
        }
        job = viewModelScope.launch {
            try {
                if (debounce) delay(300)
                val origin = if (state.sort == PlaceSort.TRAVEL_TIME) resolve(originContext.previous) else null
                val next = if (origin != null && originContext.next != null) {
                    resolve(originContext.next) ?: error("다음 방문지의 위치가 없어 이동시간을 비교할 수 없어요. 최신순으로 찾아주세요.")
                } else null
                if (requestVersion != version) return@launch
                _uiState.update { it.copy(originAvailable = origin != null) }
                val page = gateway.search(PlaceCandidateQuery(
                    query = state.query, region = originContext.region, category = categoryCode(state.selectedCategory),
                    sort = state.sort, transportMode = state.transportMode ?: RouteTransportMode.PUBLIC_TRANSIT,
                    origin = origin, next = next, cursor = if (append) cursor else null,
                ))
                if (requestVersion != version) return@launch
                cursor = page.nextCursor
                _uiState.update {
                    it.copy(places = (if (append) it.places else emptyList()) + page.items.map(PlaceCandidate::toItem),
                        isLoading = false, isLoadingMore = false, rankingSort = page.sort, limited = page.limited,
                        hasNext = state.sort == PlaceSort.RECENT && page.hasNext && page.nextCursor != null)
                }
            } catch (error: Exception) {
                error.rethrowCancellation()
                if (requestVersion == version) {
                    _uiState.update { it.copy(isLoading = false, isLoadingMore = false,
                        errorMessage = error.userFacingMessage("장소를 불러오지 못했어요. 다시 시도해 주세요.")) }
                }
            }
        }
    }

    private suspend fun resolve(schedule: TravelSchedule?): PlaceCoordinate? {
        schedule ?: return null
        validCoordinate(schedule.latitude, schedule.longitude)?.let { return it }
        val id = schedule.placeId ?: return null
        coordinates[id]?.let { return it }
        val place = getPlace(id)
        check(place.contentId == id) { "선택한 장소의 위치를 확인할 수 없어요." }
        return validCoordinate(place.latitude, place.longitude)?.also { coordinates[id] = it }
    }

    companion object {
        fun factory(gateway: PlaceCandidateGateway, getPlace: suspend (String) -> TourPlace, initialMode: RouteTransportMode? = null) = viewModelFactory {
            initializer { PlaceCandidateViewModel(gateway, getPlace, initialMode) }
        }
    }
}

private fun validCoordinate(latitude: Double?, longitude: Double?): PlaceCoordinate? =
    if (latitude == null || longitude == null) null else runCatching { PlaceCoordinate(latitude, longitude) }.getOrNull()

private fun categoryCode(label: String): String? = when (label) {
    "맛집" -> "RESTAURANT"
    "카페" -> "CAFE"
    "숙소" -> "ACCOMMODATION"
    "관광명소" -> "ATTRACTION"
    else -> null
}

private fun PlaceCandidate.toItem(): PlaceItem {
    val category = when (categoryCode) {
        "RESTAURANT" -> "맛집"
        "CAFE" -> "카페"
        "ACCOMMODATION" -> "숙소"
        else -> "관광명소"
    }
    return PlaceItem(
        id = place.contentId, name = place.title, category = category, rating = 0.0, reviews = 0,
        crowdLevel = when (place.crowdLevel) { "RELAXED" -> CrowdLevel.RELAXED; "CROWDED" -> CrowdLevel.CROWDED; else -> CrowdLevel.NORMAL },
        emoji = when (category) { "맛집" -> "🍲"; "카페" -> "☕"; "숙소" -> "🏨"; else -> "🏞️" },
        description = listOf(place.address, place.addressDetail).filter(String::isNotBlank).joinToString(" "),
        imageUrl = place.imageUrl, latitude = place.latitude, longitude = place.longitude,
        hasRealtimeDetails = place.crowdProviderDataAvailable, travelTime = travelTime,
    )
}

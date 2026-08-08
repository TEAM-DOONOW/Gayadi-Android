package com.gayadi.android.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class CrowdLevel(val label: String) { RELAXED("여유"), NORMAL("보통"), CROWDED("혼잡") }

data class PlaceItem(
    val id: String,
    val name: String,
    val category: String,
    val rating: Double,
    val reviews: Int,
    val crowdLevel: CrowdLevel,
    val emoji: String,
    val description: String,
    val distanceMeters: Int = 500,
    val weather: String = "맑음",
    val temperatureCelsius: Int = 23,
    val rainProbability: Int = 10,
)

data class PlaceUiState(
    val query: String = "",
    val selectedCategory: String = "전체",
    val places: List<PlaceItem> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
) {
    val filteredPlaces: List<PlaceItem>
        get() = places.filter { place ->
            (selectedCategory == "전체" || place.category == selectedCategory) &&
                (query.isBlank() || place.name.contains(query, ignoreCase = true) ||
                    place.description.contains(query, ignoreCase = true))
        }
}

interface PlaceRepository {
    fun getPlaces(): Result<List<PlaceItem>>
    fun getNearbyPlaces(originPlaceId: String?): Result<List<PlaceItem>>
}

class FakePlaceRepository : PlaceRepository {
    override fun getPlaces(): Result<List<PlaceItem>> = Result.success(
        listOf(
            PlaceItem("place-1", "명진전복", "맛집", 4.5, 1284, CrowdLevel.RELAXED, "🍲", "제주 성산 전복 요리", 320, "맑음", 24, 10),
            PlaceItem("place-2", "카페 글렌코", "카페", 4.4, 892, CrowdLevel.NORMAL, "☕", "제주 오션뷰 카페", 680, "구름 많음", 23, 20),
            PlaceItem("place-3", "섭지코지", "관광명소", 4.7, 3561, CrowdLevel.CROWDED, "🏞️", "제주 동부 해안 산책", 1_200, "바람", 21, 30),
            PlaceItem("place-4", "스테이 성산", "숙소", 4.6, 421, CrowdLevel.RELAXED, "🏨", "성산일출봉 인근 숙소", 900, "맑음", 22, 10),
        ),
    )

    override fun getNearbyPlaces(originPlaceId: String?): Result<List<PlaceItem>> =
        getPlaces().map { places -> places.filterNot { it.id == originPlaceId }.sortedBy(PlaceItem::distanceMeters) }
}

class PlaceViewModel(
    private val repository: PlaceRepository = FakePlaceRepository(),
) : ViewModel() {
    private val _uiState = MutableStateFlow(PlaceUiState())
    val uiState: StateFlow<PlaceUiState> = _uiState.asStateFlow()

    init {
        loadPlaces()
    }

    fun updateQuery(query: String) = _uiState.update { it.copy(query = query) }

    fun selectCategory(category: String) = _uiState.update { it.copy(selectedCategory = category) }

    fun retry() = loadPlaces()

    fun findPlace(placeId: String): PlaceItem? = _uiState.value.places.find { it.id == placeId }

    fun nearbyPlaces(originPlaceId: String?): List<PlaceItem> =
        repository.getNearbyPlaces(originPlaceId).getOrDefault(emptyList())

    private fun loadPlaces() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        repository.getPlaces().fold(
            onSuccess = { places -> _uiState.update { it.copy(places = places, isLoading = false) } },
            onFailure = { error ->
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = error.message ?: "장소를 불러오지 못했습니다.")
                }
            },
        )
    }

    companion object {
        fun factory(repository: PlaceRepository = FakePlaceRepository()) = viewModelFactory {
            initializer { PlaceViewModel(repository) }
        }
    }
}

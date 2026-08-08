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
    val regionName: String = "제주 성산",
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
    fun getPlaces(regionName: String = "제주 성산"): Result<List<PlaceItem>>
}

class FakePlaceRepository : PlaceRepository {
    override fun getPlaces(regionName: String): Result<List<PlaceItem>> = Result.success(
        when (regionName) {
            "제주", "서귀포", "제주 성산" -> listOf(
                PlaceItem("place-1", "명진전복", "맛집", 4.5, 1284, CrowdLevel.RELAXED, "🍲", "제주 성산 전복 요리", 320, "맑음", 24, 10),
                PlaceItem("place-2", "카페 글렌코", "카페", 4.4, 892, CrowdLevel.NORMAL, "☕", "제주 오션뷰 카페", 680, "구름 많음", 23, 20),
                PlaceItem("place-3", "섭지코지", "관광명소", 4.7, 3561, CrowdLevel.CROWDED, "🏞️", "제주 동부 해안 산책", 1_200, "바람", 21, 30),
                PlaceItem("place-4", "스테이 성산", "숙소", 4.6, 421, CrowdLevel.RELAXED, "🏨", "성산일출봉 인근 숙소", 900, "맑음", 22, 10),
            )
            "서울" -> listOf(
                PlaceItem("seoul-place-1", "광장시장", "맛집", 4.5, 2418, CrowdLevel.CROWDED, "🍜", "서울 종로의 전통시장 먹거리", 350, "맑음", 26, 10),
                PlaceItem("seoul-place-2", "서울숲 카페거리", "카페", 4.4, 1372, CrowdLevel.NORMAL, "☕", "서울 성수의 감성 카페 거리", 720, "구름 많음", 25, 20),
                PlaceItem("seoul-place-3", "경복궁", "관광명소", 4.8, 5230, CrowdLevel.CROWDED, "🏯", "서울을 대표하는 조선 왕궁", 1_100, "맑음", 25, 10),
                PlaceItem("seoul-place-4", "서울역 스테이", "숙소", 4.3, 684, CrowdLevel.RELAXED, "🏨", "서울역 인근 도심 숙소", 880, "맑음", 24, 10),
            )
            else -> regionalPlaces(regionName)
        },
    )
}

class PlaceViewModel(
    private val repository: PlaceRepository = FakePlaceRepository(),
) : ViewModel() {
    private val _uiState = MutableStateFlow(PlaceUiState())
    private val knownPlaces = mutableMapOf<String, PlaceItem>()
    val uiState: StateFlow<PlaceUiState> = _uiState.asStateFlow()

    init {
        loadPlaces()
    }

    fun updateQuery(query: String) = _uiState.update { it.copy(query = query) }

    fun selectCategory(category: String) = _uiState.update { it.copy(selectedCategory = category) }

    fun retry() = loadPlaces()

    fun setRegion(regionName: String) {
        val resolvedRegion = regionName.ifBlank { "제주 성산" }
        if (resolvedRegion == _uiState.value.regionName) return
        _uiState.update {
            it.copy(regionName = resolvedRegion, query = "", selectedCategory = "전체")
        }
        loadPlaces()
    }

    fun findPlace(placeId: String): PlaceItem? = knownPlaces[placeId]

    fun nearbyPlaces(originPlaceId: String?): List<PlaceItem> =
        _uiState.value.places.filterNot { it.id == originPlaceId }.sortedBy(PlaceItem::distanceMeters)

    private fun loadPlaces() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        repository.getPlaces(_uiState.value.regionName).fold(
            onSuccess = { places ->
                knownPlaces.putAll(places.associateBy(PlaceItem::id))
                _uiState.update { it.copy(places = places, isLoading = false) }
            },
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

private fun regionalPlaces(regionName: String): List<PlaceItem> {
    val regionKey = regionName.hashCode().toUInt().toString(16)
    return listOf(
        PlaceItem("$regionKey-place-1", "$regionName 향토식당", "맛집", 4.5, 824, CrowdLevel.NORMAL, "🍲", "$regionName 대표 향토 음식", 340, "맑음", 24, 10),
        PlaceItem("$regionKey-place-2", "$regionName 전망 카페", "카페", 4.4, 536, CrowdLevel.RELAXED, "☕", "$regionName 풍경을 즐기는 카페", 670, "구름 많음", 23, 20),
        PlaceItem("$regionKey-place-3", "$regionName 대표 명소", "관광명소", 4.7, 1940, CrowdLevel.CROWDED, "🏞️", "$regionName 여행의 대표 관광지", 1_150, "맑음", 24, 10),
        PlaceItem("$regionKey-place-4", "$regionName 스테이", "숙소", 4.4, 392, CrowdLevel.RELAXED, "🏨", "$regionName 중심가의 편안한 숙소", 860, "맑음", 22, 10),
    )
}

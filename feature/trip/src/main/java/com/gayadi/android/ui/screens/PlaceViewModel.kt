package com.gayadi.android.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.gayadi.android.domain.model.TourPlace
import com.gayadi.android.domain.usecase.GetNearbyTourPlacesUseCase
import com.gayadi.android.domain.usecase.GetTourPlacesUseCase
import com.gayadi.android.domain.usecase.SearchTourPlacesUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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
    val imageUrl: String = "",
    val longitude: Double? = null,
    val latitude: Double? = null,
    val hasRealtimeDetails: Boolean = true,
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

data class NearbyPlacesUiState(
    val places: List<PlaceItem> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)

interface PlaceRepository {
    suspend fun getPlaces(regionName: String = "제주 성산"): Result<List<PlaceItem>>

    suspend fun searchPlaces(regionName: String, keyword: String): Result<List<PlaceItem>> =
        getPlaces(regionName).map { places ->
            places.filter { place ->
                place.name.contains(keyword, ignoreCase = true) ||
                    place.description.contains(keyword, ignoreCase = true)
            }
        }
}

class FakePlaceRepository : PlaceRepository {
    fun places(regionName: String = "제주 성산"): Result<List<PlaceItem>> = Result.success(
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

    override suspend fun getPlaces(regionName: String): Result<List<PlaceItem>> = places(regionName)
}

class TourApiPlaceRepository(
    private val getTourPlaces: GetTourPlacesUseCase,
    private val searchTourPlaces: SearchTourPlacesUseCase? = null,
) : PlaceRepository {
    override suspend fun getPlaces(regionName: String): Result<List<PlaceItem>> {
        val placesByContentId = linkedMapOf<String, PrioritizedPlaceItem>()
        TOUR_PLACE_REQUESTS.forEach { request ->
            val categoryPlaces = getTourPlaces(
                contentTypeId = request.contentTypeId,
                lclsSystm1 = request.lclsSystm1,
                lclsSystm2 = request.lclsSystm2,
                maxPages = MAX_PAGES_PER_CATEGORY,
            ).getOrElse { error ->
                if (error is CancellationException) throw error
                return Result.failure(error)
            }
            categoryPlaces.forEach { place ->
                val existing = placesByContentId[place.contentId]
                if (existing == null || request.priority > existing.priority) {
                    placesByContentId[place.contentId] = PrioritizedPlaceItem(
                        item = place.toPlaceItem(
                            forcedCategory = request.forcedCategory,
                            fallbackContentTypeId = request.contentTypeId,
                        ),
                        priority = request.priority,
                    )
                }
            }
        }
        return Result.success(placesByContentId.values.map(PrioritizedPlaceItem::item))
    }

    override suspend fun searchPlaces(regionName: String, keyword: String): Result<List<PlaceItem>> {
        val search = searchTourPlaces ?: return getPlaces(regionName).map { places ->
            places.filter { it.name.contains(keyword, ignoreCase = true) }
        }
        return search(keyword = keyword, arrange = "C", maxPages = 1).map { places ->
            places.map { it.toPlaceItem(forcedCategory = null, fallbackContentTypeId = it.contentTypeId.toIntOrNull() ?: 12) }
        }
    }

    private fun TourPlace.toPlaceItem(
        forcedCategory: TourPlaceCategory?,
        fallbackContentTypeId: Int,
    ): PlaceItem {
        val placeCategory = forcedCategory ?: category(fallbackContentTypeId)
        return PlaceItem(
            id = contentId,
            name = title,
            category = placeCategory.label,
            rating = 0.0,
            reviews = 0,
            crowdLevel = CrowdLevel.NORMAL,
            emoji = placeCategory.emoji,
            description = listOf(address, addressDetail)
                .filter(String::isNotBlank)
                .joinToString(" "),
            imageUrl = imageUrl,
            longitude = longitude,
            latitude = latitude,
            hasRealtimeDetails = false,
        )
    }

    private fun TourPlace.category(fallbackContentTypeId: Int): TourPlaceCategory =
        when (contentTypeId.trim().ifBlank { fallbackContentTypeId.toString() }) {
            TOURIST_ATTRACTION_CONTENT_TYPE_ID.toString() -> TourPlaceCategory.TOURIST_ATTRACTION
            STAY_CONTENT_TYPE_ID.toString() -> TourPlaceCategory.STAY
            RESTAURANT_CONTENT_TYPE_ID.toString() -> {
                val foodCategoryLevel2 = lclsSystm2.trim()
                val foodCategoryLevel3 = lclsSystm3.trim()
                val hasStructuredFoodCategory =
                    foodCategoryLevel2.isNotBlank() || foodCategoryLevel3.isNotBlank()
                val isStructuredCafe = foodCategoryLevel2.startsWith(CAFE_CATEGORY_PREFIX, ignoreCase = true) ||
                    foodCategoryLevel3.startsWith(CAFE_CATEGORY_PREFIX, ignoreCase = true)
                val isFallbackCafe = !hasStructuredFoodCategory &&
                    CAFE_TITLE_KEYWORDS.any { title.contains(it, ignoreCase = true) }
                if (isStructuredCafe || isFallbackCafe) {
                    TourPlaceCategory.CAFE
                } else {
                    TourPlaceCategory.RESTAURANT
                }
            }
            else -> TourPlaceCategory.TOURIST_ATTRACTION
        }

    private data class PrioritizedPlaceItem(
        val item: PlaceItem,
        val priority: Int,
    )

    private enum class TourPlaceCategory(
        val label: String,
        val emoji: String,
    ) {
        TOURIST_ATTRACTION("관광명소", "🏞️"),
        RESTAURANT("맛집", "🍲"),
        CAFE("카페", "☕"),
        STAY("숙소", "🏨"),
    }

    private data class TourPlaceRequest(
        val contentTypeId: Int,
        val lclsSystm1: String? = null,
        val lclsSystm2: String? = null,
        val forcedCategory: TourPlaceCategory? = null,
        val priority: Int = GENERIC_RESULT_PRIORITY,
    )

    private companion object {
        const val TOURIST_ATTRACTION_CONTENT_TYPE_ID = 12
        const val STAY_CONTENT_TYPE_ID = 32
        const val RESTAURANT_CONTENT_TYPE_ID = 39
        const val RESTAURANT_CATEGORY_PREFIX = "FD01"
        const val CAFE_CATEGORY_PREFIX = "FD05"
        const val FOOD_CATEGORY_PREFIX = "FD"
        const val MAX_PAGES_PER_CATEGORY = 1
        const val GENERIC_RESULT_PRIORITY = 0
        const val EXACT_RESTAURANT_RESULT_PRIORITY = 1
        const val EXACT_CAFE_RESULT_PRIORITY = 2
        val TOUR_PLACE_REQUESTS = listOf(
            TourPlaceRequest(contentTypeId = TOURIST_ATTRACTION_CONTENT_TYPE_ID),
            TourPlaceRequest(contentTypeId = RESTAURANT_CONTENT_TYPE_ID),
            TourPlaceRequest(
                contentTypeId = RESTAURANT_CONTENT_TYPE_ID,
                lclsSystm1 = FOOD_CATEGORY_PREFIX,
                lclsSystm2 = RESTAURANT_CATEGORY_PREFIX,
                forcedCategory = TourPlaceCategory.RESTAURANT,
                priority = EXACT_RESTAURANT_RESULT_PRIORITY,
            ),
            TourPlaceRequest(
                contentTypeId = RESTAURANT_CONTENT_TYPE_ID,
                lclsSystm1 = FOOD_CATEGORY_PREFIX,
                lclsSystm2 = CAFE_CATEGORY_PREFIX,
                forcedCategory = TourPlaceCategory.CAFE,
                priority = EXACT_CAFE_RESULT_PRIORITY,
            ),
            TourPlaceRequest(contentTypeId = STAY_CONTENT_TYPE_ID),
        )
        val CAFE_TITLE_KEYWORDS = listOf(
            "카페",
            "커피",
            "cafe",
            "coffee",
            "로스터리",
            "베이커리",
            "bakery",
            "디저트",
            "티룸",
            "찻집",
        )
    }
}

class PlaceViewModel(
    private val repository: PlaceRepository = FakePlaceRepository(),
    private val getNearbyTourPlaces: GetNearbyTourPlacesUseCase? = null,
) : ViewModel() {
    private val _uiState = MutableStateFlow(PlaceUiState())
    private val knownPlaces = mutableMapOf<String, PlaceItem>()
    private val _nearbyUiState = MutableStateFlow(NearbyPlacesUiState())
    val uiState: StateFlow<PlaceUiState> = _uiState.asStateFlow()
    val nearbyUiState: StateFlow<NearbyPlacesUiState> = _nearbyUiState.asStateFlow()
    private var loadJob: Job? = null
    private var nearbyLoadJob: Job? = null
    private var searchJob: Job? = null

    init {
        loadPlaces()
    }

    fun updateQuery(query: String) {
        _uiState.update { it.copy(query = query) }
        searchJob?.cancel()
        if (query.isBlank()) {
            loadPlaces()
            return
        }
        val regionName = _uiState.value.regionName
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        searchJob = viewModelScope.launch {
            delay(250)
            repository.searchPlaces(regionName, query.trim()).fold(
                onSuccess = { places ->
                    knownPlaces.putAll(places.associateBy(PlaceItem::id))
                    _uiState.update { it.copy(places = places, isLoading = false) }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = error.message ?: "장소를 검색하지 못했습니다.")
                    }
                },
            )
        }
    }

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

    fun loadNearbyPlaces(originPlaceId: String? = null) {
        nearbyLoadJob?.cancel()
        val origin = originPlaceId?.let(knownPlaces::get) ?: _uiState.value.places.firstOrNull()
        if (getNearbyTourPlaces == null || origin?.longitude == null || origin.latitude == null) {
            _nearbyUiState.value = NearbyPlacesUiState(places = nearbyPlaces(originPlaceId))
            return
        }
        _nearbyUiState.value = NearbyPlacesUiState(isLoading = true)
        nearbyLoadJob = viewModelScope.launch {
            getNearbyTourPlaces(
                mapX = origin.longitude.toString(),
                mapY = origin.latitude.toString(),
                radius = NEARBY_RADIUS_METERS,
                arrange = "E",
                maxPages = 1,
            ).fold(
                onSuccess = { places ->
                    val nearby = places
                        .filterNot { it.contentId == origin.id }
                        .map(TourPlace::toNearbyPlaceItem)
                    _nearbyUiState.value = NearbyPlacesUiState(places = nearby)
                },
                onFailure = { error ->
                    _nearbyUiState.value = NearbyPlacesUiState(
                        errorMessage = error.message ?: "주변 장소를 불러오지 못했습니다.",
                    )
                },
            )
        }
    }

    private fun loadPlaces() {
        loadJob?.cancel()
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        val regionName = _uiState.value.regionName
        loadJob = viewModelScope.launch {
            repository.getPlaces(regionName).fold(
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
    }

    companion object {
        private const val NEARBY_RADIUS_METERS = 2_000

        fun factory(repository: PlaceRepository = FakePlaceRepository()) = viewModelFactory {
            initializer { PlaceViewModel(repository) }
        }

        fun factory(getTourPlaces: GetTourPlacesUseCase) =
            factory(TourApiPlaceRepository(getTourPlaces))

        fun factory(
            getTourPlaces: GetTourPlacesUseCase,
            getNearbyTourPlaces: GetNearbyTourPlacesUseCase,
            searchTourPlaces: SearchTourPlacesUseCase,
        ) = viewModelFactory {
            initializer {
                PlaceViewModel(
                    repository = TourApiPlaceRepository(getTourPlaces, searchTourPlaces),
                    getNearbyTourPlaces = getNearbyTourPlaces,
                )
            }
        }
    }

}

private fun TourPlace.toNearbyPlaceItem(): PlaceItem {
    val category = when (contentTypeId.trim()) {
        "32" -> "숙소"
        "39" -> "맛집"
        else -> "관광명소"
    }
    val emoji = when (category) {
        "숙소" -> "🏨"
        "맛집" -> "🍲"
        else -> "🏞️"
    }
    return PlaceItem(
        id = contentId,
        name = title,
        category = category,
        rating = 0.0,
        reviews = 0,
        crowdLevel = CrowdLevel.NORMAL,
        emoji = emoji,
        description = listOf(address, addressDetail).filter(String::isNotBlank).joinToString(" "),
        distanceMeters = distanceMeters ?: 0,
        imageUrl = imageUrl,
        longitude = longitude,
        latitude = latitude,
        hasRealtimeDetails = false,
    )
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

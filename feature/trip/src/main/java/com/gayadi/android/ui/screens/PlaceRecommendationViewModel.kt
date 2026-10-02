package com.gayadi.android.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.gayadi.android.domain.error.rethrowCancellation
import com.gayadi.android.domain.error.userFacingMessage
import com.gayadi.android.domain.model.AgentRecommendation
import com.gayadi.android.domain.model.TourPlace
import com.gayadi.android.domain.repository.AgentGateway
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PlaceRecommendationUiState(
    val recommendations: List<AgentRecommendation> = emptyList(),
    val reasoning: String = "",
    val isLoading: Boolean = false,
    val hasRequested: Boolean = false,
    val errorMessage: String? = null,
    /** 추천 목록 제목(예: "경복궁 다음으로 가기 좋은 곳"). 비어 있으면 일반 추천 제목을 쓴다. */
    val headline: String = "",
    /** 추천 장소 ID별 카드 표시 정보(이미지·주소·좌표). 상세 조회에 실패한 추천은 기본 정보만 쓴다. */
    val places: Map<String, PlaceItem> = emptyMap(),
) {
    /** 목록에 그대로 끼워 넣을 수 있는 추천 카드. 일정에 추가하려면 서버 장소 ID가 필요하다. */
    val recommendedPlaces: List<RecommendedPlace>
        get() = recommendations
            .filter { it.placeId.toLongOrNull()?.let { id -> id > 0 } == true }
            .distinctBy(AgentRecommendation::placeId)
            .map { recommendation ->
                RecommendedPlace(recommendation, places[recommendation.placeId] ?: recommendation.toFallbackPlaceItem())
            }
}

data class RecommendedPlace(
    val recommendation: AgentRecommendation,
    val place: PlaceItem,
)

class PlaceRecommendationViewModel(
    private val gateway: AgentGateway,
    private val placeLookup: (suspend (String) -> TourPlace)? = null,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {
    private val _uiState = MutableStateFlow(PlaceRecommendationUiState())
    val uiState: StateFlow<PlaceRecommendationUiState> = _uiState.asStateFlow()

    fun recommend(
        destination: String,
        profile: String,
        latitude: Double?,
        longitude: Double?,
        keywords: List<String>,
        groupSize: Int,
        force: Boolean = false,
        headline: String = "",
    ) {
        if (_uiState.value.isLoading || _uiState.value.hasRequested && !force) return
        if (latitude == null || longitude == null) {
            _uiState.update {
                it.copy(hasRequested = false, errorMessage = "장소 위치를 불러온 뒤 다시 추천해 주세요.")
            }
            return
        }
        _uiState.update { it.copy(isLoading = true, hasRequested = true, errorMessage = null, headline = headline) }
        viewModelScope.launch(ioDispatcher) {
            runCatching {
                gateway.recommendPlaces(
                    destination = destination,
                    profile = profile.ifBlank { "새로운 여행지를 둘러보고 싶어요." },
                    latitude = latitude,
                    longitude = longitude,
                    keywords = keywords.filter(String::isNotBlank),
                    groupSize = groupSize,
                )
            }.mapCatching { result -> result to resolvePlaces(result.recommendations) }.fold(
                onSuccess = { (result, places) ->
                    _uiState.update {
                        it.copy(
                            recommendations = result.recommendations,
                            reasoning = result.reasoning,
                            isLoading = false,
                            places = places,
                        )
                    }
                },
                onFailure = { error ->
                    error.rethrowCancellation()
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.userFacingMessage("맞춤 장소를 추천하지 못했어요."),
                        )
                    }
                },
            )
        }
    }

    /** 추천 응답에는 이미지가 없어 서버 장소 상세로 카드 정보를 채운다. 실패한 항목은 기본 카드로 보여준다. */
    private suspend fun resolvePlaces(recommendations: List<AgentRecommendation>): Map<String, PlaceItem> {
        val lookup = placeLookup ?: return emptyMap()
        val ids = recommendations.map(AgentRecommendation::placeId)
            .filter { it.toLongOrNull()?.let { id -> id > 0 } == true }
            .distinct()
        return coroutineScope {
            ids.map { id ->
                async {
                    runCatching { lookup(id) }
                        .onFailure { it.rethrowCancellation() }
                        .getOrNull()
                        ?.let { place -> id to place.toNearbyPlaceItem().copy(id = id) }
                }
            }.awaitAll().filterNotNull().toMap()
        }.mapValues { (id, place) ->
            val recommendation = recommendations.first { it.placeId == id }
            place.copy(category = recommendation.category.toPlaceCategoryLabel().ifBlank { place.category })
        }
    }

    companion object {
        fun factory(
            gateway: AgentGateway,
            placeLookup: (suspend (String) -> TourPlace)? = null,
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    PlaceRecommendationViewModel(gateway, placeLookup) as T
            }
    }
}

private fun AgentRecommendation.toFallbackPlaceItem(): PlaceItem = PlaceItem(
    id = placeId,
    name = name,
    category = category.toPlaceCategoryLabel().ifBlank { "관광명소" },
    rating = 0.0,
    reviews = 0,
    crowdLevel = CrowdLevel.NORMAL,
    emoji = "✨",
    description = "",
    hasRealtimeDetails = false,
)

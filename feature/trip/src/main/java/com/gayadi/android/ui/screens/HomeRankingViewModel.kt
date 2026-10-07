package com.gayadi.android.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.gayadi.android.domain.error.rethrowCancellation
import com.gayadi.android.domain.error.userFacingMessage
import com.gayadi.android.domain.model.RankingList
import com.gayadi.android.domain.model.RankingType
import com.gayadi.android.domain.repository.RankingGateway
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** 홈 카테고리 칩 하나. [region]이 비면 전국 순위다. */
data class HomeRankingCategory(
    val label: String,
    val title: String,
    val type: RankingType,
    val region: String = "",
)

val homeRankingCategories = listOf(
    HomeRankingCategory("인기 관광지", "서울 인기 관광 TOP 10", RankingType.ATTRACTION, "서울"),
    HomeRankingCategory("축제·행사", "축제 TOP 10", RankingType.FESTIVAL),
    HomeRankingCategory("인기 지역", "인기 지역 TOP 10", RankingType.REGION),
    HomeRankingCategory("찜 많은 맛집", "찜 많은 맛집 TOP 10", RankingType.RESTAURANT),
)

data class HomeRankingUiState(
    val selected: HomeRankingCategory = homeRankingCategories.first(),
    val rankings: RankingList? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)

class HomeRankingViewModel(
    private val gateway: RankingGateway,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {
    private val _uiState = MutableStateFlow(HomeRankingUiState())
    val uiState: StateFlow<HomeRankingUiState> = _uiState.asStateFlow()
    private val cache = mutableMapOf<HomeRankingCategory, RankingList>()
    private var job: Job? = null

    init {
        load(homeRankingCategories.first())
    }

    fun select(category: HomeRankingCategory) {
        if (category == _uiState.value.selected && _uiState.value.errorMessage == null) return
        load(category)
    }

    fun retry() = load(_uiState.value.selected, force = true)

    private fun load(category: HomeRankingCategory, force: Boolean = false) {
        val cached = cache[category]?.takeUnless { force }
        job?.cancel()
        if (cached != null) {
            _uiState.value = HomeRankingUiState(selected = category, rankings = cached)
            return
        }
        _uiState.value = HomeRankingUiState(selected = category, isLoading = true)
        job = viewModelScope.launch(ioDispatcher) {
            runCatching { gateway.getRankings(category.type, category.region) }.fold(
                onSuccess = { result ->
                    cache[category] = result
                    _uiState.update {
                        if (it.selected == category) it.copy(rankings = result, isLoading = false) else it
                    }
                },
                onFailure = { error ->
                    error.rethrowCancellation()
                    _uiState.update {
                        if (it.selected != category) it else it.copy(
                            isLoading = false,
                            errorMessage = error.userFacingMessage("순위를 불러오지 못했어요."),
                        )
                    }
                },
            )
        }
    }

    companion object {
        fun factory(gateway: RankingGateway): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    HomeRankingViewModel(gateway) as T
            }
    }
}

package com.gayadi.android.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.gayadi.android.domain.error.rethrowCancellation
import com.gayadi.android.domain.error.userFacingMessage
import com.gayadi.android.domain.model.AgentRecommendation
import com.gayadi.android.domain.repository.AgentGateway
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
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
)

class PlaceRecommendationViewModel(
    private val gateway: AgentGateway,
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
    ) {
        if (_uiState.value.isLoading || _uiState.value.hasRequested && !force) return
        if (latitude == null || longitude == null) {
            _uiState.update {
                it.copy(hasRequested = false, errorMessage = "장소 위치를 불러온 뒤 다시 추천해 주세요.")
            }
            return
        }
        _uiState.update { it.copy(isLoading = true, hasRequested = true, errorMessage = null) }
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
            }.fold(
                onSuccess = { result ->
                    _uiState.update {
                        it.copy(
                            recommendations = result.recommendations,
                            reasoning = result.reasoning,
                            isLoading = false,
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

    companion object {
        fun factory(gateway: AgentGateway): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    PlaceRecommendationViewModel(gateway) as T
            }
    }
}

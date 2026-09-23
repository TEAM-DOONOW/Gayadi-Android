package com.gayadi.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import com.gayadi.android.ui.theme.PrimaryAction
import com.gayadi.android.ui.theme.TextTertiary
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gayadi.android.domain.model.RouteTransportMode
import com.gayadi.android.domain.repository.PlaceSort
import com.gayadi.android.domain.repository.PlaceTravelTime
import com.gayadi.android.ui.components.TransportModeSelector
import com.gayadi.android.ui.theme.TextSecondary

@Composable
internal fun PlaceSearchControls(
    state: PlaceUiState,
    onTransportModeSelected: (RouteTransportMode?) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        TransportModeSelector(state.transportMode, onTransportModeSelected)
        if (!state.isLoading && state.errorMessage == null && state.sort == PlaceSort.TRAVEL_TIME) {
            if (state.rankingSort == PlaceSort.RECENT) {
                Text(state.recentRankingNotice(), style = MaterialTheme.typography.bodySmall, color = TextSecondary)

            }
        }
    }
}

internal fun PlaceTravelTime.displayLabel(): String {
    val mode = when (transportMode) {
        RouteTransportMode.CAR -> "자동차"
        RouteTransportMode.PUBLIC_TRANSIT -> "대중교통"
        RouteTransportMode.WALK -> "도보"
        RouteTransportMode.BICYCLE -> "자전거"
    }
    return "$mode ${if (isEstimate) "추정 " else ""}약 ${durationMinutes}분"
}

internal fun PlaceUiState.recentRankingNotice(): String = when (originAvailable) {
    false -> "이전 장소 위치가 없어 지역 내 최신순으로 표시해요."
    else -> "서버에서 최신순으로 반환했어요. 이동시간 정렬을 다시 시도해 주세요."
}

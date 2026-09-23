package com.gayadi.android.ui.screens

import com.gayadi.android.domain.model.RouteTransportMode
import com.gayadi.android.domain.repository.PlaceSort

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.foundation.layout.navigationBarsPadding
import com.gayadi.android.ui.theme.PrimaryAction
import com.gayadi.android.ui.theme.Background
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import com.gayadi.android.domain.repository.PlaceTravelTime
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.gayadi.android.ui.components.GayadiTopAppBar
import com.gayadi.android.ui.components.ScheduleOptionsBottomSheet
import com.gayadi.android.ui.components.UsageGuideCallout
import com.gayadi.android.ui.components.UsageGuideOverlay
import com.gayadi.android.ui.components.UsageGuidePlacement
import com.gayadi.android.ui.theme.GayadiTheme
import com.gayadi.android.ui.theme.PrimaryBlue
import com.gayadi.android.ui.theme.TagGreen
import com.gayadi.android.ui.theme.TagGreenText
import com.gayadi.android.ui.theme.TagOrange
import com.gayadi.android.ui.theme.TagOrangeText
import com.gayadi.android.ui.theme.TagRed
import com.gayadi.android.ui.theme.TagRedText
import com.gayadi.android.ui.theme.TextPrimary
import com.gayadi.android.ui.theme.TextSecondary
import com.gayadi.android.ui.theme.TextTertiary
import com.gayadi.android.domain.model.AgentRecommendation

private val placeCategories = listOf("전체", "맛집", "카페", "관광명소", "숙소")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaceSearchScreen(
    uiState: PlaceUiState,
    recommendationUiState: PlaceRecommendationUiState = PlaceRecommendationUiState(),
    onBack: () -> Unit,
    onQueryChange: (String) -> Unit,
    onCategorySelected: (String) -> Unit,
    onPlaceClick: (String) -> Unit,
    onRetry: () -> Unit,
    favoritePlaceIds: Set<String> = emptySet(),
    onToggleFavorite: (String) -> Unit = {},
    onNearby: () -> Unit = {},
    onFavorites: () -> Unit = {},
    onRequestRecommendations: () -> Unit = {},
    onRecommendationClick: (AgentRecommendation) -> Unit = {},
    tripName: String = "",
    tripDate: String = "",
    scheduledPlaceIds: Set<String> = emptySet(),
    scheduledPlaceNames: Set<String> = emptySet(),
    onAddToSchedule: (placeId: String, time: String, memo: String) -> Unit = { _, _, _ -> },
    onTransportModeSelected: (RouteTransportMode?) -> Unit = {},
    insertionOptions: List<Pair<String, String>> = emptyList(),
    beforeScheduleId: String? = null,
    onBeforeSelected: (String?) -> Unit = {},
    onLoadMore: () -> Unit = {},
    showUsageGuide: Boolean = false,
    onUsageGuideFinished: () -> Unit = {},
) {
    val density = LocalDensity.current
    val filterDialogVisible = remember { mutableStateOf(false) }
    var schedulePlace by remember { mutableStateOf<PlaceItem?>(null) }
    var isUsageGuideVisible by rememberSaveable { mutableStateOf(showUsageGuide) }
    val firstPlace = uiState.filteredPlaces.firstOrNull()
    var firstPlaceBounds by remember(firstPlace?.id) { mutableStateOf<Rect?>(null) }
    var rootBounds by remember { mutableStateOf<Rect?>(null) }
    val finishGuide = {
        isUsageGuideVisible = false
        onUsageGuideFinished()
    }
    Box(Modifier.fillMaxSize().onGloballyPositioned { rootBounds = it.boundsInRoot() }) {
        Column(Modifier.fillMaxSize().background(Color.White)) {
            GayadiTopAppBar(title = "장소 찾기", onBack = onBack, showDivider = true)

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                BasicTextField(
                    value = uiState.query,
                    onValueChange = onQueryChange,
                    modifier = Modifier.weight(1f).height(40.dp),
                    textStyle = TextStyle(fontSize = 13.sp, color = TextPrimary),
                    singleLine = true,
                    decorationBox = { innerTextField ->
                        Row(
                            modifier = Modifier.fillMaxSize().background(Color(0xFFF5F5F5)).padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Default.Search, contentDescription = "장소 검색", modifier = Modifier.size(20.dp), tint = TextSecondary)
                            Spacer(Modifier.width(8.dp))
                            Box(Modifier.weight(1f)) {
                                if (uiState.query.isBlank()) {
                                    Text("맛집, 카페, 명소 검색", fontSize = 13.sp, color = TextSecondary)
                                }
                                innerTextField()
                            }
                        }
                    },
                )
                IconButton(onClick = { filterDialogVisible.value = true }) {
                    Icon(Icons.Default.Tune, contentDescription = "장소 필터")
                }
            }
            Spacer(Modifier.height(12.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                if (!uiState.isLoading && uiState.errorMessage == null && uiState.sort == PlaceSort.TRAVEL_TIME) {
                    val notice = if (uiState.rankingSort == PlaceSort.RECENT) {
                        uiState.recentRankingNotice()
                    } else null
                    if (notice != null) item {
                        Text(notice, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    }
                }
                if (uiState.sort == PlaceSort.RECENT) {
                item {
                    AgentRecommendationSection(
                        uiState = recommendationUiState,
                        onRetry = onRequestRecommendations,
                        onRecommendationClick = onRecommendationClick,
                        scheduledPlaceIds = scheduledPlaceIds,
                        scheduledPlaceNames = scheduledPlaceNames,
                        onAddToSchedule = { recommendation ->
                            val place = uiState.places.firstOrNull {
                                it.id == recommendation.placeId
                            }
                            if (place != null) schedulePlace = place
                            else onRecommendationClick(recommendation)
                        },
                    )
                }
                }
                when {
                    uiState.isLoading -> item {
                        Box(Modifier.fillMaxWidth().height(240.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = PrimaryBlue)
                        }
                    }
                    uiState.errorMessage != null -> item {
                        Column(
                            Modifier.fillMaxWidth().height(240.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(uiState.errorMessage, color = TextSecondary)
                            Button(onClick = onRetry) { Text("다시 시도") }
                        }
                    }
                    uiState.filteredPlaces.isEmpty() -> item {
                        Box(Modifier.fillMaxWidth().height(240.dp), contentAlignment = Alignment.Center) {
                            Text("조건에 맞는 장소가 없어요", color = TextSecondary)
                        }
                    }
                    else -> {
                        item {
                            Text(
                            if (uiState.rankingSort == PlaceSort.TRAVEL_TIME) "이동시간순 장소" else "${uiState.regionName}의 모든 장소",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            modifier = Modifier.padding(top = 10.dp),
                        )
                    }
                    items(uiState.filteredPlaces.chunked(2)) { rowPlaces ->
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            rowPlaces.forEach { place ->
                                PlaceCard(
                                    place,
                                    modifier = Modifier.weight(1f).onGloballyPositioned {
                                        if (place.id == firstPlace?.id) firstPlaceBounds = it.boundsInRoot()
                                    },
                                    isFavorite = place.id in favoritePlaceIds,
                                    isScheduled = place.id in scheduledPlaceIds || place.name in scheduledPlaceNames,
                                    onClick = { onPlaceClick(place.id) },
                                    onToggleFavorite = { onToggleFavorite(place.id) },
                                    onAddToSchedule = { schedulePlace = place },
                                )
                            }
                            if (rowPlaces.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                        if (uiState.hasNext || uiState.isLoadingMore) item {
                            TextButton(onClick = onLoadMore, enabled = !uiState.isLoadingMore) {
                                Text(if (uiState.isLoadingMore) "불러오는 중…" else "더 보기")
                            }
                        }
                        item { Spacer(Modifier.height(24.dp)) }
                    }
                }
            }
        }

        val target = firstPlaceBounds
        val root = rootBounds
        if (isUsageGuideVisible && !uiState.isLoading && uiState.errorMessage == null &&
            !filterDialogVisible.value && firstPlace != null && target != null && root != null &&
            target.top >= root.top && target.bottom <= root.bottom
        ) {
            UsageGuideOverlay(
                callouts = listOf(
                    UsageGuideCallout(
                        target = target.translate(-root.topLeft),
                        text = AnnotatedString("가고 싶은 장소를 눌러\n상세 정보를 확인해 보세요"),
                        placement = if (root.bottom - target.bottom >= with(density) { 144.dp.toPx() * fontScale }) {
                            UsageGuidePlacement.BELOW
                        } else {
                            UsageGuidePlacement.ABOVE
                        },
                    ),
                ),
                onDismiss = finishGuide,
                onTargetClick = {
                    finishGuide()
                    onPlaceClick(firstPlace.id)
                },
            )
        }
    }

    if (filterDialogVisible.value) {
        ModalBottomSheet(
            onDismissRequest = { filterDialogVisible.value = false },
            containerColor = Background,
            tonalElevation = 0.dp,
        ) {
            Column(
                Modifier.fillMaxWidth().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text("장소 필터", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                PlaceSearchControls(uiState, onTransportModeSelected)
                Text("카테고리", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    placeCategories.forEach { category ->
                        FilterChip(
                            selected = category == uiState.selectedCategory,
                            onClick = { onCategorySelected(category) },
                            label = { Text(category) },
                            leadingIcon = {
                                Icon(
                                    imageVector = when (category) {
                                        "맛집" -> Icons.Default.Restaurant
                                        "카페" -> Icons.Default.LocalCafe
                                        "관광명소" -> Icons.Default.Place
                                        "숙소" -> Icons.Default.Hotel
                                        else -> Icons.Default.Apps
                                    },
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = Background,
                                selectedContainerColor = PrimaryAction,
                                selectedLabelColor = Color.White,
                                selectedLeadingIconColor = Color.White,
                            ),
                        )
                    }
                }
                Button(onClick = { filterDialogVisible.value = false }, modifier = Modifier.fillMaxWidth(), shape = RectangleShape) {
                    Text("장소 보기")
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }

    schedulePlace?.let { place ->
        ScheduleOptionsBottomSheet(
            title = place.name,
            contextText = listOf(tripName, tripDate).filter(String::isNotBlank).joinToString(" · "),
            onDismiss = { schedulePlace = null },
            onConfirm = { time, memo ->
                onAddToSchedule(place.id, time, memo)
                schedulePlace = null
            },
        )
    }
}

@Composable
private fun AgentRecommendationSection(
    uiState: PlaceRecommendationUiState,
    onRetry: () -> Unit,
    onRecommendationClick: (AgentRecommendation) -> Unit,
    scheduledPlaceIds: Set<String>,
    scheduledPlaceNames: Set<String>,
    onAddToSchedule: (AgentRecommendation) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFFF1F6FF))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = PrimaryBlue,
                modifier = Modifier.size(22.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "가야디 에이전트 추천",
                modifier = Modifier.weight(1f),
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
            )
            TextButton(onClick = onRetry, enabled = !uiState.isLoading) {
                Text("다시 추천", color = PrimaryBlue)
            }
        }
        when {
            uiState.isLoading -> Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = PrimaryBlue)
                Text("여행 성향과 현재 상황을 분석하고 있어요.", fontSize = 12.sp, color = TextSecondary)
            }
            uiState.errorMessage != null -> {
                Text(uiState.errorMessage, fontSize = 12.sp, color = TextSecondary)
                TextButton(onClick = onRetry) { Text("추천 다시 받기", color = PrimaryBlue) }
            }
            uiState.recommendations.isEmpty() -> {
                Text("맞춤 추천을 받아 여행지 후보를 확인해 보세요.", fontSize = 12.sp, color = TextSecondary)
            }
            else -> {
                uiState.reasoning.takeIf(String::isNotBlank)?.let {
                    Text(it, fontSize = 12.sp, color = TextSecondary)
                }
                uiState.recommendations.forEach { recommendation ->
                    val isScheduled = recommendation.placeId in scheduledPlaceIds ||
                        recommendation.name in scheduledPlaceNames
                    Column(
                        modifier = Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.White)
                            .clickable { onRecommendationClick(recommendation) }
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                recommendation.name,
                                modifier = Modifier.weight(1f),
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary,
                            )
                            Text(
                                "추천 ${(recommendation.score * 100).toInt().coerceIn(0, 100)}%",
                                fontSize = 11.sp,
                                color = PrimaryBlue,
                            )
                        }
                        Text(recommendation.reason, fontSize = 12.sp, color = TextSecondary)
                        TextButton(
                            onClick = { onAddToSchedule(recommendation) },
                            enabled = !isScheduled,
                            modifier = Modifier.align(Alignment.End),
                            shape = RectangleShape,
                        ) {
                            Text(if (isScheduled) "일정에 추가됨" else "일정 추가", color = PrimaryBlue)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaceCard(
    place: PlaceItem,
    modifier: Modifier = Modifier,
    isFavorite: Boolean,
    isScheduled: Boolean,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onAddToSchedule: () -> Unit,
) {
    val (tagBackground, tagText) = when (place.crowdLevel) {
        CrowdLevel.RELAXED -> TagGreen to TagGreenText
        CrowdLevel.NORMAL -> TagOrange to TagOrangeText
        CrowdLevel.CROWDED -> TagRed to TagRedText
    }
    Column(modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Box(Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(18.dp)).background(Color(0xFFF0F0F0))) {
            if (place.imageUrl.isNotBlank()) {
                AsyncImage(
                    model = place.imageUrl,
                    contentDescription = "${place.name} 이미지",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Text(place.emoji, fontSize = 28.sp)
            }
            IconButton(onClick = onToggleFavorite, modifier = Modifier.align(Alignment.TopEnd)) {
                Icon(
                    if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = if (isFavorite) "찜 해제" else "찜 추가",
                    tint = if (isFavorite) Color(0xFFE84D6E) else Color.White,
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(place.name, modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold,
                color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            place.travelTime?.let { TravelTimeBadge(it) }
        }
        place.travelTime?.let { time ->
            time.additionalDurationMinutes?.let { additional ->
                Text("추가 이동시간 ${additional}분", color = TextSecondary, style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
            }
        }
            Text(
                if (place.reviews > 0) "${place.category} · ★ ${place.rating} · 리뷰 ${place.reviews}" else place.category,
                fontSize = 12.sp,
                color = TextSecondary,
            )
            Text(place.description, fontSize = 11.sp, color = TextTertiary, maxLines = 1)
        if (place.hasRealtimeDetails) {
            Text(
                place.crowdLevel.label,
                modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(tagBackground)
                    .padding(horizontal = 8.dp, vertical = 3.dp),
                fontSize = 10.sp,
                color = tagText,
            )
        }
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = onAddToSchedule,
            enabled = !isScheduled,
            modifier = Modifier.fillMaxWidth().height(36.dp),
            shape = RectangleShape,
        ) {
            Text(if (isScheduled) "추가됨" else "일정 추가", fontSize = 12.sp)
        }
    }
}

@Composable
private fun TravelTimeBadge(time: PlaceTravelTime) {
    var showDetails by remember(time) { mutableStateOf(false) }
    val icon = when (time.transportMode) {
        RouteTransportMode.CAR -> Icons.Default.DirectionsCar
        RouteTransportMode.PUBLIC_TRANSIT -> Icons.Default.DirectionsBus
        RouteTransportMode.WALK -> Icons.Default.DirectionsWalk
        RouteTransportMode.BICYCLE -> Icons.Default.DirectionsBike
    }
    Row(
        Modifier.clickable(role = Role.Button, onClickLabel = "이동시간 안내", onClick = { showDetails = true })
            .semantics(mergeDescendants = true) { contentDescription = time.displayLabel() }
            .padding(vertical = 8.dp, horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = TextSecondary)
        Text("${time.durationMinutes}분", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
    }
    if (showDetails) {
        AlertDialog(
            onDismissRequest = { showDetails = false },
            containerColor = Background,
            title = { Text(time.displayLabel()) },
            text = {
                Text(when {
                    time.transportMode == RouteTransportMode.WALK || time.transportMode == RouteTransportMode.BICYCLE ->
                        "직선거리를 기준으로 계산한 추정 시간이에요. 실제 경로에 따라 달라질 수 있어요."
                    time.isEstimate -> "실제 교통 조회 결과가 아닌 추정 시간이에요."
                    else -> "경로 조회로 계산한 예상 시간이에요. 실제 이동 상황에 따라 달라질 수 있어요."
                })
            },
            confirmButton = { TextButton(onClick = { showDetails = false }) { Text("확인") } },
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PlaceSearchPreview() {
    GayadiTheme {
        PlaceSearchScreen(
            uiState = PlaceUiState(places = FakePlaceRepository().places().getOrThrow(), isLoading = false),
            onBack = {},
            onQueryChange = {},
            onCategorySelected = {},
            onPlaceClick = {},
            onRetry = {},
        )
    }
}

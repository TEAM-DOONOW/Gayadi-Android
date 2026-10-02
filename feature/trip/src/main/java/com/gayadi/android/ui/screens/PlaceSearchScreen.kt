package com.gayadi.android.ui.screens

import com.gayadi.android.domain.model.RouteTransportMode
import com.gayadi.android.domain.model.AgentRecommendation
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
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.ui.graphics.graphicsLayer
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.foundation.border
import androidx.compose.foundation.selection.toggleable
import com.gayadi.android.ui.theme.Divider
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gayadi.android.ui.components.PlacePhoto
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
    onFavorites: () -> Unit = {},
    onRequestRecommendations: () -> Unit = {},
    onRecommendationClick: (AgentRecommendation) -> Unit = {},
    replaceTargetName: String? = null,
    onReplaceWithPlace: (PlaceItem) -> Unit = {},
    linkMode: Boolean = true,
    onLinkModeChange: (Boolean) -> Unit = {},
    replaceTargetOptions: List<Pair<String, String>> = emptyList(),
    replaceTargetId: String? = null,
    onReplaceTargetSelected: (String?) -> Unit = {},
    isLinkedReplacement: (PlaceItem) -> Boolean = { true },
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
    var replacementPlace by remember { mutableStateOf<PlaceItem?>(null) }
    val recommendedPlaces = recommendationUiState.recommendedPlaces
    val recommendedIds = recommendedPlaces.map { it.place.id }.toSet()
    val listedPlaces = uiState.filteredPlaces.filterNot { it.id in recommendedIds }
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
                item {
                    LinkModeControls(
                        linkMode = linkMode,
                        onLinkModeChange = onLinkModeChange,
                        targetOptions = replaceTargetOptions,
                        selectedTargetId = replaceTargetId,
                        onTargetSelected = onReplaceTargetSelected,
                    )
                }
                item {
                    RecommendationHeader(recommendationUiState, onRetry = onRequestRecommendations)
                }
                items(recommendedPlaces.chunked(2), key = { row -> "rec-" + row.joinToString { it.place.id } }) { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        row.forEach { recommended ->
                            val place = recommended.place
                            PlaceCard(
                                place,
                                modifier = Modifier.weight(1f),
                                isFavorite = place.id in favoritePlaceIds,
                                isScheduled = place.id in scheduledPlaceIds || place.name in scheduledPlaceNames,
                                onClick = { onRecommendationClick(recommended.recommendation) },
                                onToggleFavorite = { onToggleFavorite(place.id) },
                                onAddToSchedule = { schedulePlace = place },
                                recommendationReason = recommended.recommendation.reason,
                                addLabel = "일정에 추가",
                                replaceEnabled = replaceTargetName != null,
                                onReplace = { replacementPlace = place },
                            )
                        }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
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
                    listedPlaces.isEmpty() -> item {
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
                    items(listedPlaces.chunked(2)) { rowPlaces ->
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
                                    replaceEnabled = replaceTargetName != null,
                                    onReplace = if (replaceTargetName != null) ({ replacementPlace = place }) else null,
                                )
                            }
                            if (rowPlaces.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                        if (uiState.hasNext || uiState.isLoadingMore) item {
                            TextButton(
                                onClick = onLoadMore,
                                enabled = !uiState.isLoadingMore,
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                            ) {
                                if (uiState.isLoadingMore) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp).semantics { contentDescription = "다음 장소 불러오는 중" },
                                        strokeWidth = 2.dp,
                                        color = PrimaryAction,
                                    )
                                } else {
                                    Icon(
                                        Icons.Default.KeyboardArrowDown,
                                        contentDescription = "다음 장소 보기",
                                        modifier = Modifier.size(32.dp).graphicsLayer(scaleX = 2f),
                                        tint = PrimaryAction,
                                    )
                                }
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

    replacementPlace?.let { place ->
        val unlinked = linkMode && !isLinkedReplacement(place)
        AlertDialog(
            onDismissRequest = { replacementPlace = null },
            containerColor = Background,
            title = { Text(if (unlinked) "기존 여행지와 연계되지 않아요" else "이 장소로 변경할까요?") },
            text = {
                Text(
                    if (unlinked) {
                        "${place.name}은(는) 앞뒤 일정과 떨어져 있어 동선이 길어질 수 있어요. 그래도 고르시겠어요?"
                    } else {
                        "${replaceTargetName.orEmpty()} 일정을 ${place.name}(으)로 바꿔요. 시간과 메모는 그대로 유지돼요."
                    },
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onReplaceWithPlace(place)
                    replacementPlace = null
                }) { Text(if (unlinked) "그래도 변경" else "변경") }
            },
            dismissButton = { TextButton(onClick = { replacementPlace = null }) { Text("취소") } },
        )
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
private fun LinkModeControls(
    linkMode: Boolean,
    onLinkModeChange: (Boolean) -> Unit,
    targetOptions: List<Pair<String, String>>,
    selectedTargetId: String?,
    onTargetSelected: (String?) -> Unit,
) {
    Column(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, Divider, RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            Modifier.fillMaxWidth().toggleable(value = linkMode, role = Role.Switch, onValueChange = onLinkModeChange),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("여행지 연계", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                Text(
                    if (linkMode) "앞뒤 일정과 이어지는 장소를 추천해요" else "동선과 상관없이 자유롭게 골라요",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )
            }
            Switch(
                checked = linkMode,
                onCheckedChange = null,
                colors = SwitchDefaults.colors(checkedTrackColor = PrimaryAction),
            )
        }
        if (targetOptions.isNotEmpty()) {
            Text("바꿀 일정", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                targetOptions.forEachIndexed { index, (id, title) ->
                    val selected = id == selectedTargetId
                    FilterChip(
                        selected = selected,
                        onClick = { onTargetSelected(if (selected) null else id) },
                        label = { Text("${index + 1}. $title", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = Background,
                            selectedContainerColor = PrimaryAction,
                            selectedLabelColor = Color.White,
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun RecommendationHeader(uiState: PlaceRecommendationUiState, onRetry: () -> Unit) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                uiState.headline.ifBlank { "가야디 추천 장소" },
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            TextButton(onClick = onRetry, enabled = !uiState.isLoading) {
                Text(if (uiState.hasRequested) "다시 추천" else "추천 받기", color = PrimaryBlue)
            }
        }
        when {
            uiState.isLoading -> Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = PrimaryBlue)
                Text("여행 성향과 동선을 보고 추천하고 있어요", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
            uiState.errorMessage != null ->
                Text(uiState.errorMessage, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            uiState.hasRequested && uiState.recommendedPlaces.isEmpty() ->
                Text("지금은 추천할 장소를 찾지 못했어요", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            !uiState.hasRequested ->
                Text("장소를 일정에 추가하면 그 장소 기준으로 다음 장소를 추천해 드려요", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            else -> uiState.reasoning.takeIf(String::isNotBlank)?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = TextSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
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
    recommendationReason: String? = null,
    addLabel: String = "일정 추가",
    replaceEnabled: Boolean = false,
    onReplace: (() -> Unit)? = null,
) {
    val (tagBackground, tagText) = when (place.crowdLevel) {
        CrowdLevel.RELAXED -> TagGreen to TagGreenText
        CrowdLevel.NORMAL -> TagOrange to TagOrangeText
        CrowdLevel.CROWDED -> TagRed to TagRedText
        CrowdLevel.UNKNOWN -> com.gayadi.android.ui.theme.SurfaceCard to TextSecondary
    }
    Column(modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Box(Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(18.dp)).background(Color(0xFFF0F0F0))) {
            PlacePhoto(place.imageUrl, "${place.name} 이미지", Modifier.fillMaxSize())
            if (recommendationReason != null) {
                Row(
                    Modifier.align(Alignment.TopStart).padding(8.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(PrimaryAction)
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                    Text("추천", style = MaterialTheme.typography.labelSmall, color = Color.White)
                }
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
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                if (place.reviews > 0) "${place.category} · ★ ${place.rating} · 리뷰 ${place.reviews}" else place.category,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontSize = 12.sp,
                color = TextSecondary,
            )
            if (place.crowdLevel != CrowdLevel.UNKNOWN) {
                Text(
                    place.crowdLevel.label,
                    modifier = Modifier.widthIn(min = 44.dp)
                        .clip(RoundedCornerShape(percent = 50)).background(tagBackground)
                        .padding(horizontal = 12.dp, vertical = 2.dp)
                        .semantics {
                            contentDescription = "예상 ${place.crowdLevel.label}" +
                                if (place.crowdEstimated && !place.crowdProviderDataAvailable) ", 달력 기반 추정" else ""
                        },
                    maxLines = 1,
                    softWrap = false,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    fontSize = 10.sp,
                    lineHeight = 14.sp,
                    color = tagText,
                )
            }
        }
        recommendationReason?.takeIf(String::isNotBlank)?.let { reason ->
            Text(
                reason,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(place.description, fontSize = 11.sp, color = TextTertiary, maxLines = 1)
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = onAddToSchedule,
            enabled = !isScheduled,
            modifier = Modifier.fillMaxWidth().height(36.dp),
            shape = RectangleShape,
        ) {
            Text(if (isScheduled) "추가됨" else addLabel, fontSize = 12.sp)
        }
        if (onReplace != null) {
            Spacer(Modifier.height(6.dp))
            OutlinedButton(
                onClick = onReplace,
                enabled = replaceEnabled && !isScheduled,
                modifier = Modifier.fillMaxWidth().height(36.dp),
                shape = RectangleShape,
                contentPadding = PaddingValues(horizontal = 4.dp),
            ) {
                Text("이 장소로 변경", fontSize = 12.sp, color = if (replaceEnabled && !isScheduled) PrimaryAction else TextTertiary)
            }
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

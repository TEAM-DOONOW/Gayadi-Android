package com.gayadi.android.ui.screens

import com.gayadi.android.ui.theme.Divider
import androidx.compose.foundation.BorderStroke
import com.gayadi.android.ui.theme.TextTertiary
import com.gayadi.android.ui.theme.SurfaceLight
import androidx.compose.material.icons.outlined.Place
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import coil.compose.AsyncImage
import com.gayadi.android.domain.model.RankingItem
import androidx.compose.foundation.Canvas
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.outlined.Luggage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gayadi.android.ui.components.gayadiPatternBackground
import com.gayadi.android.ui.components.UsageGuideCallout
import com.gayadi.android.ui.components.UsageGuideOverlay
import com.gayadi.android.ui.components.UsageGuidePlacement
import com.gayadi.android.ui.theme.GayadiTheme
import com.gayadi.android.ui.theme.PretendardFontFamily
import com.gayadi.android.ui.theme.PretendardSemiBoldFontFamily
import com.gayadi.android.ui.theme.PrimaryBlue
import com.gayadi.android.ui.theme.TextPrimary
import com.gayadi.android.ui.theme.TextSecondary
import com.gayadi.android.domain.model.TripStatus
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

private val TripAccentColor = Color(0xFF343548)
private val tripSummaryDateFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd")

data class TripSummary(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val startDate: String,
    val endDate: String,
    val cities: List<String>,
    val coverImageResList: List<Int>,
    val status: TripStatus = TripStatus.PLANNING,
    val participantIds: List<String> = emptyList(),
    val inviteCode: String = "",
    val isGroupTrip: Boolean = false,
    val dateAvailability: Map<String, List<String>> = emptyMap(),
    val ownerId: String = "",
)

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun MyTripScreen(
    trips: List<TripSummary>,
    showUsageGuide: Boolean = false,
    onUsageGuideFinished: () -> Unit = {},
    onAddTrip: () -> Unit,
    onJoinTrip: () -> Unit,
    onOpenTripDetail: (String) -> Unit,
    onDeleteTrip: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAgent: () -> Unit = {},
    onJoinTripWithCode: (String) -> Unit = { onJoinTrip() },
    rankingUiState: HomeRankingUiState = HomeRankingUiState(),
    onRankingCategorySelected: (HomeRankingCategory) -> Unit = {},
    onRankingRetry: () -> Unit = {},
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var showJoinTripSheet by rememberSaveable { mutableStateOf(false) }
    var isUsageGuideVisible by rememberSaveable { mutableStateOf(showUsageGuide) }
    var inviteButtonBounds by remember { mutableStateOf<Rect?>(null) }
    var addButtonBounds by remember { mutableStateOf<Rect?>(null) }
    val pageScrollState = rememberScrollState()
    val today = LocalDate.now()
    val ongoingTrips = trips.filter { trip ->
        trip.status != TripStatus.COMPLETED && trip.endDate.toTripDate()?.isBefore(today) != true
    }
    val completedTrips = trips.filter { trip ->
        trip.status == TripStatus.COMPLETED || trip.endDate.toTripDate()?.isBefore(today) == true
    }
    val visibleTrips = if (selectedTab == 0) ongoingTrips else completedTrips

    Box(Modifier.fillMaxSize().background(Color.White)) {
        Box(Modifier.fillMaxWidth().height(410.dp).gayadiPatternBackground())
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(pageScrollState)
                .padding(bottom = 16.dp),
        ) {
        Spacer(modifier = Modifier.height(48.dp))
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "국내여행",
                fontFamily = PretendardSemiBoldFontFamily,
                fontSize = 22.sp,
                color = TextPrimary,
            )
            Row {
                IconButton(onClick = onOpenAgent) {
                    Icon(
                        Icons.Filled.AutoAwesome,
                        contentDescription = "가야디 에이전트",
                        tint = TripAccentColor,
                    )
                }
                IconButton(
                    onClick = { showJoinTripSheet = true },
                ) {
                    Icon(
                        imageVector = Icons.Filled.PersonAdd,
                        contentDescription = "초대 코드로 여행 참여",
                        tint = TripAccentColor,
                        modifier = Modifier.onGloballyPositioned { inviteButtonBounds = it.boundsInRoot() },
                    )
                }
                IconButton(onClick = onOpenSettings) {
                    Icon(Icons.Filled.Settings, contentDescription = "설정", tint = TripAccentColor)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "이번 주말, 어디로 갈까요?",
            modifier = Modifier.padding(horizontal = 20.dp),
            fontFamily = PretendardSemiBoldFontFamily,
            fontSize = 22.sp,
            color = TripAccentColor,
        )
        Spacer(modifier = Modifier.height(14.dp))
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            homeRankingCategories.forEach { category ->
                val selected = category == rankingUiState.selected
                Text(
                    category.label,
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { onRankingCategorySelected(category) }
                        .semantics { this.selected = selected }
                        .background(if (selected) TripAccentColor else Color.White)
                        .padding(horizontal = 20.dp, vertical = 6.dp),
                    fontFamily = PretendardFontFamily,
                    fontSize = 14.sp,
                    color = if (selected) Color.White else TextSecondary,
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        HomeRankingSection(rankingUiState, onRetry = onRankingRetry)
        Spacer(modifier = Modifier.height(28.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFF5F5F7)),
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(topStart = 36.dp, topEnd = 36.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            ) {
                Column(Modifier.fillMaxWidth().padding(30.dp)) {
                Text(
                    text = "나의 여행",
                    fontFamily = PretendardSemiBoldFontFamily,
                    fontSize = 20.sp,
                    color = TripAccentColor,
                )
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .background(Color(0xFFF0F0F2), RoundedCornerShape(20.dp))
                        .padding(4.dp),
                ) {
                    TripTab(
                        text = "진행 중인 여행",
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        modifier = Modifier.weight(1f),
                    )
                    TripTab(
                        text = "완료된 여행",
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        modifier = Modifier.weight(1f),
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))

                Box(modifier = Modifier.fillMaxWidth().heightIn(min = 170.dp)) {
                    if (visibleTrips.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            visibleTrips.forEach { trip ->
                                TripCard(
                                    trip = trip,
                                    onClick = { onOpenTripDetail(trip.id) },
                                    onDelete = { onDeleteTrip(trip.id) },
                                )
                            }
                        }
                    } else if (selectedTab == 0) {
                        EmptyTrips(modifier = Modifier.align(Alignment.Center))
                    } else {
                        EmptyTrips(
                            title = "아직 완료된 여행이 없어요",
                            message = "여행이 끝나면 이곳에 자동으로 모아둘게요.",
                            modifier = Modifier.align(Alignment.Center),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))
                Button(
                    onClick = onAddTrip,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                        .onGloballyPositioned { addButtonBounds = it.boundsInRoot() },
                    shape = RoundedCornerShape(2.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TripAccentColor),
                ) {
                    Text("여행 추가하기", fontFamily = PretendardSemiBoldFontFamily, fontSize = 15.sp)
                }
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        }

        if (isUsageGuideVisible) {
            LaunchedEffect(pageScrollState.maxValue) {
                pageScrollState.animateScrollTo(pageScrollState.maxValue)
            }
            val inviteTarget = inviteButtonBounds
            val addTarget = addButtonBounds
            if (inviteTarget != null && addTarget != null) {
                val finishGuide = {
                    isUsageGuideVisible = false
                    onUsageGuideFinished()
                }
                UsageGuideOverlay(
                    callouts = listOf(
                        UsageGuideCallout(
                            target = inviteTarget,
                            text = buildAnnotatedString {
                                append("초대 코드를 받았다면\n")
                                withStyle(SpanStyle(color = PrimaryBlue, fontWeight = FontWeight.SemiBold)) {
                                    append("사람 추가 버튼")
                                }
                                append("을 누르세요")
                            },
                            placement = UsageGuidePlacement.BELOW,
                            spotlightPadding = 6.dp,
                            spotlightRadius = 12.dp,
                        ),
                        UsageGuideCallout(
                            target = addTarget,
                            text = buildAnnotatedString {
                                withStyle(SpanStyle(color = PrimaryBlue, fontWeight = FontWeight.SemiBold)) {
                                    append("여행 추가하기")
                                }
                                append("를 눌러 새 여행을 만드세요")
                            },
                            placement = UsageGuidePlacement.ABOVE,
                            spotlightPadding = 10.dp,
                            spotlightRadius = 18.dp,
                        ),
                    ),
                    onDismiss = finishGuide,
                    onTargetClick = { targetIndex ->
                        finishGuide()
                        if (targetIndex == 0) showJoinTripSheet = true else onAddTrip()
                    },
                )
            }
        }
    }

    if (showJoinTripSheet) {
        JoinTripBottomSheet(
            onDismiss = { showJoinTripSheet = false },
            onSubmit = { code ->
                showJoinTripSheet = false
                onJoinTripWithCode(code)
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun JoinTripBottomSheet(onDismiss: () -> Unit, onSubmit: (String) -> Unit) {
    var inviteCode by rememberSaveable { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(start = 24.dp, top = 8.dp, end = 24.dp, bottom = 32.dp),
        ) {
            Text("초대코드로 여행 참여", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Spacer(Modifier.height(8.dp))
            Text("여행을 만든 사람에게 받은 코드를 입력해 주세요", fontSize = 13.sp, color = TextSecondary)
            Spacer(Modifier.height(20.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp)
                    .border(1.dp, Color(0xFFD2D3D8), RectangleShape)
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                BasicTextField(
                    value = inviteCode,
                    onValueChange = { inviteCode = it.filter(Char::isLetterOrDigit).take(6).uppercase() },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    textStyle = TextStyle(fontSize = 14.sp, color = TextPrimary),
                    cursorBrush = SolidColor(TripAccentColor),
                    decorationBox = { innerTextField ->
                        if (inviteCode.isEmpty()) {
                            Text("여행 초대코드", fontSize = 14.sp, color = TextSecondary)
                        }
                        innerTextField()
                    },
                )
            }
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { onSubmit(inviteCode) },
                enabled = inviteCode.length == 6,
                modifier = Modifier.fillMaxWidth().height(40.dp),
                shape = RectangleShape,
                colors = ButtonDefaults.buttonColors(containerColor = TripAccentColor),
            ) { Text("참여하기", color = Color.White) }
        }
    }
}

@Composable
private fun HomeRankingSection(uiState: HomeRankingUiState, onRetry: () -> Unit) {
    val rankings = uiState.rankings
    Column(Modifier.fillMaxWidth()) {
        Text(
            uiState.selected.title,
            modifier = Modifier.padding(horizontal = 20.dp),
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary,
        )
        Spacer(Modifier.height(10.dp))
        when {
            uiState.isLoading -> Box(
                Modifier.fillMaxWidth().height(HomeRankingCardHeight),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = TripAccentColor, modifier = Modifier.size(28.dp), strokeWidth = 2.dp)
            }
            uiState.errorMessage != null -> HomeRankingMessage(uiState.errorMessage, actionLabel = "다시 시도", onAction = onRetry)
            rankings == null || rankings.items.isEmpty() -> HomeRankingMessage("아직 순위 정보가 없어요. 잠시 후 다시 확인해 주세요.")
            else -> {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(rankings.items, key = { "${it.rank}-${it.title}" }) { item ->
                        HomeRankingCard(item)
                    }
                }
                if (!rankings.providerDataAvailable) {
                    Text(
                        "순위 자료를 준비 중이라 추천 관광지를 먼저 보여드려요",
                        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeRankingMessage(message: String, actionLabel: String? = null, onAction: () -> Unit = {}) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(message, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        if (actionLabel != null) {
            TextButton(onClick = onAction, contentPadding = PaddingValues(0.dp)) {
                Text(actionLabel, color = TripAccentColor)
            }
        }
    }
}

private val HomeRankingCardHeight = 214.dp
@Composable
private fun HomeRankingCard(item: RankingItem) {
    Card(
        modifier = Modifier.width(160.dp).semantics(mergeDescendants = true) {},
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        // 패턴 배경 아래 흰 영역까지 카드가 걸쳐도 경계가 보이도록 테두리를 둔다.
        border = BorderStroke(1.dp, Divider),
    ) {
        Column(Modifier.fillMaxWidth().padding(10.dp)) {
            Box(
                Modifier.fillMaxWidth().height(128.dp).clip(RoundedCornerShape(12.dp)).background(SurfaceLight),
                contentAlignment = Alignment.Center,
            ) {
                // 이미지가 없거나 불러오지 못하면 다른 장소 사진 대신 중립적인 장소 아이콘을 보여준다.
                Icon(Icons.Outlined.Place, contentDescription = null, tint = TextTertiary, modifier = Modifier.size(32.dp))
                if (item.imageUrl.isNotBlank()) {
                    AsyncImage(
                        model = item.imageUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                }
                Text(
                    "${item.rank}",
                    modifier = Modifier.align(Alignment.TopStart).padding(8.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(TripAccentColor)
                        .padding(horizontal = 10.dp, vertical = 3.dp)
                        .semantics { contentDescription = "${item.rank}위" },
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White,
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                item.title,
                style = MaterialTheme.typography.titleSmall,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                item.subtitle.ifBlank { item.metricLabel },
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (item.subtitle.isNotBlank() && item.metricLabel.isNotBlank()) {
                Text(
                    item.metricLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = TripAccentColor,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun TripTab(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                color = if (selected) TripAccentColor else Color.Transparent,
                shape = RoundedCornerShape(16.dp),
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            fontFamily = if (selected) PretendardSemiBoldFontFamily else PretendardFontFamily,
            fontSize = 13.sp,
            color = if (selected) Color.White else Color(0xFF777983),
        )
    }
}

@Composable
private fun EmptyTrips(
    title: String = "아직 만든 여행이 없어요",
    message: String = "여행 추가하기를 눌러 첫 여행을 만들어 보세요.",
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.padding(bottom = 36.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = Icons.Outlined.Luggage,
            contentDescription = null,
            modifier = Modifier.size(52.dp),
            tint = Color(0xFFB7B8C0),
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(title, fontFamily = PretendardSemiBoldFontFamily, fontSize = 17.sp, color = TextPrimary)
        Spacer(modifier = Modifier.height(6.dp))
        Text(message, fontFamily = PretendardFontFamily, fontSize = 13.sp, color = TextSecondary)
    }
}

@Composable
private fun TripCard(trip: TripSummary, onClick: () -> Unit, onDelete: () -> Unit) {
    var showDelete by remember(trip.id) { mutableStateOf(false) }
    var confirmDelete by remember(trip.id) { mutableStateOf(false) }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CityImageGrid(
                imageResources = cityCoverImageResources(trip.cities).ifEmpty { trip.coverImageResList },
                modifier = Modifier.size(64.dp),
            )
            Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        trip.name,
                        fontFamily = PretendardSemiBoldFontFamily,
                        fontSize = 17.sp,
                        color = TextPrimary,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(
                        onClick = { if (showDelete) confirmDelete = true else showDelete = true },
                        modifier = Modifier.size(36.dp),
                    ) {
                        Icon(
                            imageVector = if (showDelete) Icons.Default.Delete else Icons.Default.MoreHoriz,
                            contentDescription = if (showDelete) "여행 삭제" else "여행 메뉴",
                            tint = if (showDelete) Color(0xFFE45858) else Color(0xFF777883),
                        )
                    }
                }
                Spacer(modifier = Modifier.height(7.dp))
                Text(
                    trip.cities.joinToString(" · "),
                    fontFamily = PretendardSemiBoldFontFamily,
                    fontSize = 14.sp,
                    color = TripAccentColor,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    if (trip.isGroupTrip && trip.startDate.isBlank()) "가능한 날짜 정하기" else "${trip.startDate} - ${trip.endDate}",
                    fontFamily = PretendardFontFamily,
                    fontSize = 13.sp,
                    color = if (trip.isGroupTrip && trip.startDate.isBlank()) TripAccentColor else Color(0xFF9A9BA2),
                )
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = {
                confirmDelete = false
                showDelete = false
            },
            title = { Text("여행을 삭제할까요?") },
            text = { Text("일정과 연결된 모든 비용, 초대 정보도 함께 삭제되며 되돌릴 수 없어요.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    showDelete = false
                    onDelete()
                }) { Text("여행 삭제", color = Color(0xFFE45858)) }
            },
            dismissButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    showDelete = false
                }) { Text("취소") }
            },
        )
    }
}

@Composable
private fun CityImageGrid(imageResources: List<Int>, modifier: Modifier = Modifier) {
    val visibleResources = imageResources.take(4)
    val resources = LocalContext.current.resources
    val images = remember(visibleResources) {
        visibleResources.map { resourceId -> ImageBitmap.imageResource(resources, resourceId) }
    }

    Canvas(modifier = modifier) {
        if (images.isEmpty()) return@Canvas
        val destinationSize = IntSize(size.width.toInt(), size.height.toInt())
        val circle = Path().apply {
            addOval(Rect(0f, 0f, size.width, size.height))
        }

        clipPath(circle) {
            images.forEachIndexed { index, image ->
                val cropSize = minOf(image.width, image.height)
                val sourceOffset = IntOffset(
                    x = (image.width - cropSize) / 2,
                    y = (image.height - cropSize) / 2,
                )
                val cell = when {
                    images.size == 1 -> Rect(0f, 0f, size.width, size.height)
                    images.size == 2 && index == 0 -> Rect(0f, 0f, size.width / 2f, size.height)
                    images.size == 2 -> Rect(size.width / 2f, 0f, size.width, size.height)
                    images.size == 3 && index == 0 -> Rect(0f, 0f, size.width, size.height / 2f)
                    images.size == 3 && index == 1 ->
                        Rect(0f, size.height / 2f, size.width / 2f, size.height)
                    images.size == 3 ->
                        Rect(size.width / 2f, size.height / 2f, size.width, size.height)
                    else -> {
                        val left = if (index % 2 == 0) 0f else size.width / 2f
                        val top = if (index < 2) 0f else size.height / 2f
                        Rect(left, top, left + size.width / 2f, top + size.height / 2f)
                    }
                }

                clipRect(cell.left, cell.top, cell.right, cell.bottom) {
                    drawImage(
                        image = image,
                        srcOffset = sourceOffset,
                        srcSize = IntSize(cropSize, cropSize),
                        dstOffset = IntOffset.Zero,
                        dstSize = destinationSize,
                    )
                }
            }

            if (images.size >= 2) {
                drawLine(
                    color = Color.White,
                    start = androidx.compose.ui.geometry.Offset(
                        size.width / 2f,
                        if (images.size == 3) size.height / 2f else 0f,
                    ),
                    end = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height),
                    strokeWidth = 3.dp.toPx(),
                )
            }
            if (images.size >= 3) {
                drawLine(
                    color = Color.White,
                    start = androidx.compose.ui.geometry.Offset(0f, size.height / 2f),
                    end = androidx.compose.ui.geometry.Offset(size.width, size.height / 2f),
                    strokeWidth = 3.dp.toPx(),
                )
            }
        }
        drawCircle(color = Color.White, style = Stroke(width = 2.dp.toPx()))
    }
}

private fun String.toTripDate(): LocalDate? =
    runCatching { LocalDate.parse(this, tripSummaryDateFormatter) }.getOrNull()

@Preview(showBackground = true, heightDp = 800)
@Composable
private fun MyTripPreview() {
    GayadiTheme {
        MyTripScreen(
            trips = emptyList(),
            onAddTrip = {},
            onJoinTrip = {},
            onOpenTripDetail = {},
            onDeleteTrip = {},
            onOpenSettings = {},
        )
    }
}

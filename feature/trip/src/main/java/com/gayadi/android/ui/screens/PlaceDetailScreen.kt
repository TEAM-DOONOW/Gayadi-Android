package com.gayadi.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Umbrella
import androidx.compose.material.icons.outlined.AcUnit
import androidx.compose.material.icons.outlined.Thermostat
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gayadi.android.ui.components.PlacePhoto
import com.gayadi.android.ui.components.GayadiTopAppBar
import com.gayadi.android.ui.components.ScheduleOptionsBottomSheet
import com.gayadi.android.ui.components.UsageGuideCallout
import com.gayadi.android.ui.components.UsageGuideOverlay
import com.gayadi.android.ui.components.UsageGuidePlacement
import com.gayadi.android.ui.theme.*

@Composable
fun PlaceDetailScreen(
    place: PlaceItem?,
    tripName: String = "",
    tripDate: String = "",
    isScheduled: Boolean,
    onBack: () -> Unit,
    onAddToSchedule: (time: String, memo: String) -> Unit,
    isFavorite: Boolean = false,
    onToggleFavorite: () -> Unit = {},
    showUsageGuide: Boolean = false,
    onUsageGuideFinished: () -> Unit = {},
    hourlyUiState: CongestionHourlyUiState = CongestionHourlyUiState(),
    onHourlyRetry: () -> Unit = {},
    forecastUiState: CongestionForecastUiState = CongestionForecastUiState(),
    onForecastRetry: () -> Unit = {},
    weatherUiState: PlaceWeatherUiState = PlaceWeatherUiState(),
    onWeatherRetry: () -> Unit = {},
) {
    var showScheduleOptions by rememberSaveable(place?.id) { mutableStateOf(false) }
    var isUsageGuideVisible by rememberSaveable(place?.id) { mutableStateOf(showUsageGuide) }
    var addButtonBounds by remember(place?.id) { mutableStateOf<Rect?>(null) }
    var rootBounds by remember { mutableStateOf<Rect?>(null) }
    val finishGuide = {
        isUsageGuideVisible = false
        onUsageGuideFinished()
    }
    val openScheduleOptions = {
        if (isUsageGuideVisible) finishGuide()
        showScheduleOptions = true
    }
    val forecast = forecastUiState.forecast
    val level = forecast?.level?.toCrowdLevel() ?: place?.crowdLevel ?: CrowdLevel.UNKNOWN
    val score = forecast?.score ?: place?.concentrationScore
    val estimated = forecast?.let { it.estimated && !it.providerDataAvailable }
        ?: (place?.crowdEstimated == true && !place.crowdProviderDataAvailable)

    Box(Modifier.fillMaxSize().onGloballyPositioned { rootBounds = it.boundsInRoot() }) {
        Column(Modifier.fillMaxSize().background(Background)) {
            GayadiTopAppBar(title = "장소 정보", onBack = onBack, showDivider = true)
            if (place == null) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("장소 정보 없음", color = TextSecondary)
                }
            } else {
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
                Spacer(Modifier.height(12.dp))
                Box(
                    Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(20.dp)).background(SurfaceLight),
                    contentAlignment = Alignment.Center,
                ) {
                    PlacePhoto(place.imageUrl, "${place.name} 이미지", Modifier.fillMaxSize())
                }
                Spacer(Modifier.height(20.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(place.name, modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.headlineSmall, color = TextPrimary)
                    IconButton(onClick = onToggleFavorite) {
                        Icon(if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = if (isFavorite) "찜 해제" else "찜 추가",
                            tint = if (isFavorite) TagRedText else TextSecondary)
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(place.category, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
                if (place.description.isNotBlank()) {
                    Text(place.description, modifier = Modifier.padding(top = 16.dp),
                        style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
                Spacer(Modifier.height(20.dp))
                HorizontalDivider(color = Divider, thickness = 1.dp)
                Spacer(Modifier.height(20.dp))
                Text("방문 전 살펴보기", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                Spacer(Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Filled.Groups, contentDescription = null, tint = CrowdedMedium, modifier = Modifier.size(28.dp))
                    Column(Modifier.weight(1f)) {
                        Text("예상 혼잡도", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                        Text(
                            when {
                                forecastUiState.isLoading -> "조회 중"
                                forecastUiState.errorMessage != null -> "조회 실패"
                                score != null -> "혼잡 점수 $score" + if (estimated) " · 달력 추정" else ""
                                estimated -> "달력 추정"
                                level == CrowdLevel.UNKNOWN -> "정보 없음"
                                else -> "혼잡 예상"
                            },
                            style = MaterialTheme.typography.bodySmall, color = TextSecondary,
                        )
                    }
                    if (forecastUiState.errorMessage != null) {
                        TextButton(onClick = onForecastRetry) { Text("재시도", color = PrimaryAction) }
                    } else if (level != CrowdLevel.UNKNOWN) CrowdBadge(level)
                }
                Spacer(Modifier.height(20.dp))
                WeatherSummary(weatherUiState, onWeatherRetry)
                Spacer(Modifier.height(32.dp))
                Text("여유롭게 둘러볼 시간", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                Text("시간대별 혼잡 예상", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                Spacer(Modifier.height(16.dp))
                HourlyGraph(hourlyUiState, onHourlyRetry)
                Spacer(Modifier.height(20.dp))
            }
                Row(
                    Modifier.fillMaxWidth().background(Background).navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Button(
                        onClick = openScheduleOptions,
                        enabled = !isScheduled,
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                            .onGloballyPositioned { addButtonBounds = it.boundsInRoot() },
                        shape = RectangleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryAction, contentColor = Color.White),
                    ) {
                        Text(if (isScheduled) "추가됨" else "일정에 추가", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }

        val target = addButtonBounds
        val root = rootBounds
        if (isUsageGuideVisible && !isScheduled && !showScheduleOptions && target != null && root != null) {
            UsageGuideOverlay(
                callouts = listOf(
                    UsageGuideCallout(
                        target = target.translate(-root.topLeft),
                        text = AnnotatedString("마음에 드는 장소라면\n시간과 메모를 정해 일정에 추가하세요"),
                        placement = UsageGuidePlacement.ABOVE,
                    ),
                ),
                onDismiss = finishGuide,
                onTargetClick = { openScheduleOptions() },
            )
        }
    }
    if (showScheduleOptions && place != null) {
        ScheduleOptionsBottomSheet(
            title = place.name,
            contextText = listOf(tripName, tripDate).filter(String::isNotBlank).joinToString(" · "),
            onDismiss = { showScheduleOptions = false },
            onConfirm = { time, memo -> onAddToSchedule(time, memo); showScheduleOptions = false },
        )
    }
}

@Composable
private fun CrowdBadge(level: CrowdLevel) {
    val colors = when (level) {
        CrowdLevel.RELAXED -> TagGreen to TagGreenText
        CrowdLevel.NORMAL -> TagOrange to TagOrangeText
        CrowdLevel.CROWDED -> TagRed to TagRedText
        CrowdLevel.UNKNOWN -> TagGray to TagGrayText
    }
    Text(level.label, modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(colors.first)
        .padding(horizontal = 8.dp, vertical = 3.dp),
        style = MaterialTheme.typography.labelSmall, color = colors.second)
}

@Composable
private fun WeatherSummary(state: PlaceWeatherUiState, onRetry: () -> Unit) {
    val weather = state.weather?.takeIf { it.available && !state.isLoading && state.errorMessage == null }
    val condition = weather?.condition.orEmpty()
    val icon = when {
        "눈" in condition -> Icons.Outlined.AcUnit
        "비" in condition || "소나기" in condition -> Icons.Outlined.Umbrella
        "흐" in condition || "구름" in condition -> Icons.Outlined.Cloud
        "맑" in condition -> Icons.Outlined.WbSunny
        else -> Icons.Outlined.Thermostat
    }
    val description = listOfNotNull(
        condition.takeIf { it.isNotBlank() },
        weather?.precipitationProbability?.takeIf { it in 0..100 }?.let { "강수확률 $it%" },
    ).joinToString(" · ")
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(icon, contentDescription = null, tint = CrowdedMedium, modifier = Modifier.size(28.dp))
        Column(Modifier.weight(1f)) {
            Text("현재 날씨", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
            Text(when {
                state.isLoading -> "조회 중"
                state.errorMessage != null -> "조회 실패"
                description.isNotEmpty() -> description
                weather?.temperature?.toDoubleOrNull() == null -> "정보 없음"
                else -> "현재 기온"
            }, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }
        if (state.errorMessage != null) {
            TextButton(onClick = onRetry) { Text("재시도", color = PrimaryAction) }
        } else {
            val temperature = weather?.temperature?.toDoubleOrNull()?.takeIf { it.isFinite() }
            Text(temperature?.let { (if (it % 1.0 == 0.0) it.toInt().toString() else it.toString()) + "°" } ?: "—",
                style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = PrimaryAction)
        }
    }
}

@Composable
private fun HourlyGraph(state: CongestionHourlyUiState, onRetry: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        listOf(CrowdLevel.RELAXED, CrowdLevel.NORMAL, CrowdLevel.CROWDED).forEach { level ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(Modifier.size(6.dp).background(levelColor(level), RoundedCornerShape(3.dp)))
                Text(level.label, fontSize = 10.sp, color = TextSecondary)
            }
        }
    }
    val points = state.forecast?.points.orEmpty().filter { it.hour in 0..23 }.associateBy { it.hour }
    if (state.isLoading || state.errorMessage != null || points.isEmpty()) {
        Box(Modifier.fillMaxWidth().height(140.dp), contentAlignment = Alignment.Center) {
            when {
                state.isLoading -> CircularProgressIndicator(Modifier.size(24.dp), color = PrimaryAction, strokeWidth = 2.dp)
                state.errorMessage != null -> TextButton(onClick = onRetry) { Text("조회 실패 · 재시도", color = TextSecondary) }
                else -> TextButton(onClick = onRetry) { Text("혼잡 정보 없음 · 다시 확인", color = TextSecondary) }
            }
        }
    } else {
        Spacer(Modifier.height(24.dp))
        Row(Modifier.fillMaxWidth().height(110.dp), horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.Bottom) {
            (0..23).forEach { hour ->
                val point = points[hour]
                Box(Modifier.weight(1f).fillMaxHeight().semantics {
                    contentDescription = if (point == null) "${hour}시 정보 없음"
                    else "${hour}시, 혼잡 점수 ${point.concentrationScore}, ${point.level.toCrowdLevel().label}"
                }, contentAlignment = Alignment.BottomCenter) {
                    if (point != null) Box(Modifier.fillMaxWidth()
                        .height((110 * point.concentrationScore.coerceIn(0, 100) / 100f).dp)
                        .background(levelColor(point.level.toCrowdLevel()), RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp)))
                }
            }
        }
        HorizontalDivider(color = Divider)
        Row(Modifier.fillMaxWidth().padding(top = 6.dp)) {
            (0..7).forEach { tick ->
                Text("${(tick * 3).toString().padStart(2, '0')}시", modifier = Modifier.weight(1f),
                    fontSize = 10.sp, color = TextSecondary)
            }
        }
        val note = listOf(
            state.forecast?.targetDate?.takeIf(String::isNotBlank),
            if (state.forecast?.estimated == true && !state.forecast.providerDataAvailable) "달력 기반 추정" else "혼잡 예상",
        ).filterNotNull().joinToString(" · ")
        Text(note, modifier = Modifier.padding(top = 12.dp), fontSize = 10.sp, color = TextTertiary)
    }
}

private fun levelColor(level: CrowdLevel): Color = when (level) {
    CrowdLevel.RELAXED -> CrowdedLow
    CrowdLevel.NORMAL -> CrowdedMedium
    CrowdLevel.CROWDED -> CrowdedHigh
    CrowdLevel.UNKNOWN -> TextTertiary
}

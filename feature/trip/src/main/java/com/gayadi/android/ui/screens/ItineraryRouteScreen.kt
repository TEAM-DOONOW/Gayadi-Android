package com.gayadi.android.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gayadi.android.domain.model.RouteTransportMode
import com.gayadi.android.domain.repository.RecommendedItinerary
import com.gayadi.android.domain.repository.RecommendedItineraryStop
import com.gayadi.android.ui.components.GayadiTopAppBar
import com.gayadi.android.ui.components.PlacePhoto
import com.gayadi.android.ui.components.TransportModeSelector
import com.gayadi.android.ui.components.WheelTimePickerDialog
import com.gayadi.android.ui.theme.Background
import com.gayadi.android.ui.theme.Border
import com.gayadi.android.ui.theme.Divider
import com.gayadi.android.ui.theme.PrimaryAction
import com.gayadi.android.ui.theme.SurfaceLight
import com.gayadi.android.ui.theme.TextPrimary
import com.gayadi.android.ui.theme.TextSecondary
import com.gayadi.android.ui.theme.TextTertiary
import com.gayadi.android.ui.theme.GayadiTheme
import java.util.Locale

private enum class TimeField { START, END }

@Composable
fun ItineraryRouteScreen(
    uiState: ItineraryRouteUiState,
    onBack: () -> Unit,
    onTransportModeSelected: (RouteTransportMode) -> Unit,
    onTimeRangeSelected: (String, String) -> Unit,
    onNewRoute: () -> Unit,
    onRetry: () -> Unit,
    onApply: () -> Unit,
) {
    var showApplyConfirmation by remember { mutableStateOf(false) }
    var editingTimeField by remember { mutableStateOf<TimeField?>(null) }
    Scaffold(
        containerColor = Background,
        topBar = {
            GayadiTopAppBar(
                title = "여행루트",
                subtitle = "${uiState.date} · 거리·이동시간·체류시간을 함께 맞춰요",
                onBack = onBack,
                showDivider = true,
            )
        },
        bottomBar = {
            Surface(color = Background, shadowElevation = 0.dp, border = BorderStroke(1.dp, Divider)) {
                Button(
                    onClick = { showApplyConfirmation = true },
                    enabled = uiState.recommendation != null && !uiState.isLoading && !uiState.isApplying,
                    modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(20.dp).height(52.dp),
                    shape = RoundedCornerShape(0.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryAction),
                ) {
                    if (uiState.isApplying) {
                        CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Text("이 루트로 일정 바꾸기", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(start = 20.dp, top = 20.dp, end = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Text("여행 시간", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TimeSelectionButton(
                        label = "시작",
                        value = uiState.startTime,
                        onClick = { editingTimeField = TimeField.START },
                        modifier = Modifier.weight(1f),
                    )
                    Text("—", color = TextTertiary, textAlign = TextAlign.Center)
                    TimeSelectionButton(
                        label = "종료",
                        value = uiState.endTime,
                        onClick = { editingTimeField = TimeField.END },
                        modifier = Modifier.weight(1f),
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    "3~12시간 범위에서 고르면 루트를 자동으로 다시 맞춰요.",
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                )
            }
            item {
                Text("이동수단", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                Spacer(Modifier.height(8.dp))
                TransportModeSelector(uiState.transportMode) { mode ->
                    mode?.let(onTransportModeSelected)
                }
            }
            when {
                uiState.isLoading -> item {
                    Box(Modifier.fillMaxWidth().height(220.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = PrimaryAction)
                            Spacer(Modifier.height(12.dp))
                            Text("시간과 거리를 계산해 루트를 만드는 중이에요", color = TextSecondary)
                        }
                    }
                }
                uiState.errorMessage != null -> item {
                    ErrorCard(uiState.errorMessage, onRetry)
                }
                uiState.recommendation != null -> {
                    item { RouteSummary(uiState.recommendation, onNewRoute) }
                    uiState.applyErrorMessage?.let { message ->
                        item { ApplyErrorCard(message) }
                    }
                    items(uiState.recommendation.stops, key = { it.placeId }) { stop ->
                        if (stop.travelMinutesFromPrevious > 0) {
                            TravelConnector(
                                stop.travelMinutesFromPrevious,
                                stop.distanceMetersFromPrevious,
                                uiState.transportMode,
                            )
                        }
                        RouteStopCard(stop)
                    }
                }
            }
        }
    }

    if (showApplyConfirmation) {
        AlertDialog(
            onDismissRequest = { showApplyConfirmation = false },
            title = { Text("이 루트로 일정을 바꿀까요?") },
            text = {
                Text("${uiState.date}의 기존 장소 일정을 모두 지우고, 추천받은 순서와 체류시간을 그대로 넣어요. 기존 일정에 연결된 비용은 일정과의 연결이 해제될 수 있어요.")
            },
            confirmButton = {
                TextButton(onClick = {
                    showApplyConfirmation = false
                    onApply()
                }) { Text("일정 바꾸기", color = PrimaryAction) }
            },
            dismissButton = {
                TextButton(onClick = { showApplyConfirmation = false }) { Text("취소") }
            },
        )
    }

    editingTimeField?.let { field ->
        WheelTimePickerDialog(
            initialTime = if (field == TimeField.START) uiState.startTime else uiState.endTime,
            onDismiss = { editingTimeField = null },
            onConfirm = { selectedTime ->
                editingTimeField = null
                if (field == TimeField.START) {
                    onTimeRangeSelected(selectedTime, uiState.endTime)
                } else {
                    onTimeRangeSelected(uiState.startTime, selectedTime)
                }
            },
        )
    }
}

@Composable
private fun TimeSelectionButton(
    label: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(64.dp),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, Border),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        colors = ButtonDefaults.outlinedButtonColors(containerColor = Background),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = label,
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                textAlign = TextAlign.Center,
            )
            Text(
                text = value,
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun RouteSummary(route: RecommendedItinerary, onNewRoute: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Background),
        border = BorderStroke(1.dp, Divider),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.AutoAwesome, contentDescription = null, tint = PrimaryAction)
                Spacer(Modifier.width(8.dp))
                Text("가야디가 맞춰본 하루 루트", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            }
            Spacer(Modifier.height(8.dp))
            Text(route.summary, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SummaryPill("방문 ${route.stops.size}곳")
                SummaryPill("체류 ${minutesLabel(route.totalStayMinutes)}")
                SummaryPill("이동 ${minutesLabel(route.totalTravelMinutes)}")
            }
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    Icons.Outlined.Lock,
                    contentDescription = null,
                    tint = TextTertiary,
                    modifier = Modifier.padding(top = 2.dp).size(14.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    buildString {
                        append("장소는 루트에 고정돼요. 바꾸려면 새 루트를 받아 전체를 다시 맞춰요.")
                        if (route.estimated) append(" 이동시간은 직선거리 기준 예상치예요.")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = TextTertiary,
                )
            }
            Spacer(Modifier.height(14.dp))
            OutlinedButton(
                onClick = onNewRoute,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(0.dp),
                border = BorderStroke(1.dp, PrimaryAction),
            ) {
                Icon(Icons.Outlined.Refresh, contentDescription = null, tint = PrimaryAction, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("새 루트 받기", color = PrimaryAction)
            }
        }
    }
}

@Composable
private fun SummaryPill(text: String) {
    Text(
        text,
        modifier = Modifier.clip(CircleShape).background(SurfaceLight).padding(horizontal = 10.dp, vertical = 6.dp),
        style = MaterialTheme.typography.labelMedium,
        color = TextPrimary,
    )
}

@Composable
private fun TravelConnector(minutes: Int, meters: Int, mode: RouteTransportMode) {
    Row(Modifier.fillMaxWidth().padding(start = 24.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width(1.dp).height(24.dp).background(Divider))
        Spacer(Modifier.width(14.dp))
        Text(
            itineraryTravelLabel(mode, minutes, meters),
            style = MaterialTheme.typography.labelMedium,
            color = TextSecondary,
        )
    }
}

internal fun itineraryTravelLabel(mode: RouteTransportMode, minutes: Int, meters: Int): String {
    val modeLabel = when (mode) {
        RouteTransportMode.CAR -> "자동차"
        RouteTransportMode.PUBLIC_TRANSIT -> "대중교통"
        RouteTransportMode.WALK -> "도보"
        RouteTransportMode.BICYCLE -> "자전거"
    }
    val distance = when {
        meters <= 0 -> ""
        meters < 1_000 -> "${meters}m"
        else -> String.format(Locale.US, "%.1fkm", meters / 1_000.0)
    }
    return if (distance.isEmpty()) "$modeLabel 약 ${minutes}분" else "$modeLabel $distance · 약 ${minutes}분"
}

@Composable
private fun RouteStopCard(stop: RecommendedItineraryStop) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Background),
        border = BorderStroke(1.dp, Divider),
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(32.dp).background(PrimaryAction, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(stop.order.toString(), color = Color.White, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(12.dp))
            PlacePhoto(
                imageUrl = stop.imageUrl,
                contentDescription = "${stop.name} 이미지",
                modifier = Modifier.size(64.dp).clip(RoundedCornerShape(10.dp)),
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(stop.name, style = MaterialTheme.typography.titleSmall, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(stop.category, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                Spacer(Modifier.height(6.dp))
                Text("${stop.arrivalTime} - ${stop.departureTime}", style = MaterialTheme.typography.labelLarge, color = PrimaryAction)
                Text("머무는 시간 ${minutesLabel(stop.stayMinutes)}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
            Icon(Icons.Outlined.Lock, contentDescription = "루트에 고정된 장소", tint = TextTertiary, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun ApplyErrorCard(message: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        shape = RoundedCornerShape(12.dp),
    ) {
        Text(
            text = message,
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onErrorContainer,
        )
    }
}

@Composable
private fun ErrorCard(message: String, onRetry: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = SurfaceLight), shape = RoundedCornerShape(12.dp)) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text(message, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            TextButton(onClick = onRetry) { Text("다시 시도", color = PrimaryAction) }
        }
    }
}

private fun minutesLabel(minutes: Int): String {
    val hours = minutes / 60
    val remainder = minutes % 60
    return when {
        hours == 0 -> "${remainder}분"
        remainder == 0 -> "${hours}시간"
        else -> "${hours}시간 ${remainder}분"
    }
}

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun ItineraryRouteScreenPreview() {
    GayadiTheme {
        ItineraryRouteScreen(
            uiState = ItineraryRouteUiState(
                date = "2026.10.03",
                recommendation = RecommendedItinerary(
                    date = "2026.10.03",
                    startTime = "10:00",
                    endTime = "18:00",
                    transportMode = RouteTransportMode.PUBLIC_TRANSIT,
                    variation = 0,
                    estimated = true,
                    totalTravelMinutes = 42,
                    totalStayMinutes = 255,
                    summary = "3곳을 255분 동안 둘러보고, 이동에는 약 42분이 걸려요.",
                    stops = listOf(
                        RecommendedItineraryStop(1, "1", "경복궁", "관광명소", "", 37.57, 126.97, "10:00", "11:20", 80, 0, 0),
                        RecommendedItineraryStop(2, "2", "인사동", "쇼핑", "", 37.57, 126.98, "11:38", "12:53", 75, 18, 1200),
                        RecommendedItineraryStop(3, "3", "광화문 맛집", "맛집", "", 37.56, 126.97, "13:17", "14:17", 60, 24, 1800),
                    ),
                ),
            ),
            onBack = {},
            onTransportModeSelected = {},
            onTimeRangeSelected = { _, _ -> },
            onNewRoute = {},
            onRetry = {},
            onApply = {},
        )
    }
}

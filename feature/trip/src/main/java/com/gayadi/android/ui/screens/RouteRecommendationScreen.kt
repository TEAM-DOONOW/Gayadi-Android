package com.gayadi.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gayadi.android.domain.model.TravelSchedule
import com.gayadi.android.domain.model.TravelTrip
import com.gayadi.android.domain.model.UserProfile
import com.gayadi.android.ui.components.UserCharacterAvatar
import com.gayadi.android.ui.components.GayadiTopAppBar
import com.gayadi.android.ui.theme.PrimaryBlue
import com.gayadi.android.ui.theme.SurfaceCard
import com.gayadi.android.ui.theme.TextSecondary

enum class RouteRecommendationType { DEPARTURE, ITINERARY, HOME }

@Composable
fun RouteRecommendationScreen(
    type: RouteRecommendationType,
    trip: TravelTrip?,
    schedules: List<TravelSchedule>,
    profile: UserProfile?,
    appliedOptionId: String?,
    onBack: () -> Unit,
    onApply: (String) -> Unit,
) {
    val options = remember(type, schedules) { routeOptions(type, schedules) }
    var selectedId by remember(type, appliedOptionId) { mutableStateOf(appliedOptionId ?: options.firstOrNull()?.id) }
    Column(Modifier.fillMaxSize().background(Color.White).verticalScroll(rememberScrollState())) {
        GayadiTopAppBar(
            title = type.title,
            subtitle = trip?.name ?: "선택한 여행",
            onBack = onBack,
        ) {
            UserCharacterAvatar(profile?.characterKey, "맞춤 경로 캐릭터")
        }
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("${profile?.nickname ?: "여행자"} 님의 여행 성향과 현재 일정에 맞춰 추천했어요.", color = TextSecondary, fontSize = 13.sp)
            options.forEach { option ->
                val selected = option.id == selectedId
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { selectedId = option.id },
                    colors = CardDefaults.cardColors(containerColor = if (selected) Color(0xFFEAF4FF) else SurfaceCard),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(option.name, fontWeight = FontWeight.Bold)
                            Text(option.duration, color = PrimaryBlue)
                        }
                        Text(option.summary, fontSize = 12.sp, color = TextSecondary)
                        Text(option.steps.joinToString("  →  "), fontSize = 12.sp)
                    }
                }
            }
            Button(
                onClick = { selectedId?.let(onApply) },
                enabled = selectedId != null,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (appliedOptionId == selectedId) "경로 적용됨" else "선택한 경로 사용")
            }
            appliedOptionId?.let { Text("추천 경로를 이번 여행에 적용했어요", color = PrimaryBlue, fontSize = 12.sp) }
            Spacer(Modifier.height(24.dp))
        }
    }
}

private data class RouteOption(val id: String, val name: String, val duration: String, val summary: String, val steps: List<String>)

private fun routeOptions(type: RouteRecommendationType, schedules: List<TravelSchedule>): List<RouteOption> = when (type) {
    RouteRecommendationType.DEPARTURE -> listOf(
        RouteOption("fast", "가장 빠른 출발", "1시간 25분", "환승 1회 · 예상 혼잡 보통", listOf("현재 위치", "공항", "여행지")),
        RouteOption("easy", "편안한 출발", "1시간 40분", "걷기 최소 · 짐이 있을 때 추천", listOf("현재 위치", "직행 버스", "여행지")),
    )
    RouteRecommendationType.ITINERARY -> {
        val steps = schedules.sortedBy { it.order }.map(TravelSchedule::title).ifEmpty { listOf("첫 장소", "추천 맛집", "숙소") }
        listOf(
            RouteOption("balanced", "균형 동선", "이동 48분", "거리와 혼잡도를 함께 줄였어요", steps),
            RouteOption("crowd", "한적한 동선", "이동 56분", "혼잡 시간대를 피해 순서를 조정했어요", steps.reversed()),
        )
    }
    RouteRecommendationType.HOME -> listOf(
        RouteOption("home-fast", "빠른 귀가", "1시간 30분", "현재 일정 종료 후 바로 출발", listOf("마지막 장소", "공항", "집")),
        RouteOption("home-rest", "여유로운 귀가", "1시간 55분", "휴식 시간을 포함한 경로", listOf("마지막 장소", "카페", "공항", "집")),
    )
}

private val RouteRecommendationType.title: String
    get() = when (this) {
        RouteRecommendationType.DEPARTURE -> "출발 경로 추천"
        RouteRecommendationType.ITINERARY -> "여행 동선 추천"
        RouteRecommendationType.HOME -> "귀가 경로 추천"
    }

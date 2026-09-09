package com.gayadi.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gayadi.android.domain.model.TravelTrip
import com.gayadi.android.domain.model.TripStatus
import com.gayadi.android.domain.repository.*
import com.gayadi.android.ui.components.GayadiTopAppBar
import com.gayadi.android.ui.theme.*

enum class RouteRecommendationType { DEPARTURE, ITINERARY, HOME }

@Composable
fun RouteRecommendationScreen(
    type: RouteRecommendationType,
    trip: TravelTrip?,
    state: PlanningUiState,
    onBack: () -> Unit,
    onReload: () -> Unit,
    onRecommend: () -> Unit,
    onApply: (RecommendedRoute) -> Unit,
    onClear: () -> Unit,
    onGenerate: () -> Unit,
    onSurvey: () -> Unit,
) {
    var confirmGenerate by remember { mutableStateOf(false) }
    var showPlan by rememberSaveable { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().background(Background).navigationBarsPadding()) {
        GayadiTopAppBar(title=type.title, subtitle=trip?.name.orEmpty(), onBack=onBack)
        if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement=Arrangement.spacedBy(16.dp)) {
            state.error?.let {
                Text(it, color=MaterialTheme.colorScheme.error)
                OutlinedButton(onClick=onReload, enabled=!state.busy) { Text("다시 조회") }
            }
            Text("자동 일정", style=MaterialTheme.typography.titleLarge)
            Text("여행 기간과 참여자의 성향으로 일정을 만들고, 이를 기준으로 이동 경로를 추천해요.", color=TextSecondary)
            OutlinedButton(onClick=onSurvey, enabled=!state.busy) { Text("이 여행 성향 등록") }
            if (state.plan == null) Text("아직 생성된 자동 일정이 없어요.", color=TextSecondary)
            else {
                Text("${state.plan.days.size}일 · ${state.plan.days.sumOf { it.items.size }}개 장소", color=TextSecondary)
                TextButton(onClick={showPlan=!showPlan}) { Text(if(showPlan) "자동 일정 접기" else "자동 일정 보기") }
            }
            if(showPlan) state.plan?.days?.forEach { day ->
                Card(colors=CardDefaults.cardColors(containerColor=SurfaceCard), modifier=Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement=Arrangement.spacedBy(8.dp)) {
                        Text("${day.date} · ${day.title}", style=MaterialTheme.typography.titleMedium)
                        day.items.forEach { item ->
                            Text("${item.start.substringAfter('T', "").take(5)}  ${item.title}")
                            if(item.address.isNotBlank()) Text(item.address, style=MaterialTheme.typography.bodySmall, color=TextSecondary)
                        }
                    }
                }
            }
            OutlinedButton(onClick={ confirmGenerate=true }, enabled=!state.busy && trip?.status==TripStatus.PLANNING) {
                Text(if(state.plan==null) "자동 일정 만들기" else "자동 일정 다시 만들기")
            }
            if(trip?.status!=TripStatus.PLANNING) Text("자동 일정은 여행 준비 중에 만들 수 있어요.", color=TextSecondary)
            if(type != RouteRecommendationType.ITINERARY) {
                Text("이미 등록된 개인 장소를 기준으로 추천해요. 이 화면에서는 출발·귀가 장소를 변경할 수 없어요.", color=TextSecondary)
            }
            Text("이동 경로", style=MaterialTheme.typography.titleLarge)
            Button(onClick=onRecommend, enabled=!state.busy && state.plan!=null,
                modifier=Modifier.fillMaxWidth().heightIn(min=55.dp), shape=RoundedCornerShape(0.dp),
                colors=ButtonDefaults.buttonColors(containerColor=PrimaryAction)) { Text("경로 추천받기") }
            if(state.routes.isEmpty() && !state.busy) Text("추천받기를 눌러 실제 이동 경로를 확인해 주세요.", color=TextSecondary)
            state.routes.forEach { route ->
                val selected = state.selected?.id==route.id && state.selected.optionId==route.optionId
                Card(colors=CardDefaults.cardColors(containerColor=SurfaceCard), modifier=Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement=Arrangement.spacedBy(8.dp)) {
                        Text(route.name, style=MaterialTheme.typography.titleMedium)
                        val metrics=listOfNotNull(route.durationMinutes?.let { "${it}분" },
                            route.distanceMeters?.let { "${it / 1000.0}km" }, route.fare?.let { "${it}원" })
                        Text(metrics.joinToString(" · ").ifBlank { "이동 정보가 제공되지 않았어요" })
                        Text(route.summary, color=TextSecondary)
                        if(route.isEstimate) Text("예상 경로예요. 실제 교통 상황과 다를 수 있어요.", color=TextSecondary)
                        Text(route.stops.joinToString(" → "))
                        Button(onClick={onApply(route)}, enabled=!state.busy && !selected) { Text(if(selected) "선택한 경로" else "이 경로 사용") }
                    }
                }
            }
            if(state.selected!=null) OutlinedButton(onClick=onClear, enabled=!state.busy) { Text("경로 선택 해제") }
        }
    }
    if(confirmGenerate) AlertDialog(onDismissRequest={confirmGenerate=false}, title={Text("자동 일정을 만들까요?")},
        text={Text("기존 자동 일정과 항목이 새 추천으로 바뀌어요. 여행 기간과 참여자의 성향을 확인해 주세요.")},
        confirmButton={TextButton(onClick={confirmGenerate=false;onGenerate()}){Text("만들기")}},
        dismissButton={TextButton(onClick={confirmGenerate=false}){Text("취소")}})
}
private val RouteRecommendationType.title: String get() = when(this) {
    RouteRecommendationType.DEPARTURE -> "출발 경로 추천"
    RouteRecommendationType.ITINERARY -> "여행 동선 추천"
    RouteRecommendationType.HOME -> "귀가 경로 추천"
}

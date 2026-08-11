package com.gayadi.android.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AltRoute
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gayadi.android.feature.home.R
import com.gayadi.android.ui.components.BottomNavBar
import com.gayadi.android.ui.components.BottomTab
import com.gayadi.android.ui.components.UserCharacterAvatar
import com.gayadi.android.ui.theme.AlertBlue
import com.gayadi.android.ui.theme.AlertBlueText
import com.gayadi.android.ui.theme.GayadiTheme
import com.gayadi.android.ui.theme.PrimaryBlue
import com.gayadi.android.ui.theme.PrimaryAction
import com.gayadi.android.ui.theme.TagBlue
import com.gayadi.android.ui.theme.TagBlueText
import com.gayadi.android.ui.theme.TextPrimary
import com.gayadi.android.ui.theme.TextSecondary
import com.gayadi.android.ui.theme.TextTertiary

data class HomeTravelPlan(
    val title: String,
    val date: String,
    val time: String,
    val isVisited: Boolean,
)

data class HomeTripDay(
    val dayNumber: Int,
    val date: String,
    val dateLabel: String,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RealtimeHomeScreen(
    uiState: RealtimeHomeUiState,
    tripTitle: String,
    nextScheduleName: String?,
    hasSchedules: Boolean = false,
    travelPlans: List<HomeTravelPlan> = emptyList(),
    tripDays: List<HomeTripDay> = emptyList(),
    participantCount: Int = 0,
    tripDday: String = "D-day",
    friendCharacterKeys: List<String?> = emptyList(),
    onNavigateMyTrip: () -> Unit,
    onNavigateMyPage: () -> Unit,
    onNavigatePlaceSearch: () -> Unit,
    onNavigateFriendAdd: () -> Unit,
    onNavigateParticipants: () -> Unit,
    onNavigateInvitation: () -> Unit,
    onNavigateSchedule: () -> Unit,
    onNavigateRoutes: () -> Unit,
    onOpenReschedule: () -> Unit,
    onDismissReschedule: () -> Unit,
    onAcceptReschedule: () -> Unit,
    onRejectReschedule: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF7F7F9)),
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
            ) {
                Spacer(modifier = Modifier.height(56.dp))

                Text(
                    text = "두근두근 여행 준비",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextSecondary,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "$tripTitle 여행일까지",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    lineHeight = 28.sp,
                )
                Text(
                    text = buildAnnotatedString {
                        withStyle(SpanStyle(color = Color(0xFFFF5A2A))) {
                            append(tripDday)
                        }
                        append(" 남았어요!")
                    },
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    lineHeight = 28.sp,
                )
                Spacer(modifier = Modifier.height(18.dp))

                TravelOverviewCard(
                    plans = travelPlans,
                    participantCount = participantCount,
                    myCharacterKey = uiState.profile?.characterKey,
                    friendCharacterKeys = friendCharacterKeys,
                    onParticipants = onNavigateParticipants,
                )

                Spacer(modifier = Modifier.height(24.dp))
                Text("여행 동선", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(modifier = Modifier.height(10.dp))
                TravelRoutePreview(plans = travelPlans, onClick = onNavigateRoutes)

                Spacer(modifier = Modifier.height(24.dp))
                Text("여행 계획", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(modifier = Modifier.height(14.dp))
                tripDays.forEach { day ->
                    TripDaySection(
                        day = day,
                        plans = travelPlans.filter { it.date == day.date },
                        onAddPlace = onNavigatePlaceSearch,
                        onPlanClick = onNavigateSchedule,
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                }

                Spacer(modifier = Modifier.height(24.dp))

                if (false) {
                uiState.profile?.nickname?.let { nickname ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("$nickname 님의 맞춤 여행", fontSize = 12.sp, color = TextSecondary)
                }

                Spacer(modifier = Modifier.height(16.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FB)),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        TripManagementAction(
                            label = "참여자",
                            icon = { Icon(Icons.Filled.Group, contentDescription = null) },
                            onClick = onNavigateParticipants,
                        )
                        TripManagementAction(
                            label = "초대",
                            icon = { Icon(Icons.Filled.PersonAdd, contentDescription = null) },
                            onClick = onNavigateInvitation,
                        )
                        TripManagementAction(
                            label = "일정",
                            icon = { Icon(Icons.Filled.CalendarMonth, contentDescription = null) },
                            onClick = onNavigateSchedule,
                        )
                        TripManagementAction(
                            label = "경로",
                            icon = { Icon(Icons.Filled.AltRoute, contentDescription = null) },
                            onClick = onNavigateRoutes,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onOpenReschedule)
                        .then(Modifier.padding(0.dp)),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = AlertBlue),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🌧️", fontSize = 18.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("실시간 알림: 방금", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = AlertBlueText)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            when (uiState.rescheduleDecision) {
                                RescheduleDecision.PENDING -> "곧 비가 와요, 실내로 다시 바꿀까요?"
                                RescheduleDecision.ACCEPTED -> "추천 일정으로 변경했어요"
                                RescheduleDecision.REJECTED -> "기존 일정을 유지해요"
                            },
                            fontSize = 14.sp,
                            color = TextPrimary,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    HomeQuickAction("장소 찾기", onNavigatePlaceSearch)
                    HomeQuickAction("여행메이트", onNavigateFriendAdd)
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    InfoChip("날씨", "14시 비")
                    InfoChip("혼잡도", "혼잡")
                    InfoChip("여유", "여유")
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("오늘의 동선", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    Text(
                        "전체보기",
                        fontSize = 13.sp,
                        color = PrimaryBlue,
                        modifier = Modifier.clickable(onClick = onNavigatePlaceSearch),
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FB)),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .padding(16.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (hasSchedules) Row(
                            horizontalArrangement = Arrangement.spacedBy(24.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RouteDot("1", PrimaryBlue)
                            Text("···", color = TextTertiary)
                            RouteDot("2", Color(0xFF666666))
                            Text("···", color = TextTertiary)
                            RouteDot("3", Color(0xFF666666))
                        }
                        if (!hasSchedules) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("아직 등록된 일정이 없어요", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("일정을 추가하면 여행 동선이 표시됩니다", fontSize = 12.sp, color = TextSecondary)
                            }
                        }
                        Text(
                            "실내 구역",
                            fontSize = 10.sp,
                            color = TextTertiary,
                            modifier = Modifier.align(Alignment.TopEnd),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color(0xFFE8E8E8), RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFF0F0F0)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("🍲", fontSize = 22.sp)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                if (nextScheduleName == null) "다음 일정 없음" else "다음 일정 · 13:00",
                                fontSize = 11.sp,
                                color = TextTertiary,
                            )
                            Text(
                                nextScheduleName ?: "장소를 일정에 추가해 보세요",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary,
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(PrimaryBlue)
                                .clickable(onClick = onNavigatePlaceSearch)
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                        ) {
                            Text(
                                "장소 찾기",
                                fontSize = 12.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                }
            }

            BottomNavBar(
                currentTab = BottomTab.OUR_TRIP,
                onTabSelected = { tab ->
                    when (tab) {
                        BottomTab.MY_TRIP -> onNavigateMyTrip()
                        BottomTab.MY_PAGE -> onNavigateMyPage()
                        else -> {}
                    }
                },
            )
        }

        if (uiState.showRescheduleSheet) {
            RescheduleBottomSheet(
                onDismiss = onDismissReschedule,
                onKeep = onRejectReschedule,
                onAccept = onAcceptReschedule,
            )
        }
    }
}

@Composable
private fun TravelOverviewCard(
    plans: List<HomeTravelPlan>,
    participantCount: Int,
    myCharacterKey: String?,
    friendCharacterKeys: List<String?>,
    onParticipants: () -> Unit,
) {
    val completed = plans.count { it.isVisited }
    val progress = if (plans.isEmpty()) 0f else completed.toFloat() / plans.size
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(185.dp),
    ) {
        Image(
            painter = painterResource(R.drawable.gayadi_letter),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds,
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 20.dp),
        ) {
            Text(
                "우리 여행 진행률",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
            )
            Spacer(Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(12.dp).clip(CircleShape),
                color = Color(0xFFFF5A2A),
                trackColor = Color(0xFFFFC9B6),
            )
            Spacer(Modifier.height(5.dp))
            Text(
                "${(progress * 100).toInt()}%",
                modifier = Modifier.align(Alignment.End),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFF5A2A),
            )
            Spacer(Modifier.height(14.dp))
            Column(
                Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onParticipants),
            ) {
                Text(
                    "함께 하는 친구",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                )
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    UserCharacterAvatar(myCharacterKey, "내 캐릭터", Modifier.size(30.dp))
                    Spacer(Modifier.width(4.dp))
                    friendCharacterKeys.take(2).forEach { key ->
                        UserCharacterAvatar(key, "함께하는 친구", Modifier.size(30.dp))
                        Spacer(Modifier.width(4.dp))
                    }
                    Box(Modifier.size(30.dp).clip(CircleShape).background(Color(0xFFECECF1)), contentAlignment = Alignment.Center) {
                        Text(if (participantCount == 0) "+" else "+$participantCount", fontSize = 10.sp, color = TextSecondary)
                    }
                }
            }
        }
    }
}

@Composable
private fun TravelRoutePreview(plans: List<HomeTravelPlan>, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FB)),
    ) {
        if (plans.isEmpty()) {
            Column(Modifier.fillMaxWidth().padding(vertical = 28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("동선이 아직 없어요", fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Spacer(Modifier.height(5.dp))
                Text("계획을 추가하면 이곳에 동선이 표시돼요", fontSize = 12.sp, color = TextSecondary)
            }
        } else {
            Row(
                Modifier.fillMaxWidth().padding(18.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                plans.take(3).forEachIndexed { index, plan ->
                    if (index > 0) Text("→", color = TextTertiary)
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        RouteDot((index + 1).toString(), if (plan.isVisited) PrimaryBlue else PrimaryAction)
                        Spacer(Modifier.height(6.dp))
                        Text(plan.title, maxLines = 1, fontSize = 11.sp, color = TextPrimary)
                    }
                }
            }
        }
    }
}

@Composable
private fun TravelPlanRow(index: Int, plan: HomeTravelPlan, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(0.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE8E8EC)),
    ) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            RouteDot(index.toString(), if (plan.isVisited) PrimaryBlue else PrimaryAction)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(plan.title, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Text("${plan.date}  ${plan.time}", fontSize = 12.sp, color = TextSecondary)
            }
            Text(if (plan.isVisited) "완료" else "예정", fontSize = 11.sp, color = if (plan.isVisited) PrimaryBlue else TextSecondary)
        }
    }
}

@Composable
private fun TripDaySection(
    day: HomeTripDay,
    plans: List<HomeTravelPlan>,
    onAddPlace: () -> Unit,
    onPlanClick: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "DAY ${day.dayNumber}",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = day.dateLabel,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = TextSecondary,
        )
    }
    if (plans.isNotEmpty()) {
        Spacer(modifier = Modifier.height(12.dp))
        plans.forEachIndexed { index, plan ->
            TravelPlanRow(index = index + 1, plan = plan, onClick = onPlanClick)
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
    Spacer(modifier = Modifier.height(14.dp))
    Button(
        onClick = onAddPlace,
        modifier = Modifier.fillMaxWidth().height(40.dp),
        shape = RoundedCornerShape(0.dp),
        colors = ButtonDefaults.buttonColors(containerColor = PrimaryAction),
    ) { Text("장소 추가", fontSize = 14.sp, fontWeight = FontWeight.SemiBold) }
}

@Composable
private fun TripManagementAction(
    label: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Color.White),
            contentAlignment = Alignment.Center,
        ) {
            androidx.compose.runtime.CompositionLocalProvider(
                androidx.compose.material3.LocalContentColor provides PrimaryAction,
                content = icon,
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
    }
}

@Composable
private fun HomeQuickAction(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(TagBlue)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
    ) {
        Text(label, fontSize = 12.sp, color = TagBlueText, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun InfoChip(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 11.sp, color = TextTertiary)
        Spacer(modifier = Modifier.height(2.dp))
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
    }
}

@Composable
private fun RouteDot(number: String, color: Color) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(color),
        contentAlignment = Alignment.Center,
    ) {
        Text(number, fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.Bold)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RescheduleBottomSheet(
    onDismiss: () -> Unit,
    onKeep: () -> Unit,
    onAccept: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        containerColor = Color.White,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFFD0D0D0)),
            )

            Spacer(modifier = Modifier.height(20.dp))

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(AlertBlue)
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            ) {
                Text("실시간 추천 대응", fontSize = 11.sp, color = AlertBlueText, fontWeight = FontWeight.Medium)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("오후 야외 일정,", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text("실내로 비꿀까요?", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                }
                Text("🌧️", fontSize = 40.sp)
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text("14-17시 섭지코지 일대 강수 예보", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color(0xFFE53935))
            Text("야외 유지 시 관람이 어렵고 동선이 꼬여요.", fontSize = 12.sp, color = TextSecondary)

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FB)),
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("기존", fontSize = 11.sp, color = TextTertiary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("15:00 섭지코지", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                        Text("야외 · 우천 영향", fontSize = 11.sp, color = Color(0xFFE53935))
                    }
                }
                Text("→", fontSize = 18.sp, color = TextTertiary, modifier = Modifier.align(Alignment.CenterVertically))
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = AlertBlue),
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("추천", fontSize = 11.sp, color = AlertBlueText)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("15:30 아쿠아플라넷", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                        Text("실내 · 우천 무관", fontSize = 11.sp, color = AlertBlueText)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text("실내 대체 장소", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, PrimaryBlue, RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFE3F2FD)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("🐠", fontSize = 22.sp)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("아쿠아플라넷 제주", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(TagBlue)
                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                            ) {
                                Text("실내", fontSize = 10.sp, color = TagBlueText)
                            }
                        }
                        Text("실내 · ★4.5 · 차로 8분", fontSize = 11.sp, color = TextSecondary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Button(
                    onClick = onKeep,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(0.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = TextPrimary,
                    ),
                ) {
                    Text("유지", fontSize = 15.sp, fontWeight = FontWeight.Medium)
                }
                Button(
                    onClick = onAccept,
                    modifier = Modifier
                        .weight(1.5f)
                        .height(48.dp),
                    shape = RoundedCornerShape(0.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryAction),
                ) {
                    Text("AI 추천대로 변경", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun RealtimeHomePreview() {
    GayadiTheme {
        RealtimeHomeScreen(
            uiState = RealtimeHomeUiState(),
            tripTitle = "제주 여행",
            nextScheduleName = "명진전복",
            hasSchedules = true,
            onNavigateMyTrip = {},
            onNavigateMyPage = {},
            onNavigatePlaceSearch = {},
            onNavigateFriendAdd = {},
            onNavigateParticipants = {},
            onNavigateInvitation = {},
            onNavigateSchedule = {},
            onNavigateRoutes = {},
            onOpenReschedule = {},
            onDismissReschedule = {},
            onAcceptReschedule = {},
            onRejectReschedule = {},
        )
    }
}

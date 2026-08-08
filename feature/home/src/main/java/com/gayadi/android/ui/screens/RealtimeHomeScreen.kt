package com.gayadi.android.ui.screens

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
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RealtimeHomeScreen(
    uiState: RealtimeHomeUiState,
    tripTitle: String,
    tripSubtitle: String,
    nextScheduleName: String?,
    onNavigateMyTrip: () -> Unit,
    onNavigateMyPage: () -> Unit,
    onNavigatePlaceSearch: () -> Unit,
    onNavigateFriendAdd: () -> Unit,
    onOpenReschedule: () -> Unit,
    onDismissReschedule: () -> Unit,
    onAcceptReschedule: () -> Unit,
    onRejectReschedule: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White),
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
            ) {
                Spacer(modifier = Modifier.height(56.dp))

                Text(tripSubtitle, fontSize = 13.sp, color = TextSecondary)
                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(tripTitle, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    UserCharacterAvatar(
                        characterKey = uiState.profile?.characterKey,
                        contentDescription = "${uiState.profile?.nickname.orEmpty()} 여행 성향 캐릭터",
                        modifier = Modifier
                            .size(44.dp)
                            .clickable(onClick = onNavigateMyPage),
                    )
                }

                uiState.profile?.nickname?.let { nickname ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("$nickname 님의 맞춤 여행", fontSize = 12.sp, color = TextSecondary)
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
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(24.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RouteDot("1", PrimaryBlue)
                            Text("···", color = TextTertiary)
                            RouteDot("2", Color(0xFF666666))
                            Text("···", color = TextTertiary)
                            RouteDot("3", Color(0xFF666666))
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
                                if (nextScheduleName == null) "장소 찾기" else "일정 보기",
                                fontSize = 12.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
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
            tripSubtitle = "여행 2일차 · 제주",
            nextScheduleName = "명진전복",
            onNavigateMyTrip = {},
            onNavigateMyPage = {},
            onNavigatePlaceSearch = {},
            onNavigateFriendAdd = {},
            onOpenReschedule = {},
            onDismissReschedule = {},
            onAcceptReschedule = {},
            onRejectReschedule = {},
        )
    }
}

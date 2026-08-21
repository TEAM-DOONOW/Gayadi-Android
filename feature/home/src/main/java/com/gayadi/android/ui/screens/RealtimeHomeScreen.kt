package com.gayadi.android.ui.screens

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
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
import com.gayadi.android.ui.theme.GayadiTheme
import com.gayadi.android.ui.theme.TextPrimary
import com.gayadi.android.ui.components.ScheduleOptionsBottomSheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RealtimeHomeScreen(
    uiState: RealtimeHomeUiState,
    tripTitle: String,
    travelPlans: List<HomeTravelPlan> = emptyList(),
    tripDays: List<HomeTripDay> = emptyList(),
    participantCount: Int = 0,
    tripStartDate: String = "",
    tripEndDate: String = "",
    tripCountdownText: String = "여행을 준비하고 있어요!",
    tripCoverImageResList: List<Int> = emptyList(),
    kakaoMapJavaScriptKey: String = "",
    kakaoMapBaseUrl: String = "https://localhost",
    friendCharacterKeys: List<String?> = emptyList(),
    onNavigateMyTrip: () -> Unit,
    onNavigateMyPage: () -> Unit,
    onNavigateLedger: () -> Unit = {},
    onNavigatePlaceSearch: () -> Unit,
    onNavigateParticipants: () -> Unit,
    onUpdateSchedule: (scheduleId: String, time: String, memo: String) -> Unit,
    onAddScheduleExpense: (scheduleId: String, time: String, memo: String) -> Unit,
    onScheduleDirections: (scheduleId: String, time: String, memo: String) -> Unit,
    onNavigateRoutes: () -> Unit,
) {
    var selectedPlan by remember { mutableStateOf<HomeTravelPlan?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.0f to Color(0xFFF2FAFF),
                            0.48f to Color(0xFFF8F8FA),
                            1.0f to Color(0xFFFFF7F0),
                        ),
                    ),
                ),
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
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF0B263B),
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "$tripTitle 여행일까지",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            lineHeight = 28.sp,
                        )
                        Text(
                            text = buildAnnotatedString {
                                val remainingDays = Regex("^\\d+일").find(tripCountdownText)?.value
                                if (remainingDays != null) {
                                    withStyle(SpanStyle(color = Color(0xFF10395F))) {
                                        append(remainingDays)
                                    }
                                    append(tripCountdownText.removePrefix(remainingDays))
                                } else {
                                    append(tripCountdownText)
                                }
                            },
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            lineHeight = 28.sp,
                        )
                    }
                    tripCoverImageResList.firstOrNull()?.let { imageRes ->
                        Spacer(modifier = Modifier.width(16.dp))
                        Image(
                            painter = painterResource(imageRes),
                            contentDescription = "$tripTitle 대표 사진",
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop,
                        )
                    }
                }
                Spacer(modifier = Modifier.height(28.dp))

                TravelOverviewCard(
                    progress = calculateTripProgress(tripStartDate, tripEndDate),
                    participantCount = participantCount,
                    myCharacterKey = uiState.profile?.characterKey,
                    friendCharacterKeys = friendCharacterKeys,
                    onParticipants = onNavigateParticipants,
                )

                Spacer(modifier = Modifier.height(22.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Color(0xFFE6E6EA)),
                )
                Spacer(modifier = Modifier.height(22.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(R.drawable.map),
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("여행 동선", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                }
                Spacer(modifier = Modifier.height(10.dp))
                TravelRoutePreview(
                    plans = travelPlans,
                    javaScriptKey = kakaoMapJavaScriptKey,
                    baseUrl = kakaoMapBaseUrl,
                    onClick = onNavigateRoutes,
                )

                Spacer(modifier = Modifier.height(22.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Color(0xFFE6E6EA)),
                )
                Spacer(modifier = Modifier.height(22.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(R.drawable.calendar),
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("여행 계획", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                }
                Spacer(modifier = Modifier.height(14.dp))
                tripDays.forEach { day ->
                    TripDaySection(
                        day = day,
                        plans = travelPlans.filter { it.date == day.date },
                        onAddPlace = onNavigatePlaceSearch,
                        onPlanClick = { selectedPlan = it },
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                }

                Spacer(modifier = Modifier.height(24.dp))
            }

        BottomNavBar(
            currentTab = BottomTab.OUR_TRIP,
            showLedger = true,
            onTabSelected = { tab ->
                when (tab) {
                    BottomTab.MY_TRIP -> onNavigateMyTrip()
                    BottomTab.MY_PAGE -> onNavigateMyPage()
                    BottomTab.LEDGER -> onNavigateLedger()
                    else -> {}
                    }
                },
            )
        }

    }

    selectedPlan?.let { plan ->
        ScheduleOptionsBottomSheet(
            title = plan.title,
            contextText = listOf(tripTitle, plan.date).filter(String::isNotBlank).joinToString(" · "),
            initialTime = plan.time,
            initialMemo = plan.memo,
            heading = "",
            confirmLabel = "수정 완료",
            onAddExpense = { time, memo ->
                selectedPlan = null
                onAddScheduleExpense(plan.id, time, memo)
            },
            onDirections = { time, memo ->
                selectedPlan = null
                onScheduleDirections(plan.id, time, memo)
            },
            onDismiss = { selectedPlan = null },
            onConfirm = { time, memo ->
                onUpdateSchedule(plan.id, time, memo)
                selectedPlan = null
            },
        )
    }
}
@Preview(showBackground = true)
@Composable
private fun RealtimeHomePreview() {
    GayadiTheme {
        RealtimeHomeScreen(
            uiState = RealtimeHomeUiState(),
            tripTitle = "제주 여행",
            onNavigateMyTrip = {},
            onNavigateMyPage = {},
            onNavigatePlaceSearch = {},
            onNavigateParticipants = {},
            onUpdateSchedule = { _, _, _ -> },
            onAddScheduleExpense = { _, _, _ -> },
            onScheduleDirections = { _, _, _ -> },
            onNavigateRoutes = {},
        )
    }
}

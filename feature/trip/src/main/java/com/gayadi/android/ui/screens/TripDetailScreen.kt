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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.gayadi.android.domain.model.TravelParticipant
import com.gayadi.android.domain.model.TravelTrip
import com.gayadi.android.domain.model.TripStatus
import com.gayadi.android.domain.model.UserProfile
import com.gayadi.android.ui.components.UserCharacterAvatar
import com.gayadi.android.ui.theme.PrimaryAction
import com.gayadi.android.ui.theme.PrimaryBlue
import com.gayadi.android.ui.theme.SurfaceCard
import com.gayadi.android.ui.theme.TextPrimary
import com.gayadi.android.ui.theme.TextSecondary

@Composable
fun TripDetailScreen(
    trip: TravelTrip?,
    participants: List<TravelParticipant>,
    profile: UserProfile?,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onStart: () -> Unit,
    onFinish: () -> Unit,
    onParticipants: () -> Unit,
    onInvitation: () -> Unit,
    onSchedule: () -> Unit,
    onRoutes: () -> Unit,
    onHome: () -> Unit,
) {
    var confirmDelete by remember { mutableStateOf(false) }
    if (trip == null) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Text("여행 정보를 찾을 수 없어요", color = TextSecondary)
            TextButton(onClick = onBack) { Text("돌아가기") }
        }
        return
    }

    Column(Modifier.fillMaxSize().background(Color.White).verticalScroll(rememberScrollState())) {
        Row(Modifier.fillMaxWidth().padding(top = 36.dp, start = 8.dp, end = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "뒤로") }
            Column(Modifier.weight(1f)) {
                Text(trip.name, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text("${trip.startDate} - ${trip.endDate}", fontSize = 12.sp, color = TextSecondary)
            }
            UserCharacterAvatar(profile?.characterKey, "${profile?.nickname ?: "여행자"} 캐릭터", Modifier.size(44.dp))
        }

        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("${profile?.nickname ?: "여행자"} 님의 ${trip.cities.joinToString(" · ")} 여행", color = TextSecondary)
            Card(colors = CardDefaults.cardColors(containerColor = SurfaceCard), shape = RoundedCornerShape(14.dp)) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    DetailLine("여행 상태", trip.status.label)
                    DetailLine("참여자", "${participants.size}명")
                }
            }

            TripMenuCard("참여자 관리", "현재 ${participants.size}명", onParticipants)
            TripMenuCard("여행 초대", "초대 코드 생성·수락·거절·취소", onInvitation)
            TripMenuCard("일정 관리", "메인·대체 일정, 순서와 방문 상태", onSchedule)
            TripMenuCard("경로 추천", "출발·여행 동선·귀가 경로", onRoutes)
            TripMenuCard("실시간 여행 홈", "날씨·혼잡도와 다음 일정 확인", onHome)

            when (trip.status) {
                TripStatus.PLANNING -> Button(onClick = onStart, Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = PrimaryAction)) { Text("여행 시작") }
                TripStatus.ONGOING -> Button(onClick = onFinish, Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = PrimaryAction)) { Text("여행 종료") }
                TripStatus.COMPLETED -> Text("완료된 여행입니다", Modifier.fillMaxWidth(), color = PrimaryBlue)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onEdit, modifier = Modifier.weight(1f)) { Text("수정") }
                OutlinedButton(onClick = { confirmDelete = true }, modifier = Modifier.weight(1f)) { Text("삭제", color = Color(0xFFD94B4B)) }
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("여행을 삭제할까요?") },
            text = { Text("초대와 일정도 함께 삭제됩니다.") },
            confirmButton = { TextButton(onClick = { confirmDelete = false; onDelete() }) { Text("삭제") } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("취소") } },
        )
    }
}

@Composable
private fun DetailLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = TextSecondary, fontSize = 13.sp)
        Text(value, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
    }
}

@Composable
private fun TripMenuCard(title: String, subtitle: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE7E7EA)),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            Text(subtitle, fontSize = 12.sp, color = TextSecondary)
        }
    }
}

private val TripStatus.label: String
    get() = when (this) {
        TripStatus.PLANNING -> "여행 전"
        TripStatus.ONGOING -> "여행 중"
        TripStatus.COMPLETED -> "여행 완료"
    }

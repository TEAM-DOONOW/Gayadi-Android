package com.gayadi.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import com.gayadi.android.domain.model.InvitationStatus
import com.gayadi.android.domain.model.TravelInvitation
import com.gayadi.android.domain.model.TravelParticipant
import com.gayadi.android.ui.theme.PrimaryBlue
import com.gayadi.android.ui.theme.SurfaceCard
import com.gayadi.android.ui.theme.TextSecondary

@Composable
fun InvitationScreen(
    tripName: String,
    invitation: TravelInvitation?,
    candidates: List<TravelParticipant>,
    message: String?,
    onBack: () -> Unit,
    onCreate: (String) -> Unit,
    onCopyCode: (String) -> Unit,
    onJoinCode: (String) -> Unit,
    onAccept: (String) -> Unit,
    onDecline: (String) -> Unit,
    onCancel: (String) -> Unit,
) {
    var code by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().background(Color.White).padding(horizontal = 20.dp)) {
        Spacer(Modifier.height(36.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "뒤로") }
            Column {
                Text("여행 초대", fontSize = 21.sp, fontWeight = FontWeight.Bold)
                Text(tripName, fontSize = 12.sp, color = TextSecondary)
            }
        }
        Spacer(Modifier.height(20.dp))
        Text("초대 코드로 참여", fontWeight = FontWeight.SemiBold)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = code,
                onValueChange = { code = it.take(8).uppercase() },
                modifier = Modifier.weight(1f),
                label = { Text("8자리 초대 코드") },
                singleLine = true,
            )
            Button(onClick = { onJoinCode(code) }, enabled = code.length == 8) { Text("참여") }
        }
        message?.let { Text(it, color = PrimaryBlue, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp)) }
        Spacer(Modifier.height(24.dp))

        Text("현재 초대", fontWeight = FontWeight.SemiBold)
        if (invitation == null) {
            Text("아직 생성한 초대가 없어요", color = TextSecondary, fontSize = 13.sp)
            Spacer(Modifier.height(12.dp))
            Text("초대할 여행메이트", color = TextSecondary, fontSize = 12.sp)
            candidates.forEach { candidate ->
                Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(candidate.nickname, Modifier.weight(1f))
                    Button(onClick = { onCreate(candidate.id) }) { Text("초대 생성") }
                }
            }
        } else {
            Card(colors = CardDefaults.cardColors(containerColor = SurfaceCard)) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("초대 코드 ${invitation.code}", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text("참여자 ID ${invitation.inviteeId}", fontSize = 12.sp, color = TextSecondary)
                    Text("상태 · ${invitation.status.label}", color = PrimaryBlue)
                    OutlinedButton(onClick = { onCopyCode(invitation.code) }, Modifier.fillMaxWidth()) { Text("코드 복사") }
                    if (invitation.status == InvitationStatus.PENDING) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { onAccept(invitation.id) }, Modifier.weight(1f)) { Text("수락") }
                            OutlinedButton(onClick = { onDecline(invitation.id) }, Modifier.weight(1f)) { Text("거절") }
                            OutlinedButton(onClick = { onCancel(invitation.id) }, Modifier.weight(1f)) { Text("취소") }
                        }
                    }
                }
            }
        }
    }
}

private val InvitationStatus.label: String
    get() = when (this) {
        InvitationStatus.PENDING -> "대기 중"
        InvitationStatus.ACCEPTED -> "수락됨"
        InvitationStatus.DECLINED -> "거절됨"
        InvitationStatus.CANCELLED -> "취소됨"
    }

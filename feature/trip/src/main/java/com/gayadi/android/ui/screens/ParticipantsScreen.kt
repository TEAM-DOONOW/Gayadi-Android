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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gayadi.android.domain.model.TravelParticipant
import com.gayadi.android.domain.model.UserProfile
import com.gayadi.android.ui.components.UserCharacterAvatar
import com.gayadi.android.ui.components.GayadiTopAppBar
import com.gayadi.android.ui.theme.TextPrimary
import com.gayadi.android.ui.theme.TextSecondary

@Composable
fun ParticipantsScreen(
    tripName: String,
    profile: UserProfile?,
    participants: List<TravelParticipant>,
    candidates: List<TravelParticipant>,
    onBack: () -> Unit,
    onAdd: (String) -> Unit,
    onRemove: (String) -> Unit,
) {
    Column(Modifier.fillMaxSize().background(Color.White)) {
        GayadiTopAppBar(title = "참여자 관리", subtitle = tripName, onBack = onBack) {
            UserCharacterAvatar(profile?.characterKey, "내 캐릭터", Modifier.size(40.dp))
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
            Spacer(Modifier.height(18.dp))
            Text("참여 중 ${participants.size}명", fontWeight = FontWeight.SemiBold)
            participants.forEach { participant ->
                ParticipantRow(participant, action = "내보내기") { onRemove(participant.id) }
            }
            if (participants.isEmpty()) Text("아직 함께하는 참여자가 없어요", color = TextSecondary, fontSize = 13.sp)
            Spacer(Modifier.height(24.dp))
            Text("추가할 수 있는 여행메이트", fontWeight = FontWeight.SemiBold)
            candidates.filterNot { candidate -> participants.any { it.id == candidate.id } }.forEach { candidate ->
                ParticipantRow(candidate, action = "추가") { onAdd(candidate.id) }
            }
        }
    }
}

@Composable
private fun ParticipantRow(participant: TravelParticipant, action: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        UserCharacterAvatar(participant.characterKey, "${participant.nickname} 캐릭터", Modifier.size(42.dp))
        Column(Modifier.weight(1f)) {
            Text(participant.nickname, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            Text("ID ${participant.id}", fontSize = 11.sp, color = TextSecondary)
        }
        if (action == "추가") Button(onClick = onClick) { Text(action) }
        else OutlinedButton(onClick = onClick) { Text(action) }
    }
}

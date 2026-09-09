package com.gayadi.android.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.gayadi.android.domain.repository.*
import com.gayadi.android.domain.model.*
import com.gayadi.android.ui.theme.*

@Composable
fun InvitationPanel(state: InvitationUiState, onSearch: (String) -> Unit,
    onInvite: (FriendshipUser) -> Unit, onCancel: (TravelInvitation) -> Unit, onReload: () -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    val clipboard = LocalClipboardManager.current
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("사용자 지정 초대", style = MaterialTheme.typography.titleLarge)
        if(state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        OutlinedTextField(value = query, onValueChange = { query = it.take(100) }, label = { Text("초대할 닉네임") }, modifier = Modifier.fillMaxWidth())
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { onSearch(query) }, enabled = !state.busy && query.isNotBlank()) { Text("사용자 검색") }
            TextButton(onClick = onReload, enabled = !state.busy) { Text("초대 새로고침") }
        }
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        state.users.forEach { user ->
            OutlinedButton(onClick = { onInvite(user) }, enabled = !state.busy) { Text("${user.nickname}님 초대") }
        }
        state.invitations.forEach { invitation ->
            Text("${invitation.inviteeNickname ?: "초대받은 사용자"} · " + when(invitation.status) {
                InvitationStatus.PENDING -> "대기 중"
                InvitationStatus.ACCEPTED -> "수락됨"
                InvitationStatus.DECLINED -> "거절됨"
                InvitationStatus.CANCELLED -> "취소됨"
            })
            if(invitation.status == InvitationStatus.PENDING) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { clipboard.setText(AnnotatedString(invitation.code)) }) { Text("초대 코드 복사") }
                TextButton(onClick = { onCancel(invitation) }, enabled = !state.busy) { Text("초대 취소") }
            }
        }
    }
}

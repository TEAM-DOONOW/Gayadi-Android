package com.gayadi.android.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gayadi.android.domain.repository.*
import com.gayadi.android.ui.theme.*

@Composable
fun FriendshipPanel(state: FriendshipUiState, onSearch: (String) -> Unit,
    onRequest: (String) -> Unit, onDecide: (Friendship, Boolean) -> Unit,
    onDelete: (Friendship) -> Unit, onReload: () -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    var removing by remember { mutableStateOf<Friendship?>(null) }
    Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("친구", style = MaterialTheme.typography.titleLarge)
        if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        OutlinedTextField(value = query, onValueChange = { query = it.take(100) },
            label = { Text("닉네임으로 친구 검색") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { onSearch(query) }, enabled = !state.busy && query.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryAction)) { Text("검색") }
            OutlinedButton(onClick = onReload, enabled = !state.busy) { Text("새로고침") }
        }
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        if (state.searched && state.results.isEmpty()) Text("검색 결과가 없어요.", color = TextSecondary)
        state.results.forEach { user ->
            val existing = state.friends.firstOrNull { it.user.id == user.id }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(user.nickname, modifier = Modifier.weight(1f))
                if (existing == null) OutlinedButton(onClick = { onRequest(user.id) }, enabled = !state.busy) { Text("친구 요청") }
                else Text("이미 등록된 관계예요", color = TextSecondary)
            }
        }
        if (!state.busy && state.friends.isEmpty()) Text("등록된 친구나 요청이 없어요.", color = TextSecondary)
        state.friends.forEach { friendship ->
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = SurfaceCard)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(friendship.user.nickname, style = MaterialTheme.typography.titleMedium)
                    Text(when(friendship.status) {
                        "ACCEPTED" -> "친구"
                        "PENDING" -> if(friendship.requestedByMe) "보낸 요청" else "받은 요청"
                        "BLOCKED" -> "차단된 관계"
                        else -> "거절된 요청"
                    }, color = TextSecondary)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (friendship.canDecide) {
                            Button(onClick = { onDecide(friendship, true) }, enabled = !state.busy) { Text("수락") }
                            OutlinedButton(onClick = { onDecide(friendship, false) }, enabled = !state.busy) { Text("거절") }
                        }
                        OutlinedButton(onClick = { removing = friendship }, enabled = !state.busy) {
                            Text(if(friendship.status == "PENDING" && friendship.requestedByMe) "요청 취소" else "삭제")
                        }
                    }
                }
            }
        }
    }
    removing?.let { friend ->
        AlertDialog(onDismissRequest = { removing = null }, title = { Text("친구 관계를 삭제할까요?") },
            text = { Text("${friend.user.nickname}님과의 관계 또는 요청을 삭제해요.") },
            confirmButton = { TextButton(onClick = { removing = null; onDelete(friend) }) { Text("삭제") } },
            dismissButton = { TextButton(onClick = { removing = null }) { Text("취소") } })
    }
}

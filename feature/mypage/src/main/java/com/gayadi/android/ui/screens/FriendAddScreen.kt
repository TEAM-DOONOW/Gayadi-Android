package com.gayadi.android.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gayadi.android.ui.theme.GayadiTheme
import com.gayadi.android.ui.theme.PrimaryBlue
import com.gayadi.android.ui.theme.TagBlue
import com.gayadi.android.ui.theme.TagBlueText
import com.gayadi.android.ui.theme.TagGreen
import com.gayadi.android.ui.theme.TagGreenText
import com.gayadi.android.ui.theme.TagOrange
import com.gayadi.android.ui.theme.TagOrangeText
import com.gayadi.android.ui.theme.TextPrimary
import com.gayadi.android.ui.components.GayadiTopAppBar
import com.gayadi.android.ui.theme.TextSecondary
import com.gayadi.android.ui.theme.TextTertiary

private const val INVITE_LINK = "https://gayadi.app/invite/local-trip"

@Composable
fun FriendAddScreen(
    uiState: FriendAddUiState,
    onBack: () -> Unit,
    onQueryChange: (String) -> Unit,
    onFriendCodeChange: (String) -> Unit = {},
    onAddByCode: () -> Unit = {},
    onAddFriend: (String) -> Unit,
    onRetry: () -> Unit,
) {
    val clipboard = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().background(Color.White)) {
        GayadiTopAppBar(title = "함께할 여행메이트", onBack = onBack, showDivider = true)

        FriendCodeCard(
            code = uiState.friendCode,
            message = uiState.codeMessage,
            onCodeChange = onFriendCodeChange,
            onAdd = onAddByCode,
        )

        OutlinedTextField(
            value = uiState.query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
            placeholder = { Text("이름, 아이디 검색") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "친구 검색") },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color(0xFFF5F5F5),
                unfocusedContainerColor = Color(0xFFF5F5F5),
                focusedBorderColor = PrimaryBlue,
                unfocusedBorderColor = Color.Transparent,
            ),
        )

        InviteLinkCard(
            copied = copied,
            onCopy = {
                clipboard.setText(AnnotatedString(INVITE_LINK))
                copied = true
            },
        )

        when {
            uiState.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = PrimaryBlue)
            }
            uiState.errorMessage != null -> Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(uiState.errorMessage, color = TextSecondary)
                Button(onClick = onRetry) { Text("다시 시도") }
            }
            uiState.visibleFriends.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("검색 결과가 없어요", color = TextSecondary)
            }
            else -> LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                item {
                    Text(
                        if (uiState.query.isBlank()) "여행메이트와 추천 친구" else "검색 결과 ${uiState.visibleFriends.size}명",
                        fontSize = 13.sp,
                        color = TextSecondary,
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                }
                items(uiState.visibleFriends, key = FriendItem::id) { friend ->
                    FriendRow(friend = friend, onAdd = { onAddFriend(friend.id) })
                }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}

@Composable
private fun FriendCodeCard(
    code: String,
    message: String?,
    onCodeChange: (String) -> Unit,
    onAdd: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF7F7F9)),
    ) {
        Column(Modifier.padding(18.dp)) {
            Text("친구 초대코드 입력", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            Spacer(Modifier.height(5.dp))
            Text("친구에게 받은 6자리 코드를 입력해 주세요", fontSize = 12.sp, color = TextSecondary)
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = code,
                    onValueChange = onCodeChange,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("예: GAYADI") },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = Color(0xFF343548),
                        unfocusedBorderColor = Color(0xFFE0E1E7),
                    ),
                )
                Button(
                    onClick = onAdd,
                    enabled = code.length == 6,
                    modifier = Modifier.height(56.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF343548)),
                ) { Text("추가") }
            }
            message?.let {
                Text(it, modifier = Modifier.padding(top = 10.dp), fontSize = 12.sp, color = PrimaryBlue)
            }
        }
    }
}

@Composable
private fun InviteLinkCard(copied: Boolean, onCopy: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFEBF5FF)),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Link, contentDescription = null, tint = PrimaryBlue)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("초대 링크 공유", fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Text(if (copied) "초대 링크를 복사했어요" else "링크로 간편하게 초대해요", fontSize = 12.sp, color = TextSecondary)
            }
            Text(
                if (copied) "복사됨" else "복사",
                modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(PrimaryBlue)
                    .clickable(onClick = onCopy).padding(horizontal = 14.dp, vertical = 8.dp),
                fontSize = 13.sp,
                color = Color.White,
            )
        }
    }
}

@Composable
private fun FriendRow(friend: FriendItem, onAdd: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(44.dp).clip(CircleShape).background(Color(0xFFF0F0F0)), contentAlignment = Alignment.Center) {
            Text(friend.emoji, fontSize = 22.sp)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(friend.name, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            Text(friend.handle, fontSize = 12.sp, color = TextSecondary)
        }
        val (label, background, content) = when (friend.status) {
            FriendStatus.TRAVEL_MATE -> Triple("참여 중", TagGreen, TagGreenText)
            FriendStatus.INVITED -> Triple("초대 중", TagOrange, TagOrangeText)
            FriendStatus.RECOMMENDED -> Triple("+ 추가", TagBlue, TagBlueText)
            FriendStatus.ADDED -> Triple("추가됨", TagGreen, TagGreenText)
        }
        Text(
            label,
            modifier = Modifier.clip(RoundedCornerShape(16.dp)).background(background)
                .then(if (friend.status == FriendStatus.RECOMMENDED) Modifier.clickable(onClick = onAdd) else Modifier)
                .padding(horizontal = 10.dp, vertical = 6.dp),
            fontSize = 11.sp,
            color = content,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun FriendAddPreview() {
    GayadiTheme {
        FriendAddScreen(
            uiState = FriendAddUiState(friends = FakeFriendRepository().getFriends().getOrThrow(), isLoading = false),
            onBack = {},
            onQueryChange = {},
            onAddFriend = {},
            onRetry = {},
        )
    }
}

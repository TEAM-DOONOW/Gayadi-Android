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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import com.gayadi.android.ui.components.GayadiCompactTextField
import com.gayadi.android.ui.components.GayadiTopAppBar
import com.gayadi.android.ui.theme.TextSecondary
import com.gayadi.android.ui.theme.TextTertiary

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
    Column(modifier = Modifier.fillMaxSize().background(Color.White)) {
        GayadiTopAppBar(title = "함께할 여행메이트", onBack = onBack, showDivider = true)

        FriendCodeCard(
            code = uiState.friendCode,
            message = uiState.codeMessage,
            onCodeChange = onFriendCodeChange,
            onAdd = onAddByCode,
        )
    }
}

@Composable
private fun FriendCodeCard(
    code: String,
    message: String?,
    onCodeChange: (String) -> Unit,
    onAdd: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 24.dp),
    ) {
            Text("여행 초대코드 입력", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            Spacer(Modifier.height(5.dp))
            Text("여행을 만든 사람에게 받은 6자리 코드를 입력해 주세요", fontSize = 12.sp, color = TextSecondary)
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                GayadiCompactTextField(
                    label = "여행 초대코드",
                    value = code,
                    onValueChange = onCodeChange,
                    modifier = Modifier.weight(1f),
                    placeholder = "예: GAYADI",
                )
                Button(
                    onClick = onAdd,
                    enabled = code.length == 6,
                    modifier = Modifier.height(44.dp),
                    shape = RoundedCornerShape(0.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF343548)),
                ) { Text("추가") }
            }
            message?.let {
                Text(it, modifier = Modifier.padding(top = 10.dp), fontSize = 12.sp, color = Color(0xFF343548))
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

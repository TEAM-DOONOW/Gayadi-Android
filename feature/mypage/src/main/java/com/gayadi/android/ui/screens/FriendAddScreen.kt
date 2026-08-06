package com.gayadi.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.gayadi.android.ui.theme.TagPink
import com.gayadi.android.ui.theme.TagPinkText
import com.gayadi.android.ui.theme.TextPrimary
import com.gayadi.android.ui.theme.TextSecondary
import com.gayadi.android.ui.theme.TextTertiary

private data class FriendItem(
    val emoji: String,
    val name: String,
    val id: String,
    val tag: String,
    val tagBg: Color,
    val tagText: Color,
)

private val travelMates = listOf(
    FriendItem("🐱", "석혁", "@sunghyeok", "방랑", TagPink, TagPinkText),
    FriendItem("🐶", "민수", "@mintsu", "참여 중", TagGreen, TagGreenText),
    FriendItem("🐱", "지은", "@jieun", "초대 중", TagOrange, TagOrangeText),
)

private val recommendedFriends = listOf(
    FriendItem("🐶", "시연", "최근 여행 함께함", "+ 추가", TagBlue, TagBlueText),
)

@Composable
fun FriendAddScreen(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .verticalScroll(rememberScrollState()),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로")
            }
            Text(
                text = "함께할 여행메이트",
                fontSize = 23.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
            )
        }

        androidx.compose.material3.HorizontalDivider(color = Color(0xFFE5E5E5))

        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            SearchBar()

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFEBF5FF)),
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.Link, contentDescription = null, tint = PrimaryBlue)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("초대 링크 공유", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                        Text("링크로 간편하게 여행 메이트를 초대해요", fontSize = 12.sp, color = TextSecondary)
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(PrimaryBlue)
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    ) {
                        Text("복사", fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.Medium)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text("함께하는 여행메이트", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextSecondary)
            Text("3명", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextSecondary,
                modifier = Modifier.align(Alignment.End).padding(bottom = 8.dp))

            travelMates.forEach { friend ->
                FriendRow(friend)
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text("추천 친구", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextSecondary)
            Spacer(modifier = Modifier.height(8.dp))

            recommendedFriends.forEach { friend ->
                FriendRow(friend)
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SearchBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFF5F5F5))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Search, contentDescription = null, tint = TextTertiary, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text("이름, 아이디 검색", fontSize = 14.sp, color = TextTertiary)
    }
}

@Composable
private fun FriendRow(friend: FriendItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Color(0xFFF0F0F0)),
            contentAlignment = Alignment.Center,
        ) {
            Text(friend.emoji, fontSize = 22.sp)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(friend.name, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            Text(friend.id, fontSize = 12.sp, color = TextSecondary)
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(friend.tagBg)
                .padding(horizontal = 10.dp, vertical = 4.dp),
        ) {
            Text(friend.tag, fontSize = 11.sp, color = friend.tagText, fontWeight = FontWeight.Medium)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun FriendAddPreview() {
    GayadiTheme { FriendAddScreen(onBack = {}) }
}

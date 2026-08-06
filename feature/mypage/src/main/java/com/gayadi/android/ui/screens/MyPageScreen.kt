package com.gayadi.android.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.outlined.Settings
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gayadi.android.ui.components.BottomNavBar
import com.gayadi.android.ui.components.BottomTab
import com.gayadi.android.ui.theme.GayadiTheme
import com.gayadi.android.ui.theme.PrimaryBlue
import com.gayadi.android.ui.theme.TextPrimary
import com.gayadi.android.ui.theme.TextSecondary

@Composable
fun MyPageScreen(
    onNavigateHome: () -> Unit,
    onNavigateSettings: () -> Unit,
) {
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

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("마이페이지", fontSize = 23.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                IconButton(onClick = onNavigateSettings) {
                    Icon(Icons.Outlined.Settings, contentDescription = "설정", tint = TextSecondary)
                }
            }

            androidx.compose.material3.HorizontalDivider(color = Color(0xFFE5E5E5))

            Spacer(modifier = Modifier.height(24.dp))

            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF0F0F0)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("🐶", fontSize = 40.sp)
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text("민수", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary)

                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFFEBF5FF))
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                ) {
                    Text("계획 절대 지켜, 꼼꼼밍", fontSize = 13.sp, color = PrimaryBlue, fontWeight = FontWeight.Medium)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FB)),
            ) {
                Row(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "이런점이\n좋아요",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        modifier = Modifier.width(72.dp),
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        listOf(
                            "여행 일정이든 미리 계획을 세워요.",
                            "시간 낭비 없이 효율적으로 여행해요.",
                            "예약, 동선, 준비물을 꼼꼼하게 챙겨요.",
                            "변수까지 고려해 플랜 B도 준비해요.",
                        ).forEach { Text("• $it", fontSize = 12.sp, color = TextSecondary, lineHeight = 20.sp) }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FB)),
            ) {
                Row(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "이런점은\n보완해야해요",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        modifier = Modifier.width(72.dp),
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        listOf(
                            "예상치 못한 상황에 스트레스를 받을 수 있어요.",
                            "계획이 틀어지면 유연하게 대처하기 어려워요.",
                            "즉흥적인 여행의 재미를 놓칠 수 있어요.",
                            "일정에 집착해서 휴식을 잊기 쉬워요.",
                        ).forEach { Text("• $it", fontSize = 12.sp, color = TextSecondary, lineHeight = 20.sp) }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        BottomNavBar(
            currentTab = BottomTab.MY_PAGE,
            showMyPage = true,
            onTabSelected = { tab ->
                if (tab == BottomTab.OUR_TRIP) onNavigateHome()
            },
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MyPagePreview() {
    GayadiTheme { MyPageScreen(onNavigateHome = {}, onNavigateSettings = {}) }
}

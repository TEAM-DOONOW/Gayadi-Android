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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import com.gayadi.android.ui.theme.TagGreen
import com.gayadi.android.ui.theme.TagGreenText
import com.gayadi.android.ui.theme.TextPrimary
import com.gayadi.android.ui.theme.TextSecondary
import com.gayadi.android.ui.theme.TextTertiary

@Composable
fun PlaceDetailScreen(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .verticalScroll(rememberScrollState()),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .background(Color(0xFFE8DDD0)),
            contentAlignment = Alignment.Center,
        ) {
            Text("🍲", fontSize = 64.sp)
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp)
                    .size(36.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color.Black.copy(alpha = 0.3f)),
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로", tint = Color.White)
            }
        }

        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            Spacer(modifier = Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("명진전복", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFFF0F0F0))
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                ) {
                    Text("맛집", fontSize = 11.sp, color = TextSecondary)
                }
                Spacer(modifier = Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(TagGreen)
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    Text("여유", fontSize = 12.sp, color = TagGreenText, fontWeight = FontWeight.Medium)
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text("맛집 · 제주 성산 · 전복 요리", fontSize = 13.sp, color = TextSecondary)

            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("★ 4.6", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("리뷰 1,284", fontSize = 13.sp, color = TextSecondary)
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text("실시간 혼잡도", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            Spacer(modifier = Modifier.height(4.dp))
            Text("지금 방문하기 좋아요 · 예상 대기 5분", fontSize = 12.sp, color = TextSecondary)

            Spacer(modifier = Modifier.height(12.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FB)),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        val hours = listOf("11시", "13시", "15시", "17시")
                        val heights = listOf(0.3f, 0.5f, 0.8f, 0.4f)
                        hours.forEachIndexed { i, hour ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier
                                        .width(32.dp)
                                        .height(60.dp),
                                    contentAlignment = Alignment.BottomCenter,
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(60.dp * heights[i])
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(
                                                if (i == 2) PrimaryBlue else Color(0xFFD0D0D0)
                                            ),
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(hour, fontSize = 10.sp, color = TextTertiary)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("혼잡 가능" to Color(0xFFE8F5E9), "매우 가능" to Color(0xFFFFF3E0), "보통 가능" to Color(0xFFE3F2FD)).forEach { (label, bg) ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(bg)
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                    ) {
                        Text(label, fontSize = 11.sp, color = TextSecondary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("리뷰 1,284", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Text("전체보기", fontSize = 13.sp, color = PrimaryBlue)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FB)),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFFE0E0E0)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("지", fontSize = 14.sp, color = TextSecondary)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("지은", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("★★★★★", fontSize = 12.sp, color = Color(0xFFFFB300))
                        Spacer(modifier = Modifier.weight(1f))
                        Text("2일 전", fontSize = 11.sp, color = TextTertiary)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "활더덕 없이 바로 입장했어요. 전복 신선하고 주차도 넉넉했어요!",
                        fontSize = 13.sp,
                        color = TextSecondary,
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {},
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(0.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
            ) {
                Text("일정에 추가", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PlaceDetailPreview() {
    GayadiTheme { PlaceDetailScreen(onBack = {}) }
}

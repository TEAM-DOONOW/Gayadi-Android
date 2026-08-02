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
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Upload
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gayadi.android.ui.theme.GayadiTheme
import com.gayadi.android.ui.theme.PrimaryBlue
import com.gayadi.android.ui.theme.TagBlue
import com.gayadi.android.ui.theme.TagBlueText
import com.gayadi.android.ui.theme.TagGreen
import com.gayadi.android.ui.theme.TagGreenText
import com.gayadi.android.ui.theme.TagPink
import com.gayadi.android.ui.theme.TagPinkText
import com.gayadi.android.ui.theme.TextPrimary
import com.gayadi.android.ui.theme.TextSecondary

@Composable
fun SurveyResultScreen(onStart: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .verticalScroll(rememberScrollState()),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "나의 여행 캐릭터",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
            )
            Row {
                IconButton(onClick = {}) {
                    Icon(Icons.Outlined.Upload, contentDescription = "공유", tint = TextSecondary)
                }
                IconButton(onClick = {}) {
                    Icon(Icons.Outlined.Share, contentDescription = "공유", tint = TextSecondary)
                }
            }
        }

        Column(
            modifier = Modifier.padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .size(140.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFF5F0E8)),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = "🐶", fontSize = 72.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "인수님의 여행 캐릭터는",
                fontSize = 14.sp,
                color = TextSecondary,
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "계획 절대 지켜, 꼼꼼밍",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = PrimaryBlue,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "한 번 세운 일정은 끝까지 지키는 든든함의 꼼꼼하게 챙기는 여행 스타일",
                fontSize = 13.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TagChip("소확행탐험가", TagPink, TagPinkText)
                TagChip("맛집블로그", TagGreen, TagGreenText)
                TagChip("쇼핑중독자", TagBlue, TagBlueText)
            }

            Spacer(modifier = Modifier.height(24.dp))

            TraitCard(
                title = "이런점이\n좋아요",
                items = listOf(
                    "여행 일정하는 데의 계획을 세워요.",
                    "시간 낭비 없이 효율적으로 여행해요.",
                    "예약, 동선, 준비물을 꼼꼼하게 챙겨요.",
                    "변수까지 고려해 플랜 B도 준비해요.",
                ),
            )

            Spacer(modifier = Modifier.height(12.dp))

            TraitCard(
                title = "이런점은\n보완해야해요",
                items = listOf(
                    "예상치 못한 상황에 스트레스를 받을 수 있어요.",
                    "계획이 틀어지면 유연하게 대처하기 어려워요.",
                    "즉흥적인 여행의 재미를 놓칠 수 있어요.",
                    "일정에 집착해서 휴식을 잊기 쉬워요.",
                ),
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = onStart,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
            ) {
                Text("가야디 시작하기", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun TagChip(text: String, bg: Color, textColor: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text(text = text, fontSize = 12.sp, color = textColor, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun TraitCard(title: String, items: List<String>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FB)),
    ) {
        Row(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                modifier = Modifier.width(72.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                items.forEach { item ->
                    Text(
                        text = "• $item",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        lineHeight = 20.sp,
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SurveyResultPreview() {
    GayadiTheme { SurveyResultScreen(onStart = {}) }
}

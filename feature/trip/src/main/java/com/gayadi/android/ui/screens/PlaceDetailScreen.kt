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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
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
import com.gayadi.android.ui.theme.PrimaryAction
import com.gayadi.android.ui.theme.PrimaryBlue
import com.gayadi.android.ui.theme.TextPrimary
import com.gayadi.android.ui.theme.TextSecondary
import com.gayadi.android.ui.theme.TextTertiary

@Composable
fun PlaceDetailScreen(
    place: PlaceItem?,
    isScheduled: Boolean,
    onBack: () -> Unit,
    onAddToSchedule: () -> Unit,
    isFavorite: Boolean = false,
    onToggleFavorite: () -> Unit = {},
    onNearby: () -> Unit = {},
) {
    if (place == null) {
        Column(
            Modifier.fillMaxSize().background(Color.White),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("장소 정보를 찾을 수 없어요", color = TextSecondary)
            Button(onClick = onBack) { Text("목록으로 돌아가기") }
        }
        return
    }

    Column(Modifier.fillMaxSize().background(Color.White).verticalScroll(rememberScrollState())) {
        Box(Modifier.fillMaxWidth().height(220.dp).background(Color(0xFFE8DDD0)), contentAlignment = Alignment.Center) {
            Text(place.emoji, fontSize = 64.sp)
            IconButton(
                onClick = onBack,
                modifier = Modifier.align(Alignment.TopStart).statusBarsPadding().padding(12.dp).size(40.dp)
                    .clip(RoundedCornerShape(20.dp)).background(Color.Black.copy(alpha = 0.3f)),
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로", tint = Color.White)
            }
        }
        Column(Modifier.padding(horizontal = 20.dp)) {
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(place.name, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(Modifier.width(8.dp))
                Text(place.category, fontSize = 11.sp, color = TextSecondary)
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onToggleFavorite) {
                    Icon(
                        if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = if (isFavorite) "찜 해제" else "찜 추가",
                        tint = if (isFavorite) Color(0xFFE84D6E) else TextSecondary,
                    )
                }
                Text(place.crowdLevel.label, fontSize = 12.sp, color = PrimaryBlue)
            }
            Text(place.description, fontSize = 13.sp, color = TextSecondary)
            Spacer(Modifier.height(8.dp))
            Text("★ ${place.rating} · 리뷰 ${place.reviews}", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(24.dp))
            Text("실시간 혼잡도", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Text("현재 ${place.crowdLevel.label} · 예상 대기 5분", fontSize = 12.sp, color = TextSecondary)
            Spacer(Modifier.height(10.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFEAF4FF)),
            ) {
                Column(Modifier.padding(14.dp)) {
                    Text("현재 날씨", fontWeight = FontWeight.SemiBold)
                    Text("${place.weather} · ${place.temperatureCelsius}℃ · 강수확률 ${place.rainProbability}%", fontSize = 12.sp, color = TextSecondary)
                }
            }
            Spacer(Modifier.height(12.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FB)),
            ) {
                Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                    listOf("11시" to 24, "13시" to 40, "15시" to 58, "17시" to 32).forEach { (hour, height) ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(Modifier.width(30.dp).height(height.dp).background(PrimaryBlue, RoundedCornerShape(4.dp)))
                            Text(hour, fontSize = 10.sp, color = TextTertiary)
                        }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onNearby,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
            ) { Text("주변 장소 보기") }
            Spacer(Modifier.height(10.dp))
            Button(
                onClick = onAddToSchedule,
                enabled = !isScheduled,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(2.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryAction),
            ) {
                Text(if (isScheduled) "일정에 추가됨" else "일정에 추가", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
            if (isScheduled) {
                Text(
                    "선택한 여행 일정에 장소를 추가했어요",
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    color = PrimaryBlue,
                    fontSize = 12.sp,
                )
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PlaceDetailPreview() {
    GayadiTheme {
        PlaceDetailScreen(
            place = FakePlaceRepository().getPlaces().getOrThrow().first(),
            isScheduled = false,
            onBack = {},
            onAddToSchedule = {},
        )
    }
}

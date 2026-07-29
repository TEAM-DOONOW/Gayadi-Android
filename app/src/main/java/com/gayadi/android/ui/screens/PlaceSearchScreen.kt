package com.gayadi.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.gayadi.android.ui.theme.TagRed
import com.gayadi.android.ui.theme.TagRedText
import com.gayadi.android.ui.theme.TextPrimary
import com.gayadi.android.ui.theme.TextSecondary
import com.gayadi.android.ui.theme.TextTertiary

private data class PlaceItem(
    val id: String,
    val name: String,
    val category: String,
    val rating: Double,
    val reviews: Int,
    val crowdTag: String,
    val crowdBg: Color,
    val crowdText: Color,
    val emoji: String,
)

private val categories = listOf("맛집", "카페", "관광명소", "숙소")

private val mockPlaces = listOf(
    PlaceItem("1", "명진전복", "맛집", 4.5, 1284, "여유", TagGreen, TagGreenText, "🍲"),
    PlaceItem("2", "카페 글렌코", "카페", 4.4, 892, "보통", TagOrange, TagOrangeText, "☕"),
    PlaceItem("3", "섭지코지", "관광명소", 4.7, 3561, "혼잡", TagRed, TagRedText, "🏞️"),
    PlaceItem("4", "스테이 성산", "숙소", 4.6, 421, "여유", TagGreen, TagGreenText, "🏨"),
)

@Composable
fun PlaceSearchScreen(
    onBack: () -> Unit,
    onPlaceClick: (String) -> Unit,
) {
    var selectedCategory by remember { mutableStateOf("맛집") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
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
            Text("장소 찾기", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
        }

        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
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
                Text("맛집, 카페, 명소 검색", fontSize = 14.sp, color = TextTertiary)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                categories.forEach { cat ->
                    val isSelected = selectedCategory == cat
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isSelected) PrimaryBlue else Color(0xFFF0F0F0))
                            .clickable { selectedCategory = cat }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    ) {
                        Text(
                            text = cat,
                            fontSize = 13.sp,
                            color = if (isSelected) Color.White else TextSecondary,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("계절 성산 · 12곳", fontSize = 13.sp, color = TextSecondary)
                Text("혼잡도 낮은순 ▾", fontSize = 13.sp, color = TextSecondary)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            modifier = Modifier.padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(mockPlaces) { place ->
                PlaceCard(place = place, onClick = { onPlaceClick(place.id) })
            }
            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun PlaceCard(place: PlaceItem, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, Color(0xFFE8E8E8), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFFF0F0F0)),
            contentAlignment = Alignment.Center,
        ) {
            Text(place.emoji, fontSize = 28.sp)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(place.name, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFFF0F0F0))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                ) {
                    Text(place.category, fontSize = 10.sp, color = TextSecondary)
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text("★ ${place.rating} · 리뷰 ${place.reviews}", fontSize = 12.sp, color = TextSecondary)
            Spacer(modifier = Modifier.height(4.dp))
            Text("차로 6분", fontSize = 11.sp, color = TextTertiary)
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(place.crowdBg)
                .padding(horizontal = 10.dp, vertical = 4.dp),
        ) {
            Text(place.crowdTag, fontSize = 11.sp, color = place.crowdText, fontWeight = FontWeight.Medium)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PlaceSearchPreview() {
    GayadiTheme { PlaceSearchScreen(onBack = {}, onPlaceClick = {}) }
}

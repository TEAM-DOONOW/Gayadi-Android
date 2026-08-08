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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.gayadi.android.ui.theme.TagGreen
import com.gayadi.android.ui.theme.TagGreenText
import com.gayadi.android.ui.theme.TagOrange
import com.gayadi.android.ui.theme.TagOrangeText
import com.gayadi.android.ui.theme.TagRed
import com.gayadi.android.ui.theme.TagRedText
import com.gayadi.android.ui.theme.TextPrimary
import com.gayadi.android.ui.theme.TextSecondary
import com.gayadi.android.ui.theme.TextTertiary

private val placeCategories = listOf("전체", "맛집", "카페", "관광명소", "숙소")

@Composable
fun PlaceSearchScreen(
    uiState: PlaceUiState,
    onBack: () -> Unit,
    onQueryChange: (String) -> Unit,
    onCategorySelected: (String) -> Unit,
    onPlaceClick: (String) -> Unit,
    onRetry: () -> Unit,
    favoritePlaceIds: Set<String> = emptySet(),
    onToggleFavorite: (String) -> Unit = {},
    onNearby: () -> Unit = {},
    onFavorites: () -> Unit = {},
) {
    Column(Modifier.fillMaxSize().background(Color.White)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로")
            }
            Text("장소 찾기", fontSize = 23.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
        }
        androidx.compose.material3.HorizontalDivider(color = Color(0xFFE5E5E5))

        OutlinedTextField(
            value = uiState.query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
            placeholder = { Text("맛집, 카페, 명소 검색") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "장소 검색") },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color(0xFFF5F5F5),
                unfocusedContainerColor = Color(0xFFF5F5F5),
                focusedBorderColor = PrimaryBlue,
                unfocusedBorderColor = Color.Transparent,
            ),
        )

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            placeCategories.forEach { category ->
                val selected = category == uiState.selectedCategory
                Text(
                    category,
                    modifier = Modifier.clip(RoundedCornerShape(18.dp))
                        .background(if (selected) PrimaryBlue else Color(0xFFF0F0F0))
                        .clickable { onCategorySelected(category) }
                        .padding(horizontal = 11.dp, vertical = 7.dp),
                    fontSize = 12.sp,
                    color = if (selected) Color.White else TextSecondary,
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(onClick = onNearby, modifier = Modifier.weight(1f)) { Text("주변 장소") }
            Button(onClick = onFavorites, modifier = Modifier.weight(1f)) { Text("찜 목록") }
        }
        Spacer(Modifier.height(12.dp))

        when {
            uiState.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = PrimaryBlue)
            }
            uiState.errorMessage != null -> Column(
                Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(uiState.errorMessage, color = TextSecondary)
                Button(onClick = onRetry) { Text("다시 시도") }
            }
            uiState.filteredPlaces.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("조건에 맞는 장소가 없어요", color = TextSecondary)
            }
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item { Text("제주 성산 · ${uiState.filteredPlaces.size}곳", fontSize = 13.sp, color = TextSecondary) }
                items(uiState.filteredPlaces, key = PlaceItem::id) { place ->
                    PlaceCard(
                        place,
                        isFavorite = place.id in favoritePlaceIds,
                        onClick = { onPlaceClick(place.id) },
                        onToggleFavorite = { onToggleFavorite(place.id) },
                    )
                }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}

@Composable
private fun PlaceCard(place: PlaceItem, isFavorite: Boolean, onClick: () -> Unit, onToggleFavorite: () -> Unit) {
    val (tagBackground, tagText) = when (place.crowdLevel) {
        CrowdLevel.RELAXED -> TagGreen to TagGreenText
        CrowdLevel.NORMAL -> TagOrange to TagOrangeText
        CrowdLevel.CROWDED -> TagRed to TagRedText
    }
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
            .border(1.dp, Color(0xFFE8E8E8), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(64.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFFF0F0F0)), contentAlignment = Alignment.Center) {
            Text(place.emoji, fontSize = 28.sp)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(place.name, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            Text("${place.category} · ★ ${place.rating} · 리뷰 ${place.reviews}", fontSize = 12.sp, color = TextSecondary)
            Text(place.description, fontSize = 11.sp, color = TextTertiary)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            IconButton(onClick = onToggleFavorite) {
                Icon(
                    if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = if (isFavorite) "찜 해제" else "찜 추가",
                    tint = if (isFavorite) Color(0xFFE84D6E) else TextSecondary,
                )
            }
            Text(
                place.crowdLevel.label,
                modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(tagBackground)
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                fontSize = 11.sp,
                color = tagText,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PlaceSearchPreview() {
    GayadiTheme {
        PlaceSearchScreen(
            uiState = PlaceUiState(places = FakePlaceRepository().getPlaces().getOrThrow(), isLoading = false),
            onBack = {},
            onQueryChange = {},
            onCategorySelected = {},
            onPlaceClick = {},
            onRetry = {},
        )
    }
}

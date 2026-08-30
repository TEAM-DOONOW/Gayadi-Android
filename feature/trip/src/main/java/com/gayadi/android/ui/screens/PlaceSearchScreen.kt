package com.gayadi.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gayadi.android.ui.theme.GayadiTheme
import com.gayadi.android.ui.components.GayadiTopAppBar
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
import coil.compose.AsyncImage

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
    val filterDialogVisible = remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().background(Color.White)) {
        GayadiTopAppBar(title = "장소 찾기", onBack = onBack, showDivider = true)

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            BasicTextField(
                value = uiState.query,
                onValueChange = onQueryChange,
                modifier = Modifier.weight(1f).height(40.dp),
                textStyle = TextStyle(fontSize = 13.sp, color = TextPrimary),
                singleLine = true,
                decorationBox = { innerTextField ->
                    Row(
                        modifier = Modifier.fillMaxSize().background(Color(0xFFF5F5F5)).padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Default.Search, contentDescription = "장소 검색", modifier = Modifier.size(20.dp), tint = TextSecondary)
                        Spacer(Modifier.width(8.dp))
                        Box(Modifier.weight(1f)) {
                            if (uiState.query.isBlank()) {
                                Text("맛집, 카페, 명소 검색", fontSize = 13.sp, color = TextSecondary)
                            }
                            innerTextField()
                        }
                    }
                },
            )
            IconButton(onClick = { filterDialogVisible.value = true }) {
                Icon(Icons.Default.Tune, contentDescription = "장소 필터")
            }
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
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                item {
                    Text(
                        "${uiState.regionName}에서 가볼 만한 곳을 골라봤어요",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        modifier = Modifier.padding(top = 10.dp),
                    )
                }
                items(uiState.filteredPlaces.chunked(2)) { rowPlaces ->
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        rowPlaces.forEach { place ->
                            PlaceCard(
                                place,
                                modifier = Modifier.weight(1f),
                                isFavorite = place.id in favoritePlaceIds,
                                onClick = { onPlaceClick(place.id) },
                                onToggleFavorite = { onToggleFavorite(place.id) },
                            )
                        }
                        if (rowPlaces.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }

    if (filterDialogVisible.value) {
        AlertDialog(
            onDismissRequest = { filterDialogVisible.value = false },
            title = { Text("장소 필터") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    placeCategories.forEach { category ->
                        TextButton(
                            onClick = {
                                onCategorySelected(category)
                                filterDialogVisible.value = false
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                category,
                                color = if (category == uiState.selectedCategory) PrimaryBlue else TextPrimary,
                            )
                        }
                    }
                }
            },
            confirmButton = {},
        )
    }
}

@Composable
private fun PlaceCard(
    place: PlaceItem,
    modifier: Modifier = Modifier,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
) {
    val (tagBackground, tagText) = when (place.crowdLevel) {
        CrowdLevel.RELAXED -> TagGreen to TagGreenText
        CrowdLevel.NORMAL -> TagOrange to TagOrangeText
        CrowdLevel.CROWDED -> TagRed to TagRedText
    }
    Column(modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Box(Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(18.dp)).background(Color(0xFFF0F0F0))) {
            if (place.imageUrl.isNotBlank()) {
                AsyncImage(
                    model = place.imageUrl,
                    contentDescription = "${place.name} 이미지",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Text(place.emoji, fontSize = 28.sp)
            }
            IconButton(onClick = onToggleFavorite, modifier = Modifier.align(Alignment.TopEnd)) {
                Icon(
                    if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = if (isFavorite) "찜 해제" else "찜 추가",
                    tint = if (isFavorite) Color(0xFFE84D6E) else Color.White,
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(place.name, fontWeight = FontWeight.SemiBold, color = TextPrimary, maxLines = 1)
            Text(
                if (place.reviews > 0) "${place.category} · ★ ${place.rating} · 리뷰 ${place.reviews}" else place.category,
                fontSize = 12.sp,
                color = TextSecondary,
            )
            Text(place.description, fontSize = 11.sp, color = TextTertiary, maxLines = 1)
        if (place.hasRealtimeDetails) {
            Text(
                place.crowdLevel.label,
                modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(tagBackground)
                    .padding(horizontal = 8.dp, vertical = 3.dp),
                fontSize = 10.sp,
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
            uiState = PlaceUiState(places = FakePlaceRepository().places().getOrThrow(), isLoading = false),
            onBack = {},
            onQueryChange = {},
            onCategorySelected = {},
            onPlaceClick = {},
            onRetry = {},
        )
    }
}

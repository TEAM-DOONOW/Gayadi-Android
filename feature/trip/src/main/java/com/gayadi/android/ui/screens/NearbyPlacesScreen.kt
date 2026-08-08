package com.gayadi.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gayadi.android.ui.theme.PrimaryBlue
import com.gayadi.android.ui.theme.SurfaceCard
import com.gayadi.android.ui.theme.TextSecondary

@Composable
fun NearbyPlacesScreen(
    places: List<PlaceItem>,
    favoriteIds: Set<String>,
    onBack: () -> Unit,
    onPlaceClick: (String) -> Unit,
    onToggleFavorite: (String) -> Unit,
) {
    Column(Modifier.fillMaxSize().background(Color.White)) {
        Spacer(Modifier.height(36.dp))
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "뒤로") }
            Column {
                Text("주변 장소", fontSize = 21.sp, fontWeight = FontWeight.Bold)
                Text("거리·날씨·혼잡도 기준", fontSize = 12.sp, color = TextSecondary)
            }
        }
        LazyColumn(
            contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(places, key = PlaceItem::id) { place ->
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { onPlaceClick(place.id) },
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(place.emoji, fontSize = 30.sp, modifier = Modifier.size(44.dp))
                        Column(Modifier.weight(1f)) {
                            Text(place.name, fontWeight = FontWeight.SemiBold)
                            Text("${place.distanceMeters}m · ${place.weather} ${place.temperatureCelsius}℃ · 강수 ${place.rainProbability}%", fontSize = 11.sp, color = TextSecondary)
                            Text("혼잡도 ${place.crowdLevel.label}", fontSize = 11.sp, color = PrimaryBlue)
                        }
                        IconButton(onClick = { onToggleFavorite(place.id) }) {
                            Icon(
                                if (place.id in favoriteIds) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                if (place.id in favoriteIds) "찜 해제" else "찜하기",
                                tint = if (place.id in favoriteIds) Color(0xFFE84D6E) else TextSecondary,
                            )
                        }
                    }
                }
            }
        }
    }
}

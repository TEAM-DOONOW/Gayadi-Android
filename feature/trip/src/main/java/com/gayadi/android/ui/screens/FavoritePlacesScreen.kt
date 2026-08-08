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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.gayadi.android.ui.theme.TextSecondary

@Composable
fun FavoritePlacesScreen(
    places: List<PlaceItem>,
    onBack: () -> Unit,
    onPlaceClick: (String) -> Unit,
    onToggleFavorite: (String) -> Unit,
) {
    Column(Modifier.fillMaxSize().background(Color.White)) {
        Spacer(Modifier.height(36.dp))
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "뒤로") }
            Text("찜한 장소", fontSize = 21.sp, fontWeight = FontWeight.Bold)
        }
        if (places.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("아직 찜한 장소가 없어요", color = TextSecondary) }
        } else {
            LazyColumn(contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(places, key = PlaceItem::id) { place ->
                    FavoritePlaceRow(place, onPlaceClick, onToggleFavorite)
                }
            }
        }
    }
}

@Composable
private fun FavoritePlaceRow(place: PlaceItem, onPlaceClick: (String) -> Unit, onToggleFavorite: (String) -> Unit) {
    androidx.compose.material3.Card(onClick = { onPlaceClick(place.id) }, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(place.emoji, fontSize = 28.sp)
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Text(place.name, fontWeight = FontWeight.SemiBold)
                Text("${place.category} · ${place.weather} · ${place.crowdLevel.label}", fontSize = 11.sp, color = TextSecondary)
            }
            IconButton(onClick = { onToggleFavorite(place.id) }) { Text("♥", color = Color(0xFFE84D6E), fontSize = 20.sp) }
        }
    }
}

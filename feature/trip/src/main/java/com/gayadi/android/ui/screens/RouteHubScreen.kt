package com.gayadi.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import com.gayadi.android.ui.theme.SurfaceCard
import com.gayadi.android.ui.theme.TextSecondary
import com.gayadi.android.ui.components.GayadiTopAppBar

@Composable
fun RouteHubScreen(tripName: String, onBack: () -> Unit, onSelect: (RouteRecommendationType) -> Unit) {
    Column(Modifier.fillMaxSize().background(Color.White)) {
        GayadiTopAppBar(title = "경로 추천", subtitle = tripName, onBack = onBack)
        Column(Modifier.padding(horizontal = 20.dp)) {
            Spacer(Modifier.height(22.dp))
            RouteRecommendationType.entries.forEach { type ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp).clickable { onSelect(type) },
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(type.hubTitle, fontWeight = FontWeight.SemiBold)
                        Text(type.hubDescription, color = TextSecondary, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

private val RouteRecommendationType.hubTitle: String
    get() = when (this) {
        RouteRecommendationType.DEPARTURE -> "출발 경로 추천"
        RouteRecommendationType.ITINERARY -> "여행 동선 추천"
        RouteRecommendationType.HOME -> "귀가 경로 추천"
    }

private val RouteRecommendationType.hubDescription: String
    get() = when (this) {
        RouteRecommendationType.DEPARTURE -> "집에서 여행지까지 빠르고 편안한 경로"
        RouteRecommendationType.ITINERARY -> "일정 순서와 혼잡도를 반영한 하루 동선"
        RouteRecommendationType.HOME -> "마지막 일정에서 집까지 여유로운 귀가 경로"
    }

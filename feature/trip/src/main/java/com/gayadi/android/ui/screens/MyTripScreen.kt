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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Luggage
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gayadi.android.ui.theme.GayadiTheme
import com.gayadi.android.ui.theme.PretendardFontFamily
import com.gayadi.android.ui.theme.PretendardSemiBoldFontFamily
import com.gayadi.android.ui.theme.TextPrimary
import com.gayadi.android.ui.theme.TextSecondary

data class TripSummary(
    val name: String,
    val startDate: String,
    val endDate: String,
)

@Composable
fun MyTripScreen(
    trips: List<TripSummary>,
    onAddTrip: () -> Unit,
    onNavigateHome: () -> Unit,
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFAFAFB))
            .padding(horizontal = 20.dp),
    ) {
        Spacer(modifier = Modifier.height(48.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "나의 여행",
                fontFamily = PretendardSemiBoldFontFamily,
                fontSize = 22.sp,
                color = TextPrimary,
            )
            IconButton(onClick = { }) {
                Icon(Icons.Filled.Settings, contentDescription = "설정", tint = Color(0xFF505466))
            }
        }

        Spacer(modifier = Modifier.height(18.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            TripTab("진행 중인 여행", selectedTab == 0) { selectedTab = 0 }
            TripTab("지나간 여행", selectedTab == 1) { selectedTab = 1 }
        }
        Spacer(modifier = Modifier.height(12.dp))

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            if (trips.isEmpty()) {
                EmptyTrips(modifier = Modifier.align(Alignment.Center))
            } else if (selectedTab == 0) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    trips.forEach { trip -> TripCard(trip = trip, onClick = onNavigateHome) }
                }
            } else {
                EmptyTrips(
                    title = "아직 지나간 여행이 없어요",
                    message = "완료된 여행은 이곳에 차곡차곡 모아둘게요.",
                    modifier = Modifier.align(Alignment.Center),
                )
            }
        }

        Button(
            onClick = onAddTrip,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(2.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF343548)),
        ) {
            Text("여행 추가하기", fontFamily = PretendardSemiBoldFontFamily, fontSize = 15.sp)
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun TripTab(text: String, selected: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier.clickable(onClick = onClick).padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = text,
            fontFamily = if (selected) PretendardSemiBoldFontFamily else PretendardFontFamily,
            fontSize = 14.sp,
            color = if (selected) TextPrimary else Color(0xFFA7A8AF),
        )
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .height(2.dp)
                .fillMaxWidth()
                .background(if (selected) Color(0xFF343548) else Color.Transparent),
        )
    }
}

@Composable
private fun EmptyTrips(
    title: String = "아직 만든 여행이 없어요",
    message: String = "여행 추가하기를 눌러 첫 여행을 만들어 보세요.",
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.padding(bottom = 36.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = Icons.Outlined.Luggage,
            contentDescription = null,
            modifier = Modifier.size(52.dp),
            tint = Color(0xFFB7B8C0),
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(title, fontFamily = PretendardSemiBoldFontFamily, fontSize = 17.sp, color = TextPrimary)
        Spacer(modifier = Modifier.height(6.dp))
        Text(message, fontFamily = PretendardFontFamily, fontSize = 13.sp, color = TextSecondary)
    }
}

@Composable
private fun TripCard(trip: TripSummary, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().border(1.dp, Color(0xFFE7E7EA), RoundedCornerShape(8.dp)).padding(14.dp),
        ) {
            Text(trip.name, fontFamily = PretendardSemiBoldFontFamily, fontSize = 15.sp, color = TextPrimary)
            Spacer(modifier = Modifier.height(5.dp))
            Text(
                "${trip.startDate} - ${trip.endDate}",
                fontFamily = PretendardFontFamily,
                fontSize = 12.sp,
                color = Color(0xFF9A9BA2),
            )
        }
    }
}

@Preview(showBackground = true, heightDp = 800)
@Composable
private fun MyTripPreview() {
    GayadiTheme { MyTripScreen(trips = emptyList(), onAddTrip = {}, onNavigateHome = {}) }
}

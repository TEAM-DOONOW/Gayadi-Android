package com.gayadi.android.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gayadi.android.ui.theme.PrimaryAction
import com.gayadi.android.ui.theme.PrimaryBlue
import com.gayadi.android.ui.theme.TextPrimary
import com.gayadi.android.ui.theme.TextSecondary

@Composable
internal fun TripDaySection(
    day: HomeTripDay,
    plans: List<HomeTravelPlan>,
    onAddPlace: () -> Unit,
    onPlanClick: (HomeTravelPlan) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "DAY ${day.dayNumber}",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = day.dateLabel,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = TextSecondary,
        )
    }
    if (plans.isNotEmpty()) {
        Spacer(modifier = Modifier.height(12.dp))
        plans.forEachIndexed { index, plan ->
            TravelPlanRow(index = index + 1, plan = plan, onClick = { onPlanClick(plan) })
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
    Spacer(modifier = Modifier.height(24.dp))
    OutlinedButton(
        onClick = onAddPlace,
        modifier = Modifier
            .fillMaxWidth()
            .height(34.dp),
        shape = RoundedCornerShape(0.dp),
        border = BorderStroke(1.dp, PrimaryAction),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Color.White,
            contentColor = PrimaryAction,
        ),
    ) {
        Text("장소 추가", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun TravelPlanRow(
    index: Int,
    plan: HomeTravelPlan,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick),
        shape = RoundedCornerShape(0.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE8E8EC)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RouteDot(index.toString(), if (plan.isVisited) PrimaryBlue else PrimaryAction)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(plan.title, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Text("${plan.date}  ${plan.time}", fontSize = 12.sp, color = TextSecondary)
            }
            Text(
                if (plan.isVisited) "완료" else "예정",
                fontSize = 11.sp,
                color = if (plan.isVisited) PrimaryBlue else TextSecondary,
            )
        }
    }
}

@Composable
private fun RouteDot(number: String, color: Color) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .background(color = color, shape = CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(number, fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.Bold)
    }
}

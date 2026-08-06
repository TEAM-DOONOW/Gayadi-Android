package com.gayadi.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import com.gayadi.android.ui.components.BottomNavBar
import com.gayadi.android.ui.components.BottomTab
import com.gayadi.android.ui.theme.GayadiTheme
import com.gayadi.android.ui.theme.PrimaryBlue
import com.gayadi.android.ui.theme.TagOrange
import com.gayadi.android.ui.theme.TagOrangeText
import com.gayadi.android.ui.theme.TagRed
import com.gayadi.android.ui.theme.TagRedText
import com.gayadi.android.ui.theme.TextPrimary
import com.gayadi.android.ui.theme.TextSecondary
import com.gayadi.android.ui.theme.TextTertiary

private data class ChecklistItem(
    val title: String,
    val date: String,
    val tag: String?,
    val tagBg: Color?,
    val tagText: Color?,
    var done: Boolean = false,
)

@Composable
fun MyTripScreen(onNavigateHome: () -> Unit) {
    var showDone by remember { mutableStateOf(false) }

    val todoItems = remember {
        listOf(
            ChecklistItem("숙소 예약하기", "2026.07.18", "나", TagOrange, TagOrangeText),
            ChecklistItem("렌터카 확인", "2026.07.18", "민수", TagRed, TagRedText),
            ChecklistItem("준비물 챙기기", "2026.07.18", "지은", TagRed, TagRedText),
            ChecklistItem("환전하기", "", "공동", null, null),
        ).toMutableList()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            Spacer(modifier = Modifier.height(56.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text("민수님,", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text("아직 안까먹으셨죠?!", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                }
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF0F0F0)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("🐶", fontSize = 24.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1)),
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("두나우들이랑 가게", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                        Row {
                            Text("여행일까지 ", fontSize = 13.sp, color = TextSecondary)
                            Text("10일", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE53935))
                            Text(" 남았어요 !", fontSize = 13.sp, color = TextSecondary)
                        }
                    }
                    Text("🧳", fontSize = 36.sp)
                }
                Text(
                    "📅 2026.07.28 ~ 2026.07.30",
                    fontSize = 11.sp,
                    color = TextTertiary,
                    modifier = Modifier.padding(start = 16.dp, bottom = 12.dp),
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                TabText("해야 해요", !showDone) { showDone = false }
                TabText("완료했어요", showDone) { showDone = true }
            }

            Spacer(modifier = Modifier.height(16.dp))

            todoItems.forEachIndexed { index, item ->
                ChecklistRow(
                    item = item,
                    onToggle = { todoItems[index] = item.copy(done = !item.done) },
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(PrimaryBlue)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("준비 추가", fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.Medium)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        BottomNavBar(
            currentTab = BottomTab.MY_TRIP,
            onTabSelected = { tab ->
                if (tab == BottomTab.OUR_TRIP) onNavigateHome()
            },
        )
    }
}

@Composable
private fun TabText(text: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        text = text,
        fontSize = 15.sp,
        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
        color = if (selected) TextPrimary else TextTertiary,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) Color.Transparent else Color.Transparent)
            .padding(vertical = 4.dp)
            .then(Modifier)
            .let { mod ->
                mod
            },
    )
    if (selected) {
        Box(
            modifier = Modifier
                .height(2.dp)
                .fillMaxWidth(0.3f)
                .background(TextPrimary),
        )
    }
}

@Composable
private fun ChecklistRow(item: ChecklistItem, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onToggle, modifier = Modifier.size(28.dp)) {
            Icon(
                imageVector = if (item.done) Icons.Filled.CheckCircle else Icons.Outlined.CheckCircle,
                contentDescription = null,
                tint = if (item.done) PrimaryBlue else Color(0xFFD0D0D0),
                modifier = Modifier.size(24.dp),
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                fontSize = 15.sp,
                color = if (item.done) TextTertiary else TextPrimary,
                fontWeight = FontWeight.Medium,
            )
            if (item.tag != null && item.tagBg != null && item.tagText != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(item.tagBg)
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                ) {
                    Text(item.tag, fontSize = 10.sp, color = item.tagText)
                }
            }
        }
        Text(item.date, fontSize = 12.sp, color = TextTertiary)
    }
}

@Preview(showBackground = true)
@Composable
private fun MyTripPreview() {
    GayadiTheme { MyTripScreen(onNavigateHome = {}) }
}

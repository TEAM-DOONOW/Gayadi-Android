package com.gayadi.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gayadi.android.domain.model.ScheduleType
import com.gayadi.android.domain.model.TravelSchedule
import com.gayadi.android.ui.theme.PrimaryBlue
import com.gayadi.android.ui.theme.SurfaceCard
import com.gayadi.android.ui.theme.TextSecondary
import java.util.UUID

@Composable
fun ScheduleScreen(
    tripId: String,
    tripName: String,
    defaultDate: String,
    schedules: List<TravelSchedule>,
    onBack: () -> Unit,
    onSave: (TravelSchedule) -> Unit,
    onDelete: (String) -> Unit,
    onMove: (String, Int) -> Unit,
    onToggleVisited: (String) -> Unit,
    onRecommendRoute: () -> Unit,
) {
    var editing by remember { mutableStateOf<TravelSchedule?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().background(Color.White)) {
        Spacer(Modifier.height(36.dp))
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "뒤로") }
            Column(Modifier.weight(1f)) {
                Text("일정 관리", fontSize = 21.sp, fontWeight = FontWeight.Bold)
                Text(tripName, fontSize = 12.sp, color = TextSecondary)
            }
            Button(onClick = { editing = null; showEditor = true }) { Text("일정 추가") }
        }
        if (schedules.isEmpty()) {
            Column(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                Text("아직 일정이 없어요", fontWeight = FontWeight.SemiBold)
                Text("메인 일정이나 대체 일정을 추가해 보세요", color = TextSecondary, fontSize = 12.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(schedules, key = TravelSchedule::id) { schedule ->
                    ScheduleCard(
                        schedule = schedule,
                        onEdit = { editing = schedule; showEditor = true },
                        onDelete = { onDelete(schedule.id) },
                        onUp = { onMove(schedule.id, -1) },
                        onDown = { onMove(schedule.id, 1) },
                        onToggleVisited = { onToggleVisited(schedule.id) },
                    )
                }
            }
        }
        OutlinedButton(onClick = onRecommendRoute, Modifier.fillMaxWidth().padding(horizontal = 20.dp)) { Text("여행 동선 추천") }
        Spacer(Modifier.height(16.dp))
    }
    if (showEditor) {
        ScheduleEditor(
            initial = editing,
            defaultDate = defaultDate,
            onDismiss = { showEditor = false },
            onSave = { title, date, time, type ->
                onSave(
                    TravelSchedule(
                        id = editing?.id ?: UUID.randomUUID().toString(),
                        tripId = tripId,
                        title = title,
                        placeId = editing?.placeId,
                        date = date,
                        time = time,
                        type = type,
                        order = editing?.order ?: schedules.size,
                        isVisited = editing?.isVisited ?: false,
                    ),
                )
                showEditor = false
            },
        )
    }
}

@Composable
private fun ScheduleCard(
    schedule: TravelSchedule,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onUp: () -> Unit,
    onDown: () -> Unit,
    onToggleVisited: () -> Unit,
) {
    Card(colors = CardDefaults.cardColors(containerColor = SurfaceCard)) {
        Column(Modifier.fillMaxWidth().padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = schedule.isVisited, onCheckedChange = { onToggleVisited() })
                Column(Modifier.weight(1f)) {
                    Text(schedule.title, fontWeight = FontWeight.SemiBold)
                    Text("${schedule.date} ${schedule.time} · ${if (schedule.type == ScheduleType.MAIN) "메인" else "대체"}", fontSize = 11.sp, color = TextSecondary)
                }
                IconButton(onClick = onUp) { Icon(Icons.Default.ArrowUpward, "위로") }
                IconButton(onClick = onDown) { Icon(Icons.Default.ArrowDownward, "아래로") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onEdit) { Text("수정") }
                TextButton(onClick = onDelete) { Text("삭제", color = Color(0xFFD94B4B)) }
                if (schedule.isVisited) Text("방문 완료", color = PrimaryBlue, modifier = Modifier.padding(top = 12.dp))
            }
        }
    }
}

@Composable
private fun ScheduleEditor(
    initial: TravelSchedule?,
    defaultDate: String,
    onDismiss: () -> Unit,
    onSave: (String, String, String, ScheduleType) -> Unit,
) {
    var title by remember(initial?.id) { mutableStateOf(initial?.title.orEmpty()) }
    var date by remember(initial?.id) { mutableStateOf(initial?.date ?: defaultDate) }
    var time by remember(initial?.id) { mutableStateOf(initial?.time ?: "10:00") }
    var type by remember(initial?.id) { mutableStateOf(initial?.type ?: ScheduleType.MAIN) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "일정 추가" else "일정 수정") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(title, { title = it.take(30) }, label = { Text("일정 이름") })
                OutlinedTextField(date, { date = it.take(10) }, label = { Text("날짜 (yyyy.MM.dd)") })
                OutlinedTextField(time, { time = it.take(5) }, label = { Text("시간 (HH:mm)") })
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ScheduleType.entries.forEach { candidate ->
                        FilterChip(
                            selected = type == candidate,
                            onClick = { type = candidate },
                            label = { Text(if (candidate == ScheduleType.MAIN) "메인 일정" else "대체 일정") },
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onSave(title.trim(), date, time, type) }, enabled = title.isNotBlank() && date.isNotBlank() && time.isNotBlank()) { Text("저장") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("취소") } },
    )
}

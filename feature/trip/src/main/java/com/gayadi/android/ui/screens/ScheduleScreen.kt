package com.gayadi.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gayadi.android.domain.model.ScheduleType
import com.gayadi.android.domain.model.TravelSchedule
import com.gayadi.android.ui.theme.PrimaryAction
import com.gayadi.android.ui.components.GayadiTopAppBar
import com.gayadi.android.ui.theme.PrimaryBlue
import com.gayadi.android.ui.theme.SurfaceCard
import com.gayadi.android.ui.theme.TextPrimary
import com.gayadi.android.ui.theme.TextSecondary
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.format.ResolverStyle
import java.util.UUID

private val scheduleDateFormatter = DateTimeFormatter.ofPattern("uuuu.MM.dd")
    .withResolverStyle(ResolverStyle.STRICT)
private val scheduleTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    .withResolverStyle(ResolverStyle.STRICT)
private val ScheduleDanger = Color(0xFFD94B4B)

@OptIn(ExperimentalMaterial3Api::class)
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
    expenseCountsBySchedule: Map<String, Int> = emptyMap(),
    selectedScheduleId: String? = null,
    onConsumeSelectedSchedule: () -> Unit = {},
    onAddExpense: (String) -> Unit = {},
    onLedger: () -> Unit = {},
) {
    var editingScheduleId by rememberSaveable { mutableStateOf<String?>(null) }
    var showEditor by rememberSaveable { mutableStateOf(false) }
    var detailScheduleId by rememberSaveable { mutableStateOf<String?>(null) }
    var deletingScheduleId by rememberSaveable { mutableStateOf<String?>(null) }
    val editing = schedules.firstOrNull { it.id == editingScheduleId }
    val detailSchedule = schedules.firstOrNull { it.id == detailScheduleId }
    val deletingSchedule = schedules.firstOrNull { it.id == deletingScheduleId }

    LaunchedEffect(selectedScheduleId, schedules) {
        if (selectedScheduleId != null && schedules.any { it.id == selectedScheduleId }) {
            detailScheduleId = selectedScheduleId
            onConsumeSelectedSchedule()
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Color.White)
            .navigationBarsPadding(),
    ) {
        GayadiTopAppBar(title = "일정 관리", subtitle = tripName, onBack = onBack) {
            Button(
                onClick = {
                    editingScheduleId = null
                    showEditor = true
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryAction),
            ) { Text("일정 추가") }
        }
        if (schedules.isEmpty()) {
            Column(
                Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("아직 일정이 없어요", fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Text("메인 일정이나 대체 일정을 추가해 보세요", color = TextSecondary, fontSize = 12.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(schedules, key = TravelSchedule::id) { schedule ->
                    ScheduleCard(
                        schedule = schedule,
                        onOpen = { detailScheduleId = schedule.id },
                        onEdit = {
                            editingScheduleId = schedule.id
                            showEditor = true
                        },
                        onDelete = { deletingScheduleId = schedule.id },
                        onUp = { onMove(schedule.id, -1) },
                        onDown = { onMove(schedule.id, 1) },
                        onToggleVisited = { onToggleVisited(schedule.id) },
                        onAddExpense = { onAddExpense(schedule.id) },
                    )
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(onClick = onLedger, modifier = Modifier.weight(1f)) { Text("여행 가계부") }
            OutlinedButton(onClick = onRecommendRoute, modifier = Modifier.weight(1f)) { Text("여행 동선 추천") }
        }
        Spacer(Modifier.height(16.dp))
    }

    if (showEditor) {
        ScheduleEditor(
            initial = editing,
            defaultDate = defaultDate,
            onDismiss = { showEditor = false },
            onSave = { title, date, startTime, endTime, type ->
                onSave(
                    TravelSchedule(
                        id = editing?.id ?: UUID.randomUUID().toString(),
                        tripId = tripId,
                        title = title,
                        placeId = editing?.placeId,
                        date = date,
                        time = startTime,
                        endTime = endTime,
                        type = type,
                        order = editing?.order ?: schedules.size,
                        isVisited = editing?.isVisited ?: false,
                    ),
                )
                showEditor = false
            },
        )
    }

    detailSchedule?.let { schedule ->
        ScheduleDetailSheet(
            schedule = schedule,
            onDismiss = { detailScheduleId = null },
            onEdit = {
                detailScheduleId = null
                editingScheduleId = schedule.id
                showEditor = true
            },
            onAddExpense = {
                detailScheduleId = null
                onAddExpense(schedule.id)
            },
        )
    }

    deletingSchedule?.let { schedule ->
        val linkedExpenseCount = expenseCountsBySchedule[schedule.id] ?: 0
        AlertDialog(
            onDismissRequest = { deletingScheduleId = null },
            title = { Text("일정을 삭제할까요?") },
            text = {
                Text(
                    if (linkedExpenseCount > 0) {
                        "이 일정과 연결된 비용 ${linkedExpenseCount}건도 함께 삭제되며 되돌릴 수 없어요."
                    } else {
                        "삭제한 일정은 되돌릴 수 없어요."
                    },
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        deletingScheduleId = null
                        onDelete(schedule.id)
                    },
                ) { Text("일정 삭제", color = ScheduleDanger) }
            },
            dismissButton = {
                TextButton(onClick = { deletingScheduleId = null }) { Text("취소") }
            },
        )
    }
}

@Composable
private fun ScheduleCard(
    schedule: TravelSchedule,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onUp: () -> Unit,
    onDown: () -> Unit,
    onToggleVisited: () -> Unit,
    onAddExpense: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = schedule.isVisited,
                    onCheckedChange = { onToggleVisited() },
                )
                Column(Modifier.weight(1f)) {
                    Text(schedule.title, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    Text(
                        schedule.displayDateTime,
                        fontSize = 12.sp,
                        color = TextSecondary,
                    )
                    Text(
                        if (schedule.type == ScheduleType.MAIN) "메인 일정" else "대체 일정",
                        fontSize = 11.sp,
                        color = if (schedule.type == ScheduleType.MAIN) PrimaryBlue else TextSecondary,
                    )
                }
                IconButton(onClick = onUp) { Icon(Icons.Default.ArrowUpward, "위로") }
                IconButton(onClick = onDown) { Icon(Icons.Default.ArrowDownward, "아래로") }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onEdit) { Text("수정") }
                TextButton(onClick = onDelete) { Text("삭제", color = ScheduleDanger) }
                if (schedule.isVisited) {
                    Text("방문 완료", color = PrimaryBlue, fontSize = 12.sp)
                }
                Spacer(Modifier.weight(1f))
                OutlinedButton(onClick = onAddExpense) { Text("비용 추가") }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScheduleDetailSheet(
    schedule: TravelSchedule,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onAddExpense: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("일정 상세", fontSize = 13.sp, color = TextSecondary)
            Text(schedule.title, fontSize = 21.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Text(schedule.displayDateTime, color = TextSecondary)
            Text(
                if (schedule.type == ScheduleType.MAIN) "메인 일정" else "대체 일정",
                color = PrimaryBlue,
                fontWeight = FontWeight.Medium,
            )
            if (schedule.endTime == null) {
                Text(
                    "종료 시각을 설정하면 일정이 끝날 때 비용 입력 알림을 받을 수 있어요.",
                    fontSize = 12.sp,
                    color = ScheduleDanger,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedButton(onClick = onEdit, modifier = Modifier.weight(1f)) { Text("일정 수정") }
                Button(
                    onClick = onAddExpense,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryAction),
                ) { Text("비용 추가") }
            }
        }
    }
}

@Composable
private fun ScheduleEditor(
    initial: TravelSchedule?,
    defaultDate: String,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String?, ScheduleType) -> Unit,
) {
    var title by rememberSaveable(initial?.id) { mutableStateOf(initial?.title.orEmpty()) }
    var date by rememberSaveable(initial?.id) { mutableStateOf(initial?.date ?: defaultDate) }
    var startTime by rememberSaveable(initial?.id) { mutableStateOf(initial?.time ?: "10:00") }
    var endTime by rememberSaveable(initial?.id) {
        mutableStateOf(initial?.endTime ?: if (initial == null) "11:00" else "")
    }
    var typeName by rememberSaveable(initial?.id) {
        mutableStateOf((initial?.type ?: ScheduleType.MAIN).name)
    }
    var submitted by rememberSaveable(initial?.id) { mutableStateOf(false) }
    val type = ScheduleType.valueOf(typeName)

    val titleError = if (title.isBlank()) "일정 이름을 입력해 주세요" else null
    val dateError = dateValidationMessage(date)
    val startTimeError = timeValidationMessage(startTime, "시작")
    val endTimeError = if (endTime.isBlank()) null else timeValidationMessage(endTime, "종료")
    val rangeError = if (
        endTime.isNotBlank() &&
        startTimeError == null &&
        endTimeError == null &&
        !isValidTimeRange(startTime, endTime)
    ) {
        "종료 시간은 시작 시간보다 뒤여야 해요"
    } else {
        null
    }
    val isValid = listOf(titleError, dateError, startTimeError, endTimeError, rangeError).all { it == null }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "일정 추가" else "일정 수정") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it.take(30) },
                    label = { Text("일정 이름") },
                    singleLine = true,
                    isError = submitted && titleError != null,
                    supportingText = errorSupportingText(submitted, titleError),
                )
                OutlinedTextField(
                    value = date,
                    onValueChange = { date = it.take(10) },
                    label = { Text("날짜 (yyyy.MM.dd)") },
                    singleLine = true,
                    isError = submitted && dateError != null,
                    supportingText = errorSupportingText(submitted, dateError),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = startTime,
                        onValueChange = { startTime = it.take(5) },
                        label = { Text("시작 (HH:mm)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        isError = submitted && startTimeError != null,
                        supportingText = errorSupportingText(submitted, startTimeError),
                    )
                    OutlinedTextField(
                        value = endTime,
                        onValueChange = { endTime = it.take(5) },
                        label = { Text("종료 (선택)") },
                        placeholder = { Text("HH:mm") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        isError = submitted && (endTimeError != null || rangeError != null),
                        supportingText = errorSupportingText(submitted, endTimeError ?: rangeError),
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ScheduleType.entries.forEach { candidate ->
                        FilterChip(
                            selected = type == candidate,
                            onClick = { typeName = candidate.name },
                            label = { Text(if (candidate == ScheduleType.MAIN) "메인 일정" else "대체 일정") },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    submitted = true
                    if (isValid) {
                        onSave(title.trim(), date, startTime, endTime.trim().takeIf(String::isNotBlank), type)
                    }
                },
            ) { Text("저장") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("취소") } },
    )
}

private val TravelSchedule.displayDateTime: String
    get() = if (endTime.isNullOrBlank()) {
        "$date $time · 종료 시각 미설정"
    } else {
        "$date $time–$endTime"
    }

private fun dateValidationMessage(value: String): String? = when {
    value.isBlank() -> "날짜를 입력해 주세요"
    else -> try {
        LocalDate.parse(value, scheduleDateFormatter)
        null
    } catch (_: DateTimeParseException) {
        "날짜를 yyyy.MM.dd 형식으로 입력해 주세요"
    }
}

private fun timeValidationMessage(value: String, label: String): String? = when {
    value.isBlank() -> "$label 시간을 입력해 주세요"
    else -> try {
        LocalTime.parse(value, scheduleTimeFormatter)
        null
    } catch (_: DateTimeParseException) {
        "$label 시간을 HH:mm 형식으로 입력해 주세요"
    }
}

private fun isValidTimeRange(startTime: String, endTime: String): Boolean {
    val start = LocalTime.parse(startTime, scheduleTimeFormatter)
    val end = LocalTime.parse(endTime, scheduleTimeFormatter)
    return end.isAfter(start)
}

@Composable
private fun errorSupportingText(show: Boolean, message: String?): (@Composable () -> Unit)? =
    if (show && message != null) ({ Text(message) }) else null

package com.gayadi.android.ui.components

import android.widget.NumberPicker

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Notes
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gayadi.android.ui.theme.PrimaryAction
import com.gayadi.android.ui.theme.TextPrimary
import com.gayadi.android.ui.theme.TextSecondary
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.format.ResolverStyle

private val scheduleTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    .withResolverStyle(ResolverStyle.STRICT)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleOptionsBottomSheet(
    title: String,
    contextText: String,
    initialTime: String = "10:00",
    initialMemo: String = "",
    heading: String = "여행 일정에 추가",
    confirmLabel: String = "일정에 추가",
    onAddExpense: ((time: String, memo: String) -> Unit)? = null,
    onDirections: ((time: String, memo: String) -> Unit)? = null,
    onDismiss: () -> Unit,
    onConfirm: (time: String, memo: String) -> Unit,
) {
    var time by rememberSaveable(title, initialTime) { mutableStateOf(initialTime) }
    var memo by rememberSaveable(title, initialMemo) { mutableStateOf(initialMemo) }
    var submitted by rememberSaveable(title) { mutableStateOf(false) }
    var showTimeDialog by rememberSaveable(title) { mutableStateOf(false) }
    var showMemoDialog by rememberSaveable(title) { mutableStateOf(false) }
    val timeError = try {
        LocalTime.parse(time, scheduleTimeFormatter)
        null
    } catch (_: DateTimeParseException) {
        "시간을 HH:mm 형식으로 입력해 주세요"
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Box(
                Modifier.padding(top = 10.dp, bottom = 8.dp).size(width = 40.dp, height = 4.dp)
                    .background(Color(0xFFD7D8DC), RoundedCornerShape(2.dp)),
            )
        },
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().imePadding().navigationBarsPadding()
                .padding(start = 24.dp, top = 8.dp, end = 24.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (heading.isNotBlank()) {
                Text(heading, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(title, fontSize = 21.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                if (contextText.isNotBlank()) {
                    Text(contextText, fontSize = 14.sp, color = TextSecondary)
                }
            }
            ScheduleOptionRow(
                icon = { Icon(Icons.Outlined.AccessTime, contentDescription = null) },
                label = "시간 추가",
                value = time,
                onClick = { showTimeDialog = true },
            )
            ScheduleOptionRow(
                icon = { Icon(Icons.AutoMirrored.Outlined.Notes, contentDescription = null) },
                label = "메모 추가",
                value = memo,
                onClick = { showMemoDialog = true },
            )
            if (onAddExpense != null || onDirections != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    onAddExpense?.let { action ->
                        OutlinedButton(
                            onClick = {
                                submitted = true
                                if (timeError == null) action(time, memo.trim())
                            },
                            modifier = Modifier.weight(1f).height(40.dp),
                            shape = RoundedCornerShape(0.dp),
                        ) {
                            Text("비용 추가", fontWeight = FontWeight.SemiBold)
                        }
                    }
                    onDirections?.let { action ->
                        OutlinedButton(
                            onClick = {
                                submitted = true
                                if (timeError == null) action(time, memo.trim())
                            },
                            modifier = Modifier.weight(1f).height(40.dp),
                            shape = RoundedCornerShape(0.dp),
                        ) {
                            Text("길찾기", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
            Button(
                onClick = {
                    submitted = true
                    if (timeError == null) onConfirm(time, memo.trim())
                },
                modifier = Modifier.fillMaxWidth().height(44.dp),
                shape = RoundedCornerShape(0.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryAction, contentColor = Color.White),
            ) {
                Text(confirmLabel, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }

    if (showTimeDialog) {
        WheelTimePickerDialog(
            initialTime = time,
            onDismiss = { showTimeDialog = false },
            onConfirm = {
                time = it
                showTimeDialog = false
            },
        )
    }

    if (showMemoDialog) {
        var draftMemo by rememberSaveable(title, memo) { mutableStateOf(memo) }
        Dialog(
            onDismissRequest = { showMemoDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).heightIn(min = 320.dp, max = 340.dp),
                color = Color.White,
                shape = RoundedCornerShape(10.dp),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text("메모", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                OutlinedTextField(
                    value = draftMemo,
                    onValueChange = { draftMemo = it.take(200) },
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    placeholder = { Text("할 일이나 참고사항을 입력해 주세요") },
                    minLines = 6,
                    maxLines = 8,
                )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutlinedButton(
                            onClick = { showMemoDialog = false },
                            modifier = Modifier.weight(1f).height(44.dp),
                            shape = RoundedCornerShape(0.dp),
                        ) { Text("취소", fontWeight = FontWeight.SemiBold) }
                        Button(
                            onClick = {
                                memo = draftMemo.trim()
                                showMemoDialog = false
                            },
                            modifier = Modifier.weight(1f).height(44.dp),
                            shape = RoundedCornerShape(0.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PrimaryAction,
                                contentColor = Color.White,
                            ),
                        ) { Text("확인", fontWeight = FontWeight.SemiBold) }
                    }
                }
            }
        }
    }
}

@Composable
private fun WheelTimePickerDialog(
    initialTime: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    val initial = runCatching { LocalTime.parse(initialTime, scheduleTimeFormatter) }
        .getOrDefault(LocalTime.of(10, 0))
    var period by rememberSaveable(initialTime) { mutableStateOf(if (initial.hour < 12) 0 else 1) }
    var hour by rememberSaveable(initialTime) {
        mutableStateOf(initial.hour.let { if (it % 12 == 0) 12 else it % 12 })
    }
    var minute by rememberSaveable(initialTime) { mutableStateOf(initial.minute) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        text = {
            Row(
                modifier = Modifier.fillMaxWidth().height(220.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                WheelNumberPicker(
                    value = period,
                    range = 0..1,
                    labels = arrayOf("오전", "오후"),
                    onValueChange = { period = it },
                )
                WheelNumberPicker(
                    value = hour,
                    range = 1..12,
                    onValueChange = { hour = it },
                )
                WheelNumberPicker(
                    value = minute,
                    range = 0..59,
                    labels = Array(60) { it.toString().padStart(2, '0') },
                    onValueChange = { minute = it },
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val hour24 = when {
                        period == 0 && hour == 12 -> 0
                        period == 1 && hour != 12 -> hour + 12
                        else -> hour
                    }
                    onConfirm("${hour24.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}")
                },
                modifier = Modifier.fillMaxWidth().height(44.dp),
                shape = RoundedCornerShape(0.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryAction, contentColor = Color.White),
            ) { Text("확인", fontWeight = FontWeight.SemiBold) }
        },
    )
}

@Composable
private fun WheelNumberPicker(
    value: Int,
    range: IntRange,
    labels: Array<String>? = null,
    onValueChange: (Int) -> Unit,
) {
    AndroidView(
        factory = { context ->
            NumberPicker(context).apply {
                minValue = range.first
                maxValue = range.last
                wrapSelectorWheel = true
                displayedValues = labels
                setOnValueChangedListener { _, _, newValue -> onValueChange(newValue) }
            }
        },
        update = { picker ->
            if (picker.value != value) picker.value = value
        },
        modifier = Modifier.width(82.dp).height(190.dp),
    )
}

@Composable
private fun ScheduleOptionRow(
    icon: @Composable () -> Unit,
    label: String,
    value: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.size(28.dp), contentAlignment = Alignment.Center) { icon() }
        Text(
            text = label,
            modifier = Modifier.padding(start = 10.dp),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = TextSecondary,
        )
        Spacer(Modifier.weight(1f))
        if (value.isNotBlank()) {
            Text(
                text = value,
                modifier = Modifier.padding(start = 16.dp),
                fontSize = 14.sp,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gayadi.android.ui.theme.PretendardFontFamily
import com.gayadi.android.ui.theme.PretendardSemiBoldFontFamily
import com.gayadi.android.ui.theme.TextPrimary
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private enum class DateField { START, END }

private val tripDateFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripCreateScreen(
    onBack: () -> Unit,
    onCreate: (TripSummary) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var startDate by remember { mutableStateOf<LocalDate?>(null) }
    var endDate by remember { mutableStateOf<LocalDate?>(null) }
    var selectingField by remember { mutableStateOf<DateField?>(null) }
    val canCreate = name.isNotBlank() && startDate != null && endDate != null

    Column(
        modifier = Modifier.fillMaxSize().background(Color(0xFFFAFAFB)).padding(horizontal = 20.dp),
    ) {
        Spacer(modifier = Modifier.height(42.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로가기")
            }
            Text(
                "새 여행 만들기",
                fontFamily = PretendardSemiBoldFontFamily,
                fontSize = 20.sp,
                color = TextPrimary,
            )
        }
        Spacer(modifier = Modifier.height(28.dp))
        Text("여행 이름", fontFamily = PretendardSemiBoldFontFamily, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("예: 여름 제주 여행", fontFamily = PretendardFontFamily) },
            singleLine = true,
        )
        Spacer(modifier = Modifier.height(22.dp))
        Text("여행 기간", fontFamily = PretendardSemiBoldFontFamily, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            DateField(
                value = startDate?.format(tripDateFormatter).orEmpty(),
                placeholder = "시작일",
                onClick = { selectingField = DateField.START },
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "~",
                modifier = Modifier.padding(horizontal = 10.dp),
                fontFamily = PretendardSemiBoldFontFamily,
                color = TextPrimary,
            )
            DateField(
                value = endDate?.format(tripDateFormatter).orEmpty(),
                placeholder = "종료일",
                onClick = { selectingField = DateField.END },
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        Button(
            onClick = {
                onCreate(
                    TripSummary(
                        name.trim(),
                        startDate!!.format(tripDateFormatter),
                        endDate!!.format(tripDateFormatter),
                    ),
                )
            },
            enabled = canCreate,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(2.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF343548),
                disabledContainerColor = Color(0xFFD5D5DA),
            ),
        ) {
            Text("여행 만들기", fontFamily = PretendardSemiBoldFontFamily, fontSize = 15.sp)
        }
        Spacer(modifier = Modifier.height(16.dp))
    }

    selectingField?.let { field ->
        val initialDate = when (field) {
            DateField.START -> startDate
            DateField.END -> endDate ?: startDate
        } ?: LocalDate.now()
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = initialDate.toUtcMillis(),
        )

        DatePickerDialog(
            onDismissRequest = { selectingField = null },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.toLocalDate()?.let { selectedDate ->
                            when (field) {
                                DateField.START -> {
                                    startDate = selectedDate
                                    if (endDate?.isBefore(selectedDate) == true) endDate = null
                                }
                                DateField.END -> {
                                    if (startDate == null || !selectedDate.isBefore(startDate)) {
                                        endDate = selectedDate
                                    }
                                }
                            }
                        }
                        selectingField = null
                    },
                ) { Text("확인") }
            },
            dismissButton = {
                TextButton(onClick = { selectingField = null }) { Text("취소") }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }
}

@Composable
private fun DateField(
    value: String,
    placeholder: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = {},
        modifier = modifier.clickable(onClick = onClick),
        enabled = false,
        placeholder = { Text(placeholder, fontFamily = PretendardFontFamily) },
        trailingIcon = { Icon(Icons.Default.DateRange, contentDescription = "$placeholder 달력 열기") },
        singleLine = true,
        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
            disabledTextColor = TextPrimary,
            disabledBorderColor = Color(0xFFD5D5DA),
            disabledPlaceholderColor = Color(0xFF8B8B95),
            disabledTrailingIconColor = Color(0xFF6D6E78),
        ),
    )
}

private fun LocalDate.toUtcMillis(): Long =
    atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

private fun Long.toLocalDate(): LocalDate =
    Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()

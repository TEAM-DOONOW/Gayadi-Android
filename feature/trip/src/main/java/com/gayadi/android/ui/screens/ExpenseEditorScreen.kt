package com.gayadi.android.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gayadi.android.domain.model.TravelExpense
import com.gayadi.android.domain.model.TravelParticipant
import com.gayadi.android.domain.model.TravelSchedule
import com.gayadi.android.ui.components.UserCharacterAvatar
import com.gayadi.android.ui.theme.GayadiTheme
import com.gayadi.android.ui.theme.PrimaryAction
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

private val expenseDateFormatter = DateTimeFormatter.ofPattern("uuuu.MM.dd")
    .withResolverStyle(ResolverStyle.STRICT)
private val expenseTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    .withResolverStyle(ResolverStyle.STRICT)
private val ExpenseError = Color(0xFFD94B4B)

@Composable
fun ExpenseEditorScreen(
    expense: TravelExpense?,
    schedule: TravelSchedule?,
    participants: List<TravelParticipant>,
    initialPayerId: String?,
    onBack: () -> Unit,
    onSave: (TravelExpense) -> Unit,
    isSaving: Boolean = false,
    errorMessage: String? = null,
) {
    val formKey = expense?.id ?: schedule?.id ?: "missing-schedule"
    val draftId = rememberSaveable(formKey) { expense?.id ?: UUID.randomUUID().toString() }
    val participantIds = remember(participants) { participants.map(TravelParticipant::id).toSet() }
    var title by rememberSaveable(formKey) { mutableStateOf(expense?.title.orEmpty()) }
    var memo by rememberSaveable(formKey) { mutableStateOf(expense?.memo.orEmpty()) }
    var amountText by rememberSaveable(formKey) { mutableStateOf(expense?.amount?.toString().orEmpty()) }
    var payerId by rememberSaveable(formKey, participantIds) {
        mutableStateOf(
            expense?.payerId?.takeIf { it in participantIds }
                ?: initialPayerId?.takeIf { it in participantIds }
                ?: participants.firstOrNull()?.id.orEmpty(),
        )
    }
    var splitParticipantIds by rememberSaveable(formKey, participantIds) {
        mutableStateOf(
            if (expense == null) {
                participantIds.toList()
            } else {
                expense.participantIds.filter { it in participantIds }
            },
        )
    }
    var date by rememberSaveable(formKey) { mutableStateOf(expense?.date ?: schedule?.date.orEmpty()) }
    var time by rememberSaveable(formKey) { mutableStateOf(expense?.time ?: schedule?.time.orEmpty()) }
    var submitted by rememberSaveable(formKey) { mutableStateOf(false) }

    val titleError = titleErrorMessage(title)
    val amountError = amountErrorMessage(amountText)
    val payerError = if (payerId.isBlank() || payerId !in participantIds) "결제자를 선택해 주세요" else null
    val splitError = if (splitParticipantIds.isEmpty()) "분담 참여자를 한 명 이상 선택해 주세요" else null
    val dateError = expenseDateErrorMessage(date)
    val timeError = expenseTimeErrorMessage(time)
    val formIsValid = schedule != null && listOf(
        titleError,
        amountError,
        payerError,
        splitError,
        dateError,
        timeError,
    ).all { it == null }

    BackHandler(enabled = isSaving) { }

    Column(Modifier.fillMaxSize().background(Color.White)) {
        Spacer(Modifier.height(36.dp))
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack, enabled = !isSaving) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "뒤로")
            }
            Column(Modifier.weight(1f)) {
                Text(
                    if (expense == null) "비용 추가" else "비용 수정",
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                )
                Text(schedule?.title ?: "일정 정보 확인 중", fontSize = 12.sp, color = TextSecondary)
            }
        }

        if (schedule == null) {
            MissingScheduleContent(onBack = onBack)
            return@Column
        }

        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            ScheduleExpenseContext(schedule)

            OutlinedTextField(
                value = amountText,
                onValueChange = { value -> amountText = value.filter(Char::isDigit).take(18) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("금액") },
                placeholder = { Text("예: 32000") },
                suffix = { Text("원") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                isError = submitted && amountError != null,
                supportingText = editorSupportingText(submitted, amountError),
            )

            OutlinedTextField(
                value = title,
                onValueChange = { title = it.take(EXPENSE_TITLE_LIMIT) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("내용") },
                placeholder = { Text("예: 흑돼지 저녁 식사") },
                singleLine = true,
                isError = submitted && titleError != null,
                supportingText = if (submitted && titleError != null) {
                    { Text(titleError) }
                } else {
                    { Text("${title.length}/$EXPENSE_TITLE_LIMIT") }
                },
            )

            OutlinedTextField(
                value = memo,
                onValueChange = { memo = it.take(EXPENSE_MEMO_LIMIT) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("메모 (선택)") },
                placeholder = { Text("영수증이나 정산에 필요한 내용을 남겨 주세요") },
                minLines = 3,
                maxLines = 5,
                supportingText = { Text("${memo.length}/$EXPENSE_MEMO_LIMIT") },
            )

            SectionLabel("결제자")
            if (participants.isEmpty()) {
                Text("여행 참여자를 먼저 추가해 주세요", fontSize = 13.sp, color = ExpenseError)
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    participants.forEach { participant ->
                        FilterChip(
                            selected = payerId == participant.id,
                            onClick = { payerId = participant.id },
                            label = { Text(participant.nickname) },
                            leadingIcon = {
                                UserCharacterAvatar(
                                    characterKey = participant.characterKey,
                                    contentDescription = "${participant.nickname} 캐릭터",
                                    modifier = Modifier.size(24.dp),
                                )
                            },
                        )
                    }
                }
            }
            if (submitted && payerError != null) Text(payerError, fontSize = 12.sp, color = ExpenseError)

            SectionLabel("함께 나눌 사람")
            Text("선택한 사람끼리 1원 단위까지 똑같이 나눠요", fontSize = 12.sp, color = TextSecondary)
            participants.forEach { participant ->
                ParticipantSelectionRow(
                    participant = participant,
                    checked = participant.id in splitParticipantIds,
                    onCheckedChange = { checked ->
                        splitParticipantIds = if (checked) {
                            splitParticipantIds + participant.id
                        } else {
                            splitParticipantIds - participant.id
                        }
                    },
                )
            }
            if (submitted && splitError != null) Text(splitError, fontSize = 12.sp, color = ExpenseError)

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = date,
                    onValueChange = { date = it.filter { character -> character.isDigit() || character == '.' }.take(10) },
                    modifier = Modifier.weight(1.25f),
                    label = { Text("지출 날짜") },
                    placeholder = { Text("yyyy.MM.dd") },
                    singleLine = true,
                    isError = submitted && dateError != null,
                    supportingText = editorSupportingText(submitted, dateError),
                )
                OutlinedTextField(
                    value = time,
                    onValueChange = { time = it.filter { character -> character.isDigit() || character == ':' }.take(5) },
                    modifier = Modifier.weight(0.75f),
                    label = { Text("시각") },
                    placeholder = { Text("HH:mm") },
                    singleLine = true,
                    isError = submitted && timeError != null,
                    supportingText = editorSupportingText(submitted, timeError),
                )
            }

            errorMessage?.let { Text(it, color = ExpenseError, fontSize = 13.sp) }
            Spacer(Modifier.height(6.dp))
        }

        Button(
            onClick = {
                submitted = true
                if (formIsValid) {
                    onSave(
                        TravelExpense(
                            id = draftId,
                            tripId = expense?.tripId ?: schedule.tripId,
                            scheduleId = expense?.scheduleId ?: schedule.id,
                            title = title.trim(),
                            memo = memo.trim(),
                            amount = requireNotNull(amountText.toLongOrNull()),
                            payerId = payerId,
                            participantIds = participants.map(TravelParticipant::id)
                                .filter { it in splitParticipantIds },
                            date = date,
                            time = time,
                        ),
                    )
                }
            },
            enabled = !isSaving,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).height(50.dp),
            shape = RoundedCornerShape(2.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryAction),
        ) {
            Text(
                when {
                    isSaving -> "저장 중…"
                    expense == null -> "비용 저장하기"
                    else -> "수정 내용 저장하기"
                },
                fontWeight = FontWeight.SemiBold,
            )
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun MissingScheduleContent(onBack: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("일정을 찾을 수 없어요", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
        Spacer(Modifier.height(6.dp))
        Text("일정이 삭제되었거나 아직 불러오는 중일 수 있어요.", fontSize = 13.sp, color = TextSecondary)
        TextButton(onClick = onBack) { Text("돌아가기") }
    }
}

@Composable
private fun ScheduleExpenseContext(schedule: TravelSchedule) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp)) {
            Text("연결된 일정", fontSize = 11.sp, color = TextSecondary)
            Spacer(Modifier.height(4.dp))
            Text(schedule.title, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            Text(
                if (schedule.endTime.isNullOrBlank()) {
                    "${schedule.date} ${schedule.time}"
                } else {
                    "${schedule.date} ${schedule.time}–${schedule.endTime}"
                },
                fontSize = 12.sp,
                color = TextSecondary,
            )
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
}

@Composable
private fun ParticipantSelectionRow(
    participant: TravelParticipant,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceCard, RoundedCornerShape(10.dp))
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        UserCharacterAvatar(
            characterKey = participant.characterKey,
            contentDescription = "${participant.nickname} 캐릭터",
            modifier = Modifier.size(32.dp),
        )
        Text(
            participant.nickname,
            modifier = Modifier.weight(1f).padding(start = 10.dp),
            fontWeight = FontWeight.Medium,
            color = TextPrimary,
        )
        Text(if (checked) "분담" else "제외", fontSize = 12.sp, color = if (checked) PrimaryBlue else TextSecondary)
    }
}

private fun titleErrorMessage(value: String): String? =
    if (value.isBlank()) "비용 내용을 입력해 주세요" else null

private fun amountErrorMessage(value: String): String? = when {
    value.isBlank() -> "금액을 입력해 주세요"
    value.toLongOrNull() == null -> "금액이 너무 커요"
    value.toLong() <= 0L -> "금액은 1원 이상이어야 해요"
    else -> null
}

private fun expenseDateErrorMessage(value: String): String? = when {
    value.isBlank() -> "날짜를 입력해 주세요"
    else -> try {
        LocalDate.parse(value, expenseDateFormatter)
        null
    } catch (_: DateTimeParseException) {
        "날짜 형식을 확인해 주세요"
    }
}

private fun expenseTimeErrorMessage(value: String): String? = when {
    value.isBlank() -> "시각을 입력해 주세요"
    else -> try {
        LocalTime.parse(value, expenseTimeFormatter)
        null
    } catch (_: DateTimeParseException) {
        "시각 형식을 확인해 주세요"
    }
}

@Composable
private fun editorSupportingText(show: Boolean, message: String?): (@Composable () -> Unit)? =
    if (show && message != null) ({ Text(message) }) else null

private const val EXPENSE_TITLE_LIMIT = 60
private const val EXPENSE_MEMO_LIMIT = 500

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun ExpenseEditorPreview() {
    GayadiTheme {
        ExpenseEditorScreen(
            expense = null,
            schedule = TravelSchedule(
                id = "schedule-1",
                tripId = "trip-1",
                title = "성산일출봉",
                date = "2026.08.13",
                time = "10:00",
                endTime = "11:30",
                order = 0,
            ),
            participants = listOf(
                TravelParticipant("local-user", "나", "character_pca"),
                TravelParticipant("friend-1", "여행곰", "character_sca"),
            ),
            initialPayerId = "local-user",
            onBack = {},
            onSave = {},
        )
    }
}

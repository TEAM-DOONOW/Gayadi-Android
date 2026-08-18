package com.gayadi.android.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.content.Intent
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.LocalActivity
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Museum
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Tour
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gayadi.android.domain.model.TravelExpense
import com.gayadi.android.domain.model.ExpenseCategory
import com.gayadi.android.domain.model.ExpensePaymentSource
import com.gayadi.android.domain.model.TravelParticipant
import com.gayadi.android.domain.model.TravelSchedule
import com.gayadi.android.ui.components.UserCharacterAvatar
import com.gayadi.android.ui.components.GayadiCompactTextField
import com.gayadi.android.ui.theme.GayadiTheme
import com.gayadi.android.ui.theme.PrimaryAction
import com.gayadi.android.ui.theme.PrimaryBlue
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
private val ExpenseEditorBackground = Color(0xFFF5F5F6)

@Composable
fun ExpenseEditorScreen(
    expense: TravelExpense?,
    schedule: TravelSchedule?,
    tripId: String = expense?.tripId ?: schedule?.tripId.orEmpty(),
    participants: List<TravelParticipant>,
    initialPayerId: String?,
    onBack: () -> Unit,
    onSave: (TravelExpense) -> Unit,
    isEditMode: Boolean = expense != null,
    isSaving: Boolean = false,
    errorMessage: String? = null,
    hasLoadedTravelState: Boolean = true,
    isLoadingTravelState: Boolean = false,
) {
    val formKey = expense?.id ?: schedule?.id ?: "unlinked-$tripId"
    val draftId = rememberSaveable(formKey) { expense?.id ?: UUID.randomUUID().toString() }
    val participantIds = remember(participants) { participants.map(TravelParticipant::id).toSet() }
    var title by rememberSaveable(formKey) { mutableStateOf(expense?.title.orEmpty()) }
    var amountText by rememberSaveable(formKey) { mutableStateOf(expense?.amount?.toString().orEmpty()) }
    var category by rememberSaveable(formKey) { mutableStateOf(expense?.category ?: ExpenseCategory.OTHER) }
    var paymentSource by rememberSaveable(formKey) {
        mutableStateOf(expense?.paymentSource ?: ExpensePaymentSource.PERSONAL)
    }
    var receiptImageUri by rememberSaveable(formKey) { mutableStateOf(expense?.receiptImageUri) }
    val context = LocalContext.current
    val receiptLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            runCatching {
                context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            receiptImageUri = it.toString()
        }
    }
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
    var date by rememberSaveable(formKey) {
        mutableStateOf(expense?.date ?: schedule?.date ?: LocalDate.now().format(expenseDateFormatter))
    }
    var time by rememberSaveable(formKey) {
        mutableStateOf(expense?.time ?: schedule?.time ?: LocalTime.now().format(expenseTimeFormatter))
    }
    var submitted by rememberSaveable(formKey) { mutableStateOf(false) }

    val titleError = titleErrorMessage(title)
    val amountError = amountErrorMessage(amountText)
    val payerError = if (
        paymentSource == ExpensePaymentSource.PERSONAL && (payerId.isBlank() || payerId !in participantIds)
    ) "결제자를 선택해 주세요" else null
    val splitError = if (splitParticipantIds.isEmpty()) "분담 참여자를 한 명 이상 선택해 주세요" else null
    val dateError = expenseDateErrorMessage(date)
    val timeError = expenseTimeErrorMessage(time)
    val formIsValid = listOf(
        titleError,
        amountError,
        payerError,
        splitError,
        dateError,
        timeError,
    ).all { it == null }

    BackHandler(enabled = isSaving) { }

    Column(
        Modifier
            .fillMaxSize()
            .background(ExpenseEditorBackground)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack, enabled = !isSaving) {
                Icon(Icons.Default.Close, "닫기")
            }
            Text(
                if (isEditMode) "지출 수정" else "지출 추가",
                fontSize = 21.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                modifier = Modifier.weight(1f),
            )
        }

        if (!hasLoadedTravelState && isLoadingTravelState) {
            LoadingScheduleContent()
            return@Column
        }

        if (!hasLoadedTravelState) {
            UnavailableTravelStateContent(onBack = onBack)
            return@Column
        }

        if (isEditMode && expense == null) {
            MissingExpenseContent(onBack = onBack)
            return@Column
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SectionLabel("지출 금액")
            GayadiCompactTextField(
                label = "지출 금액",
                value = amountText,
                onValueChange = { value -> amountText = value.filter(Char::isDigit).take(18) },
                modifier = Modifier.fillMaxWidth().background(Color.White),
                placeholder = "0",
                leadingContent = { Text("KRW", fontWeight = FontWeight.Bold, color = TextSecondary) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
            if (submitted && amountError != null) Text(amountError, fontSize = 12.sp, color = ExpenseError)
            Spacer(Modifier.height(6.dp))

            SectionLabel("카테고리")
            ExpenseCategoryPicker(selected = category, onSelect = { category = it })
            Spacer(Modifier.height(6.dp))

            SectionLabel("지출 내용")
            GayadiCompactTextField(
                label = "지출 내용",
                value = title,
                onValueChange = { title = it.take(EXPENSE_TITLE_LIMIT) },
                modifier = Modifier.fillMaxWidth().background(Color.White),
                placeholder = "무엇에 사용했나요?",
            )
            if (submitted && titleError != null) Text(titleError, fontSize = 12.sp, color = ExpenseError)
            Spacer(Modifier.height(6.dp))

            SectionLabel("결제한 사람")
            if (participants.isEmpty()) {
                Text("여행 참여자를 먼저 추가해 주세요", fontSize = 13.sp, color = ExpenseError)
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White, RoundedCornerShape(8.dp))
                        .padding(horizontal = 6.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    PaymentSourceRow(
                        title = "공동경비",
                        selected = paymentSource == ExpensePaymentSource.SHARED_FUND,
                        icon = Icons.Default.AccountBalanceWallet,
                        onClick = { paymentSource = ExpensePaymentSource.SHARED_FUND },
                    )
                    participants.forEach { participant ->
                        ParticipantRadioRow(
                            participant = participant,
                            selected = paymentSource == ExpensePaymentSource.PERSONAL && payerId == participant.id,
                            onClick = {
                                paymentSource = ExpensePaymentSource.PERSONAL
                                payerId = participant.id
                            },
                        )
                    }
                }
            }
            if (submitted && payerError != null) Text(payerError, fontSize = 12.sp, color = ExpenseError)
            Spacer(Modifier.height(6.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                SectionLabel("돈 낼 사람")
                Spacer(Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .background(PrimaryAction, RoundedCornerShape(16.dp))
                        .clickable { splitParticipantIds = participantIds.toList() }
                        .padding(horizontal = 14.dp, vertical = 7.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("1/N", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White, RoundedCornerShape(8.dp))
                    .padding(horizontal = 6.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                participants.forEach { participant ->
                    ParticipantSelectionRow(
                        participant = participant,
                        checked = participant.id in splitParticipantIds,
                        onCheckedChange = { checked ->
                            splitParticipantIds = if (checked) splitParticipantIds + participant.id
                            else splitParticipantIds - participant.id
                        },
                    )
                }
            }
            if (submitted && splitError != null) Text(splitError, fontSize = 12.sp, color = ExpenseError)
            Spacer(Modifier.height(6.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White, RoundedCornerShape(8.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SectionLabel("지출 날짜")
                GayadiCompactTextField(
                    label = "지출 날짜",
                    value = date,
                    onValueChange = { date = it.filter { character -> character.isDigit() || character == '.' }.take(10) },
                    modifier = Modifier.fillMaxWidth().background(Color.White),
                    placeholder = "yyyy.MM.dd",
                )
                if (submitted && dateError != null) Text(dateError, fontSize = 12.sp, color = ExpenseError)
                SectionLabel("지출 시간")
                GayadiCompactTextField(
                    label = "지출 시간",
                    value = time,
                    onValueChange = { time = it.filter { character -> character.isDigit() || character == ':' }.take(5) },
                    modifier = Modifier.fillMaxWidth().background(Color.White),
                    placeholder = "HH:mm",
                )
                if (submitted && timeError != null) Text(timeError, fontSize = 12.sp, color = ExpenseError)
                Text("연결된 일정  ·  ${schedule?.title ?: "없음"}", fontSize = 12.sp, color = TextSecondary)
            }
            Spacer(Modifier.height(6.dp))

            SectionLabel("영수증")
            ReceiptAttachmentCard(
                receiptImageUri = receiptImageUri,
                onAdd = { receiptLauncher.launch(arrayOf("image/*")) },
                onRemove = { receiptImageUri = null },
            )

            errorMessage?.let { Text(it, color = ExpenseError, fontSize = 13.sp) }
            Spacer(Modifier.height(6.dp))
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Button(
                onClick = onBack,
                enabled = !isSaving,
                modifier = Modifier.weight(1f).height(50.dp),
                shape = RoundedCornerShape(0.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFE1E1E3),
                    contentColor = TextPrimary,
                ),
            ) {
                Text("취소", fontWeight = FontWeight.SemiBold)
            }
            Button(
                onClick = {
                    submitted = true
                    if (formIsValid) {
                        onSave(
                            TravelExpense(
                                id = draftId,
                                tripId = expense?.tripId ?: tripId,
                                scheduleId = expense?.scheduleId ?: schedule?.id.orEmpty(),
                                title = title.trim(),
                                memo = "",
                                amount = requireNotNull(amountText.toLongOrNull()),
                                payerId = payerId,
                                participantIds = participants.map(TravelParticipant::id)
                                    .filter { it in splitParticipantIds },
                                date = date,
                                time = time,
                                category = category,
                                paymentSource = paymentSource,
                                receiptImageUri = receiptImageUri,
                            ),
                        )
                    }
                },
                enabled = !isSaving,
                modifier = Modifier.weight(1f).height(50.dp),
                shape = RoundedCornerShape(0.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryAction),
            ) {
                Text(if (isSaving) "저장 중…" else "저장", fontWeight = FontWeight.SemiBold)
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun LoadingScheduleContent() {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator()
        Spacer(Modifier.height(12.dp))
        Text("일정 정보를 불러오는 중이에요", fontSize = 14.sp, color = TextSecondary)
    }
}

@Composable
private fun UnavailableTravelStateContent(onBack: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            "여행 정보를 불러오지 못했어요",
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary,
        )
        Spacer(Modifier.height(6.dp))
        Text("잠시 후 다시 시도해 주세요.", fontSize = 13.sp, color = TextSecondary)
        TextButton(onClick = onBack) { Text("돌아가기") }
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
        Text("일정이 삭제되었거나 주소가 올바르지 않아요.", fontSize = 13.sp, color = TextSecondary)
        TextButton(onClick = onBack) { Text("돌아가기") }
    }
}

@Composable
private fun MissingExpenseContent(onBack: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            "비용 내역을 찾을 수 없어요",
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary,
        )
        Spacer(Modifier.height(6.dp))
        Text("비용이 삭제되었거나 주소가 올바르지 않아요.", fontSize = 13.sp, color = TextSecondary)
        TextButton(onClick = onBack) { Text("돌아가기") }
    }
}

@Composable
private fun ScheduleExpenseContext(schedule: TravelSchedule) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(0.dp),
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
    Text(text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
}

private data class ExpenseCategoryOption(
    val category: ExpenseCategory,
    val label: String,
    val icon: ImageVector,
    val color: Color,
)

private val expenseCategoryOptions = listOf(
    ExpenseCategoryOption(ExpenseCategory.TOUR, "관광", Icons.Default.Tour, Color(0xFF4C3CF4)),
    ExpenseCategoryOption(ExpenseCategory.MUSEUM, "박물관·미술관", Icons.Default.Museum, Color(0xFFC98B45)),
    ExpenseCategoryOption(ExpenseCategory.ACTIVITY, "액티비티", Icons.Default.LocalActivity, Color(0xFFFF6659)),
    ExpenseCategoryOption(ExpenseCategory.SHOPPING, "쇼핑", Icons.Default.ShoppingBag, Color(0xFFFF59CC)),
    ExpenseCategoryOption(ExpenseCategory.FOOD, "음식", Icons.Default.Restaurant, Color(0xFFF1C86E)),
    ExpenseCategoryOption(ExpenseCategory.LODGING, "숙박", Icons.Default.Hotel, Color(0xFF8B20EB)),
    ExpenseCategoryOption(ExpenseCategory.TRANSPORT, "교통", Icons.Default.DirectionsBus, Color(0xFF46D9A1)),
    ExpenseCategoryOption(ExpenseCategory.FLIGHT, "항공", Icons.Default.Flight, Color(0xFF5CB7EF)),
    ExpenseCategoryOption(ExpenseCategory.OTHER, "기타", Icons.Default.MoreHoriz, Color(0xFF99999B)),
)

@Composable
private fun ExpenseCategoryPicker(selected: ExpenseCategory, onSelect: (ExpenseCategory) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        expenseCategoryOptions.forEach { option ->
            Column(
                modifier = Modifier
                    .background(
                        if (selected == option.category) option.color.copy(alpha = 0.18f) else Color.Transparent,
                        RoundedCornerShape(0.dp),
                    )
                    .clickable { onSelect(option.category) }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier.size(48.dp).background(option.color, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(option.icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(26.dp))
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    option.label,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                )
            }
        }
    }
}

@Composable
private fun PaymentSourceRow(
    title: String,
    selected: Boolean,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = selected, onCheckedChange = { onClick() }, modifier = Modifier.size(32.dp))
        Box(Modifier.size(28.dp).background(Color(0xFFE5E5E8), CircleShape), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(17.dp))
        }
        Text(title, modifier = Modifier.weight(1f).padding(start = 6.dp), fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
    }
}

@Composable
private fun ParticipantRadioRow(
    participant: TravelParticipant,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = selected, onCheckedChange = { onClick() }, modifier = Modifier.size(32.dp))
        UserCharacterAvatar(
            characterKey = participant.characterKey,
            contentDescription = "${participant.nickname} 캐릭터",
            modifier = Modifier.size(28.dp),
        )
        Text(participant.nickname, modifier = Modifier.weight(1f).padding(start = 6.dp), fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
    }
}

@Composable
private fun ReceiptAttachmentCard(
    receiptImageUri: String?,
    onAdd: () -> Unit,
    onRemove: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .clickable(onClick = onAdd)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(46.dp).background(Color(0xFFE8E8EB), CircleShape), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, tint = PrimaryAction)
        }
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text(if (receiptImageUri == null) "영수증 사진 추가" else "영수증 사진 첨부됨", fontWeight = FontWeight.SemiBold, color = TextPrimary)
            Text("이미지를 저장해 두고 OCR은 추후 연결해요", fontSize = 11.sp, color = TextSecondary)
        }
        if (receiptImageUri != null) {
            IconButton(onClick = onRemove) { Icon(Icons.Default.Close, contentDescription = "영수증 제거") }
        }
    }
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
            .background(Color.White)
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange, modifier = Modifier.size(32.dp))
        UserCharacterAvatar(
            characterKey = participant.characterKey,
            contentDescription = "${participant.nickname} 캐릭터",
            modifier = Modifier.size(28.dp),
        )
        Text(
            participant.nickname,
            modifier = Modifier.weight(1f).padding(start = 6.dp),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = TextPrimary,
        )
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

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun ExpenseEditorPreview() {
    GayadiTheme {
        ExpenseEditorScreen(
            tripId = "trip-1",
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

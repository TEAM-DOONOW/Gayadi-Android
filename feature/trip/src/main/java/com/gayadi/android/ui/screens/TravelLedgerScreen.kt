package com.gayadi.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gayadi.android.domain.model.ExpenseSettlementSummary
import com.gayadi.android.domain.model.ParticipantExpenseBalance
import com.gayadi.android.domain.model.SettlementTransfer
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
import java.text.NumberFormat
import java.util.Locale

private val LedgerDanger = Color(0xFFD94B4B)

@Composable
fun TravelLedgerScreen(
    tripName: String,
    expenses: List<TravelExpense>,
    schedules: List<TravelSchedule>,
    participants: List<TravelParticipant>,
    settlementSummary: ExpenseSettlementSummary,
    onBack: () -> Unit,
    onAddExpense: (String) -> Unit,
    onEditExpense: (String, String) -> Unit,
    onDeleteExpense: (String) -> Unit,
) {
    var choosingSchedule by rememberSaveable { mutableStateOf(false) }
    var deletingExpenseId by rememberSaveable { mutableStateOf<String?>(null) }
    val scheduleById = remember(schedules) { schedules.associateBy(TravelSchedule::id) }
    val participantById = remember(participants) { participants.associateBy(TravelParticipant::id) }
    val sortedExpenses = remember(expenses) {
        expenses.sortedWith(compareByDescending<TravelExpense> { it.date }.thenByDescending { it.time }.thenBy { it.id })
    }
    val expenseGroups = remember(sortedExpenses) {
        sortedExpenses.groupBy { expense -> expense.date to expense.scheduleId }.toList()
    }

    Box(Modifier.fillMaxSize().background(Color.White)) {
        Column(Modifier.fillMaxSize()) {
            Spacer(Modifier.height(36.dp))
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "뒤로") }
                Column(Modifier.weight(1f)) {
                    Text("여행 가계부", fontSize = 21.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text(tripName, fontSize = 12.sp, color = TextSecondary)
                }
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                item {
                    LedgerTotalCard(
                        total = settlementSummary.totalAmount,
                        expenseCount = expenses.size,
                        participantCount = participants.size,
                    )
                }
                if (expenses.isEmpty()) {
                    item { EmptyLedger(onAdd = { choosingSchedule = true }) }
                } else {
                    items(expenseGroups, key = { (key, _) -> "${key.first}:${key.second}" }) { (key, group) ->
                        ExpenseGroup(
                            date = key.first,
                            schedule = scheduleById[key.second],
                            expenses = group,
                            participantById = participantById,
                            onEdit = onEditExpense,
                            onDelete = { deletingExpenseId = it },
                        )
                    }
                    item {
                        SettlementSection(
                            summary = settlementSummary,
                            participantById = participantById,
                        )
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { choosingSchedule = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp),
            containerColor = PrimaryAction,
            contentColor = Color.White,
        ) {
            Icon(Icons.Default.Add, contentDescription = "비용 추가")
        }
    }

    if (choosingSchedule) {
        SchedulePickerDialog(
            schedules = schedules,
            onDismiss = { choosingSchedule = false },
            onSelect = { scheduleId ->
                choosingSchedule = false
                onAddExpense(scheduleId)
            },
        )
    }

    deletingExpenseId?.let { expenseId ->
        val expense = expenses.firstOrNull { it.id == expenseId }
        AlertDialog(
            onDismissRequest = { deletingExpenseId = null },
            title = { Text("비용을 삭제할까요?") },
            text = {
                Text(
                    if (expense == null) {
                        "삭제한 비용은 복구할 수 없어요."
                    } else {
                        "${expense.title} ${expense.amount.toWon()} 내역은 복구할 수 없어요."
                    },
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        deletingExpenseId = null
                        onDeleteExpense(expenseId)
                    },
                ) { Text("삭제", color = LedgerDanger) }
            },
            dismissButton = { TextButton(onClick = { deletingExpenseId = null }) { Text("취소") } },
        )
    }
}

@Composable
private fun LedgerTotalCard(total: Long, expenseCount: Int, participantCount: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = PrimaryAction),
        shape = RoundedCornerShape(14.dp),
    ) {
        Column(Modifier.fillMaxWidth().padding(20.dp)) {
            Text("여행 총지출", fontSize = 13.sp, color = Color.White.copy(alpha = 0.72f))
            Spacer(Modifier.height(4.dp))
            Text(total.toWon(), fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(Modifier.height(10.dp))
            Text(
                "비용 ${expenseCount}건 · 참여자 ${participantCount}명",
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.72f),
            )
        }
    }
}

@Composable
private fun EmptyLedger(onAdd: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 52.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("아직 등록한 비용이 없어요", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
        Spacer(Modifier.height(6.dp))
        Text("일정에서 쓴 비용을 기록하면 자동으로 정산해 드려요", fontSize = 12.sp, color = TextSecondary)
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = onAdd,
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryAction),
        ) { Text("첫 비용 추가") }
    }
}

@Composable
private fun ExpenseGroup(
    date: String,
    schedule: TravelSchedule?,
    expenses: List<TravelExpense>,
    participantById: Map<String, TravelParticipant>,
    onEdit: (String, String) -> Unit,
    onDelete: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
            Column(Modifier.weight(1f)) {
                Text(date, fontSize = 12.sp, color = TextSecondary)
                Text(schedule?.title ?: "삭제된 일정", fontWeight = FontWeight.SemiBold, color = TextPrimary)
            }
            Text(expenses.sumOf(TravelExpense::amount).toWon(), fontWeight = FontWeight.SemiBold, color = TextPrimary)
        }
        expenses.forEach { expense ->
            ExpenseRow(
                expense = expense,
                payer = participantById[expense.payerId],
                splitParticipants = expense.participantIds.mapNotNull(participantById::get),
                onEdit = { onEdit(expense.id, expense.scheduleId) },
                onDelete = { onDelete(expense.id) },
            )
        }
    }
}

@Composable
private fun ExpenseRow(
    expense: TravelExpense,
    payer: TravelParticipant?,
    splitParticipants: List<TravelParticipant>,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    var expanded by rememberSaveable(expense.id) { mutableStateOf(false) }
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onEdit),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 14.dp, top = 14.dp, bottom = 14.dp),
            verticalAlignment = Alignment.Top,
        ) {
            UserCharacterAvatar(
                characterKey = payer?.characterKey,
                contentDescription = "${payer?.nickname ?: "알 수 없는 결제자"} 캐릭터",
                modifier = Modifier.size(38.dp),
            )
            Column(Modifier.weight(1f).padding(start = 10.dp)) {
                Text(
                    expense.title,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                )
                Text(
                    "${expense.time} · ${payer?.nickname ?: "알 수 없음"} 결제",
                    fontSize = 12.sp,
                    color = TextSecondary,
                )
                Text(
                    "분담 ${participantNames(splitParticipants, expense.participantIds)}",
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 11.sp,
                    color = TextSecondary,
                )
                if (expense.memo.isNotBlank()) {
                    Text(
                        expense.memo,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 11.sp,
                        color = TextSecondary,
                    )
                }
            }
            Text(expense.amount.toWon(), fontWeight = FontWeight.Bold, color = TextPrimary, modifier = Modifier.padding(top = 2.dp))
            Box {
                IconButton(onClick = { expanded = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "${expense.title} 메뉴")
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    DropdownMenuItem(
                        text = { Text("수정") },
                        onClick = {
                            expanded = false
                            onEdit()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("삭제", color = LedgerDanger) },
                        onClick = {
                            expanded = false
                            onDelete()
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun SettlementSection(
    summary: ExpenseSettlementSummary,
    participantById: Map<String, TravelParticipant>,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("사람별 정산", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        summary.balances.forEach { balance ->
            BalanceRow(balance = balance, participant = participantById[balance.participantId])
        }
        Spacer(Modifier.height(4.dp))
        Text("보낼 돈", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
        if (summary.transfers.isEmpty()) {
            Text("정산할 금액이 없어요", fontSize = 13.sp, color = TextSecondary)
        } else {
            summary.transfers.forEach { transfer ->
                TransferRow(
                    transfer = transfer,
                    from = participantById[transfer.fromParticipantId],
                    to = participantById[transfer.toParticipantId],
                )
            }
        }
    }
}

@Composable
private fun BalanceRow(balance: ParticipantExpenseBalance, participant: TravelParticipant?) {
    Row(
        modifier = Modifier.fillMaxWidth().background(SurfaceCard, RoundedCornerShape(10.dp)).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        UserCharacterAvatar(
            characterKey = participant?.characterKey,
            contentDescription = "${participant?.nickname ?: "알 수 없는 참여자"} 캐릭터",
            modifier = Modifier.size(34.dp),
        )
        Column(Modifier.weight(1f).padding(start = 10.dp)) {
            Text(participant?.nickname ?: balance.participantId, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            Text(
                "결제 ${balance.paidAmount.toWon()} · 부담 ${balance.owedAmount.toWon()}",
                fontSize = 11.sp,
                color = TextSecondary,
            )
        }
        Text(
            signedWon(balance.netAmount),
            fontWeight = FontWeight.Bold,
            color = if (balance.netAmount >= 0L) PrimaryBlue else LedgerDanger,
        )
    }
}

@Composable
private fun TransferRow(
    transfer: SettlementTransfer,
    from: TravelParticipant?,
    to: TravelParticipant?,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFEBF5FF)),
        shape = RoundedCornerShape(10.dp),
    ) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "${from?.nickname ?: transfer.fromParticipantId} → ${to?.nickname ?: transfer.toParticipantId}",
                modifier = Modifier.weight(1f),
                fontWeight = FontWeight.Medium,
                color = TextPrimary,
            )
            Text(transfer.amount.toWon(), fontWeight = FontWeight.Bold, color = PrimaryBlue)
        }
    }
}

@Composable
private fun SchedulePickerDialog(
    schedules: List<TravelSchedule>,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("일정을 선택해 주세요") },
        text = {
            if (schedules.isEmpty()) {
                Text("비용을 연결할 일정이 없어요. 먼저 일정을 추가해 주세요.")
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    schedules.sortedWith(compareBy<TravelSchedule> { it.date }.thenBy { it.time }.thenBy { it.order })
                        .forEach { schedule ->
                            TextButton(
                                onClick = { onSelect(schedule.id) },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Column(Modifier.fillMaxWidth()) {
                                    Text(schedule.title, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                                    Text("${schedule.date} ${schedule.time}", fontSize = 11.sp, color = TextSecondary)
                                }
                            }
                        }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("취소") } },
    )
}

private fun participantNames(
    participants: List<TravelParticipant>,
    originalIds: List<String>,
): String = if (participants.isEmpty()) {
    if (originalIds.isEmpty()) "없음" else "알 수 없는 참여자 ${originalIds.size}명"
} else {
    participants.joinToString(" · ", transform = TravelParticipant::nickname)
}

private fun Long.toWon(): String = "${NumberFormat.getNumberInstance(Locale.KOREA).format(this)}원"

private fun signedWon(amount: Long): String = when {
    amount > 0L -> "+${amount.toWon()}"
    amount < 0L -> "-${(-amount).toWon()}"
    else -> "0원"
}

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun TravelLedgerPreview() {
    val participants = listOf(
        TravelParticipant("local-user", "나", "character_pca"),
        TravelParticipant("friend-1", "여행곰", "character_sca"),
    )
    val schedules = listOf(
        TravelSchedule(
            id = "schedule-1",
            tripId = "trip-1",
            title = "성산일출봉",
            date = "2026.08.13",
            time = "10:00",
            endTime = "11:30",
            order = 0,
        ),
    )
    val expenses = listOf(
        TravelExpense(
            id = "expense-1",
            tripId = "trip-1",
            scheduleId = "schedule-1",
            title = "점심 식사",
            memo = "흑돼지 세트",
            amount = 35_001,
            payerId = "local-user",
            participantIds = participants.map(TravelParticipant::id),
            date = "2026.08.13",
            time = "12:00",
        ),
    )
    GayadiTheme {
        TravelLedgerScreen(
            tripName = "제주 여행",
            expenses = expenses,
            schedules = schedules,
            participants = participants,
            settlementSummary = ExpenseSettlementSummary(
                totalAmount = 35_001,
                balances = listOf(
                    ParticipantExpenseBalance("local-user", 35_001, 17_501, 17_500),
                    ParticipantExpenseBalance("friend-1", 0, 17_500, -17_500),
                ),
                transfers = listOf(SettlementTransfer("friend-1", "local-user", 17_500)),
            ),
            onBack = {},
            onAddExpense = {},
            onEditExpense = { _, _ -> },
            onDeleteExpense = {},
        )
    }
}

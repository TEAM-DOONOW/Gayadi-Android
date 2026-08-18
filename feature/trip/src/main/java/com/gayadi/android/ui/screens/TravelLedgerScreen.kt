package com.gayadi.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gayadi.android.domain.model.ExpenseSettlementSummary
import com.gayadi.android.domain.model.ExpenseCategory
import com.gayadi.android.domain.model.ParticipantExpenseBalance
import com.gayadi.android.domain.model.SettlementTransfer
import com.gayadi.android.domain.model.TravelExpense
import com.gayadi.android.domain.model.TravelParticipant
import com.gayadi.android.domain.model.TravelSchedule
import com.gayadi.android.feature.trip.R
import com.gayadi.android.ui.components.UserCharacterAvatar
import com.gayadi.android.ui.components.AddSharedFundBottomSheet
import com.gayadi.android.ui.theme.GayadiTheme
import com.gayadi.android.ui.components.GayadiTopAppBar
import com.gayadi.android.ui.theme.PrimaryAction
import com.gayadi.android.ui.theme.TextPrimary
import com.gayadi.android.ui.theme.TextSecondary
import java.text.NumberFormat
import java.util.Locale

private val LedgerBlue = PrimaryAction
private val LedgerBackground = Color(0xFFF7F7F8)
private val LedgerSurface = Color(0xFFFFFFFF)
private val LedgerDivider = Color(0xFFF0F0F2)
private val LedgerDanger = Color(0xFFD94B4B)

private enum class LedgerTab(val label: String) {
    History("내역"), Settlement("정산"), Statistics("통계")
}

@Composable
fun TravelLedgerScreen(
    tripName: String,
    expenses: List<TravelExpense>,
    schedules: List<TravelSchedule>,
    participants: List<TravelParticipant>,
    settlementSummary: ExpenseSettlementSummary,
    settlementErrorMessage: String? = null,
    sharedFundBalance: Long = 0L,
    onBack: () -> Unit,
    onAddExpense: (String) -> Unit,
    onAddSharedFund: (Long) -> Unit = {},
    onOpenSettlementDetails: (String, String) -> Unit = { _, _ -> },
    onEditExpense: (String, String) -> Unit,
    onDeleteExpense: (String) -> Unit,
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var deletingExpenseId by rememberSaveable { mutableStateOf<String?>(null) }
    var addingSharedFund by rememberSaveable { mutableStateOf(false) }
    val scheduleById = remember(schedules) { schedules.associateBy(TravelSchedule::id) }
    val participantById = remember(participants) { participants.associateBy(TravelParticipant::id) }
    val sortedExpenses = remember(expenses) {
        expenses.sortedWith(compareByDescending<TravelExpense> { it.date }.thenByDescending { it.time }.thenBy { it.id })
    }
    val expenseGroups = remember(sortedExpenses) { sortedExpenses.groupBy(TravelExpense::date).toList() }

    Box(
        Modifier
            .fillMaxSize()
            .background(LedgerBackground)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Column(Modifier.fillMaxSize()) {
            LedgerTopBar(onBack = onBack)

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(start = 20.dp, top = 10.dp, end = 20.dp, bottom = 112.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                item { LedgerTabs(selected = selectedTab, onSelect = { selectedTab = it }) }
                if (LedgerTab.entries[selectedTab] == LedgerTab.History) {
                    item {
                        LedgerSummary(
                            total = settlementSummary.totalAmount.takeIf { settlementErrorMessage == null },
                            sharedFundBalance = sharedFundBalance,
                            onAddSharedFund = { addingSharedFund = true },
                        )
                    }
                }
                settlementErrorMessage?.let { message -> item { SettlementErrorCard(message) } }

                when (LedgerTab.entries[selectedTab]) {
                    LedgerTab.History -> {
                        if (expenses.isEmpty()) {
                            item { EmptyLedger() }
                        } else {
                            items(expenseGroups, key = { it.first }) { (date, group) ->
                                ExpenseDayGroup(
                                    date = date,
                                    expenses = group,
                                    scheduleById = scheduleById,
                                    onEdit = onEditExpense,
                                    onDelete = { deletingExpenseId = it },
                                )
                            }
                        }
                    }
                    LedgerTab.Settlement -> item {
                        SettlementContent(
                            summary = settlementSummary,
                            participantById = participantById,
                            onOpenDetails = onOpenSettlementDetails,
                        )
                    }
                    LedgerTab.Statistics -> item {
                        StatisticsContent(
                            total = settlementSummary.totalAmount,
                            expenses = expenses,
                        )
                    }
                }
            }
        }

        Button(
            onClick = { onAddExpense("") },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 18.dp)
                .height(54.dp),
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(containerColor = LedgerBlue, contentColor = Color.White),
            contentPadding = PaddingValues(horizontal = 24.dp),
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.size(8.dp))
            Text("지출 추가", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        }
    }

    deletingExpenseId?.let { expenseId ->
        val expense = expenses.firstOrNull { it.id == expenseId }
        AlertDialog(
            onDismissRequest = { deletingExpenseId = null },
            title = { Text("비용을 삭제할까요?") },
            text = { Text(expense?.let { "${it.title} ${it.amount.toWon()} 내역은 복구할 수 없어요." } ?: "삭제한 비용은 복구할 수 없어요.") },
            confirmButton = {
                TextButton(onClick = { deletingExpenseId = null; onDeleteExpense(expenseId) }) {
                    Text("삭제", color = LedgerDanger)
                }
            },
            dismissButton = { TextButton(onClick = { deletingExpenseId = null }) { Text("취소") } },
        )
    }

    if (addingSharedFund) {
        AddSharedFundBottomSheet(
            onDismiss = { addingSharedFund = false },
            onConfirm = { amount ->
                onAddSharedFund(amount)
                addingSharedFund = false
            },
        )
    }
}

@Composable
private fun LedgerTopBar(onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().background(LedgerBackground).padding(start = 8.dp, end = 20.dp, top = 12.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로") }
        Text("여행 가계부", fontSize = 21.sp, fontWeight = FontWeight.Bold, color = TextPrimary, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun LedgerTabs(selected: Int, onSelect: (Int) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().height(42.dp).background(Color(0xFFECECEE), RoundedCornerShape(21.dp)).padding(4.dp),
    ) {
        LedgerTab.entries.forEachIndexed { index, tab ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .background(if (selected == index) Color(0xFF343548) else Color.Transparent, RoundedCornerShape(17.dp))
                    .clickable { onSelect(index) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    tab.label,
                    fontSize = 14.sp,
                    fontWeight = if (selected == index) FontWeight.SemiBold else FontWeight.Medium,
                    color = if (selected == index) Color.White else TextSecondary,
                )
            }
        }
    }
}

@Composable
private fun LedgerSummary(total: Long?, sharedFundBalance: Long, onAddSharedFund: () -> Unit) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        LedgerAmountCard(label = "총 지출", amount = total, modifier = Modifier.fillMaxWidth())
        LedgerAmountCard(
            label = "공동 경비 잔액",
            amount = sharedFundBalance,
            modifier = Modifier.fillMaxWidth(),
            amountColor = LedgerBlue,
            onAddSharedFund = onAddSharedFund,
        )
    }
}

@Composable
private fun LedgerAmountCard(
    label: String,
    amount: Long?,
    modifier: Modifier = Modifier,
    amountColor: Color = TextPrimary,
    onAddSharedFund: (() -> Unit)? = null,
) {
    Card(
        modifier = modifier.height(if (onAddSharedFund == null) 104.dp else 120.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = LedgerSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                label,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                maxLines = 1,
            )
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                Row(Modifier.weight(1f), verticalAlignment = Alignment.Bottom) {
                    Text(
                        "KRW",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF666874),
                        modifier = Modifier.padding(bottom = 4.dp),
                    )
                    Spacer(Modifier.size(6.dp))
                    Text(
                        amount?.toNumber() ?: "계산 불가",
                        fontSize = if (amount == null) 16.sp else 23.sp,
                        lineHeight = 27.sp,
                        fontWeight = FontWeight.Bold,
                        color = amountColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                onAddSharedFund?.let { onAdd ->
                    Button(
                        onClick = onAdd,
                        modifier = Modifier.height(38.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = LedgerBlue, contentColor = Color.White),
                        contentPadding = PaddingValues(horizontal = 12.dp),
                    ) {
                        Icon(Icons.Default.Savings, contentDescription = null, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.size(6.dp))
                        Text("공동 경비 추가", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyLedger() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 34.dp, bottom = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            painter = painterResource(R.drawable.money_bag),
            contentDescription = null,
            modifier = Modifier.size(30.dp),
            contentScale = ContentScale.Fit,
        )
        Spacer(Modifier.height(12.dp))
        Text("아직 등록한 비용이 없어요", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
    }
}

@Composable
private fun ExpenseDayGroup(
    date: String,
    expenses: List<TravelExpense>,
    scheduleById: Map<String, TravelSchedule>,
    onEdit: (String, String) -> Unit,
    onDelete: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(9.dp).background(LedgerBlue, CircleShape))
            Spacer(Modifier.size(8.dp))
            Text(formatDayLabel(date), fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextPrimary, modifier = Modifier.weight(1f))
            Text(expenses.sumOf(TravelExpense::amount).toWon(), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
        }
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = LedgerSurface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        ) {
            Column {
                expenses.forEachIndexed { index, expense ->
                    ExpenseRow(
                        expense = expense,
                        subtitle = scheduleById[expense.scheduleId]?.title ?: expense.time,
                        onEdit = { onEdit(expense.id, expense.scheduleId) },
                        onDelete = { onDelete(expense.id) },
                    )
                    if (index != expenses.lastIndex) Spacer(Modifier.fillMaxWidth().height(1.dp).background(LedgerDivider))
                }
            }
        }
    }
}

@Composable
private fun ExpenseRow(expense: TravelExpense, subtitle: String, onEdit: () -> Unit, onDelete: () -> Unit) {
    var expanded by rememberSaveable(expense.id) { mutableStateOf(false) }
    val visual = remember(expense.category, expense.title) { categoryVisual(expense.category, expense.title) }
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onEdit).padding(start = 14.dp, top = 13.dp, bottom = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.size(46.dp).background(visual.background, CircleShape), contentAlignment = Alignment.Center) {
            Image(
                painter = painterResource(visual.iconRes),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                contentScale = ContentScale.Fit,
                colorFilter = ColorFilter.tint(Color.White),
            )
        }
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text(expense.title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
            Spacer(Modifier.height(2.dp))
            Text(subtitle, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 11.sp, color = TextSecondary)
        }
        Text(expense.amount.toWon(), fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
        Box {
            IconButton(onClick = { expanded = true }) { Icon(Icons.Default.MoreVert, contentDescription = "${expense.title} 메뉴", tint = TextSecondary) }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                DropdownMenuItem(text = { Text("수정") }, onClick = { expanded = false; onEdit() })
                DropdownMenuItem(text = { Text("삭제", color = LedgerDanger) }, onClick = { expanded = false; onDelete() })
            }
        }
    }
}

@Composable
private fun SettlementContent(
    summary: ExpenseSettlementSummary,
    participantById: Map<String, TravelParticipant>,
    onOpenDetails: (String, String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("친구와 정산", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        if (summary.balances.isEmpty()) {
            Card(colors = CardDefaults.cardColors(containerColor = LedgerSurface), shape = RoundedCornerShape(18.dp)) {
                Text("여행 참여자가 없어요", modifier = Modifier.fillMaxWidth().padding(22.dp), fontSize = 14.sp, color = TextSecondary)
            }
        } else {
            summary.balances.forEach { balance ->
                ParticipantSettlementCard(balance, participantById[balance.participantId], onOpenDetails)
            }
        }
    }
}

@Composable
private fun ParticipantSettlementCard(
    balance: ParticipantExpenseBalance,
    participant: TravelParticipant?,
    onOpenDetails: (String, String) -> Unit,
) {
    val settlementLabel = when {
        balance.netAmount < 0L -> "보내야 할 금액"
        balance.netAmount > 0L -> "받을 금액"
        else -> "보낼 금액"
    }
    val settlementAmount = kotlin.math.abs(balance.netAmount)
    val settlementColor = when {
        balance.netAmount < 0L -> LedgerDanger
        balance.netAmount > 0L -> LedgerBlue
        else -> TextPrimary
    }
    Card(colors = CardDefaults.cardColors(containerColor = LedgerSurface), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                UserCharacterAvatar(
                    characterKey = participant?.characterKey.orEmpty(),
                    contentDescription = "${participant?.nickname ?: balance.participantId} 프로필",
                    modifier = Modifier.size(38.dp),
                )
                Text(
                    participant?.nickname ?: balance.participantId,
                    modifier = Modifier.padding(start = 10.dp),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SettlementMetricBox(
                    label = "결제한 금액",
                    amount = balance.paidAmount,
                    onClick = { onOpenDetails(balance.participantId, "paid") },
                    modifier = Modifier.weight(1f),
                )
                SettlementMetricBox(
                    label = "지출된 금액",
                    amount = balance.owedAmount,
                    onClick = { onOpenDetails(balance.participantId, "spent") },
                    modifier = Modifier.weight(1f),
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.Bottom) {
                Text(
                    settlementLabel,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                )
                Spacer(Modifier.size(10.dp))
                Text(settlementAmount.toWon(), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = settlementColor)
            }
        }
    }
}

@Composable
private fun SettlementMetricBox(
    label: String,
    amount: Long,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .background(LedgerBackground, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                label,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
            )
            Spacer(Modifier.height(4.dp))
            Text(amount.toWon(), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        }
        Icon(
            Icons.Default.ChevronRight,
            contentDescription = "내역 보기",
            tint = TextSecondary,
            modifier = Modifier.size(20.dp),
        )
    }
}


@Composable
private fun StatisticsContent(total: Long, expenses: List<TravelExpense>) {
    val categoryStats = remember(expenses) {
        val totals = expenses.groupBy(::resolvedCategory).mapValues { (_, values) -> values.sumOf(TravelExpense::amount) }
        statisticsCategoryOrder.map { category -> CategoryExpenseStat(category, totals[category] ?: 0L) }
    }
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Column(Modifier.fillMaxWidth()) {
            Text("카테고리별 지출", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Spacer(Modifier.height(12.dp))
            Text("총 지출", fontSize = 13.sp, color = TextSecondary)
            Text(total.toWon(), fontSize = 25.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        }

        if (total > 0L) {
            CategoryDistributionBar(stats = categoryStats)
        } else {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(28.dp)
                    .background(Color(0xFFE3E3E6), RoundedCornerShape(9.dp)),
            )
        }
        Card(colors = CardDefaults.cardColors(containerColor = LedgerSurface), shape = RoundedCornerShape(22.dp)) {
            Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                categoryStats.forEach { stat ->
                    CategoryStatisticRow(stat = stat, total = total)
                }
            }
        }
    }
}

private data class CategoryExpenseStat(val category: ExpenseCategory, val amount: Long)

private val statisticsCategoryOrder = listOf(
    ExpenseCategory.FLIGHT,
    ExpenseCategory.TRANSPORT,
    ExpenseCategory.FOOD,
    ExpenseCategory.LODGING,
    ExpenseCategory.SHOPPING,
    ExpenseCategory.TOUR,
    ExpenseCategory.OTHER,
    ExpenseCategory.ACTIVITY,
    ExpenseCategory.MUSEUM,
)

@Composable
private fun CategoryDistributionBar(stats: List<CategoryExpenseStat>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(28.dp)
            .clip(RoundedCornerShape(9.dp)),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        stats.forEach { stat ->
            Box(
                Modifier
                    .weight(stat.amount.toFloat().coerceAtLeast(1f))
                    .fillMaxHeight()
                    .background(categoryVisual(stat.category, "").background),
            )
        }
    }
}

@Composable
private fun CategoryStatisticRow(stat: CategoryExpenseStat, total: Long) {
    val visual = categoryVisual(stat.category, "")
    val percentage = if (total <= 0L) 0.0 else stat.amount.toDouble() * 100.0 / total.toDouble()
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(42.dp).background(visual.background, CircleShape), contentAlignment = Alignment.Center) {
            Image(
                painter = painterResource(visual.iconRes),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                contentScale = ContentScale.Fit,
                colorFilter = ColorFilter.tint(Color.White),
            )
        }
        Text(
            categoryLabel(stat.category),
            modifier = Modifier.weight(1f).padding(start = 12.dp),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = TextPrimary,
        )
        Text(stat.amount.toWon(), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
        Text(
            String.format(Locale.KOREA, "%.1f%%", percentage),
            modifier = Modifier.padding(start = 10.dp),
            fontSize = 13.sp,
            color = TextSecondary,
        )
    }
}

@Composable
private fun SettlementErrorCard(message: String) {
    Card(colors = CardDefaults.cardColors(containerColor = LedgerDanger.copy(alpha = 0.1f)), shape = RoundedCornerShape(14.dp)) {
        Column(Modifier.fillMaxWidth().padding(14.dp)) {
            Text("정산 정보를 계산하지 못했어요", fontWeight = FontWeight.SemiBold, color = LedgerDanger)
            Spacer(Modifier.height(4.dp))
            Text(message, fontSize = 12.sp, color = TextSecondary)
        }
    }
}

@Composable
private fun SchedulePickerDialog(schedules: List<TravelSchedule>, onDismiss: () -> Unit, onSelect: (String) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("일정을 선택해 주세요") },
        text = {
            if (schedules.isEmpty()) {
                Text("비용을 연결할 일정이 없어요. 먼저 일정을 추가해 주세요.")
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    schedules.sortedWith(compareBy<TravelSchedule> { it.date }.thenBy { it.time }.thenBy { it.order }).forEach { schedule ->
                        TextButton(onClick = { onSelect(schedule.id) }, modifier = Modifier.fillMaxWidth()) {
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

private data class CategoryVisual(val iconRes: Int, val background: Color)

private fun categoryLabel(category: ExpenseCategory): String = when (category) {
    ExpenseCategory.TOUR -> "관광"
    ExpenseCategory.MUSEUM -> "박물관·미술관"
    ExpenseCategory.ACTIVITY -> "액티비티"
    ExpenseCategory.SHOPPING -> "쇼핑"
    ExpenseCategory.FOOD -> "음식"
    ExpenseCategory.LODGING -> "숙박"
    ExpenseCategory.TRANSPORT -> "교통"
    ExpenseCategory.FLIGHT -> "항공"
    ExpenseCategory.OTHER -> "기타"
}

private fun resolvedCategory(expense: TravelExpense): ExpenseCategory =
    if (expense.category != ExpenseCategory.OTHER) expense.category else inferCategory(expense.title)

private fun inferCategory(title: String): ExpenseCategory {
    val normalized = title.lowercase(Locale.KOREA)
    return when {
        listOf("호텔", "숙소", "펜션", "게스트").any(normalized::contains) -> ExpenseCategory.LODGING
        listOf("항공", "비행", "티켓").any(normalized::contains) -> ExpenseCategory.FLIGHT
        listOf("렌터카", "택시", "교통", "주유").any(normalized::contains) -> ExpenseCategory.TRANSPORT
        listOf("카페", "커피", "디저트", "식사", "점심", "저녁", "아침", "고기", "맛집").any(normalized::contains) -> ExpenseCategory.FOOD
        listOf("박물관", "뮤지엄", "미술관").any(normalized::contains) -> ExpenseCategory.MUSEUM
        listOf("관광", "관람", "입장").any(normalized::contains) -> ExpenseCategory.TOUR
        listOf("체험", "액티비티", "놀이").any(normalized::contains) -> ExpenseCategory.ACTIVITY
        listOf("쇼핑", "기념품", "마트").any(normalized::contains) -> ExpenseCategory.SHOPPING
        else -> ExpenseCategory.OTHER
    }
}

private fun categoryVisual(category: ExpenseCategory, title: String): CategoryVisual {
    when (category) {
        ExpenseCategory.TOUR -> return CategoryVisual(R.drawable.location, Color(0xFF4E4BEA))
        ExpenseCategory.MUSEUM -> return CategoryVisual(R.drawable.goverment, Color(0xFFC98B45))
        ExpenseCategory.ACTIVITY -> return CategoryVisual(R.drawable.stretching_exercises, Color(0xFFFF6659))
        ExpenseCategory.SHOPPING -> return CategoryVisual(R.drawable.grocery_store, Color(0xFFFF59CC))
        ExpenseCategory.FOOD -> return CategoryVisual(R.drawable.cutlery, Color(0xFFF0B84B))
        ExpenseCategory.LODGING -> return CategoryVisual(R.drawable.bed, Color(0xFF8B31E8))
        ExpenseCategory.TRANSPORT -> return CategoryVisual(R.drawable.bus, Color(0xFF39B982))
        ExpenseCategory.FLIGHT -> return CategoryVisual(R.drawable.plane, Color(0xFF48A9E4))
        ExpenseCategory.OTHER -> Unit
    }
    val normalized = title.lowercase(Locale.KOREA)
    return when {
        listOf("호텔", "숙소", "펜션", "게스트").any(normalized::contains) -> CategoryVisual(R.drawable.bed, Color(0xFF8B31E8))
        listOf("항공", "비행", "티켓").any(normalized::contains) -> CategoryVisual(R.drawable.plane, Color(0xFF48A9E4))
        listOf("렌터카", "택시", "교통", "주유").any(normalized::contains) -> CategoryVisual(R.drawable.bus, Color(0xFF39B982))
        listOf("카페", "커피", "디저트", "식사", "점심", "저녁", "아침", "고기", "맛집").any(normalized::contains) -> CategoryVisual(R.drawable.cutlery, Color(0xFFF0B84B))
        listOf("박물관", "뮤지엄").any(normalized::contains) -> CategoryVisual(R.drawable.goverment, Color(0xFFC98B45))
        listOf("관광", "관람", "입장").any(normalized::contains) -> CategoryVisual(R.drawable.location, Color(0xFF4E4BEA))
        listOf("체험", "액티비티", "놀이").any(normalized::contains) -> CategoryVisual(R.drawable.stretching_exercises, Color(0xFFFF6659))
        listOf("쇼핑", "기념품", "마트").any(normalized::contains) -> CategoryVisual(R.drawable.grocery_store, Color(0xFFFF59CC))
        else -> CategoryVisual(R.drawable.three_dots, Color(0xFF6E7180))
    }
}

private fun formatDayLabel(date: String): String = date.replace('.', '/').trimEnd('/')
private fun Long.toNumber(): String = NumberFormat.getNumberInstance(Locale.KOREA).format(this)
private fun Long.toWon(): String = "KRW ${toNumber()}"

@Preview(showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun TravelLedgerPreview() {
    val participants = listOf(
        TravelParticipant("local-user", "나", "character_pca"),
        TravelParticipant("friend-1", "여행친구", "character_sca"),
    )
    val schedules = listOf(
        TravelSchedule(
            id = "schedule-1",
            tripId = "trip-1",
            title = "제주 카페 투어",
            date = "2026.08.13",
            time = "10:00",
            order = 0,
            endTime = "11:30",
        ),
        TravelSchedule(
            id = "schedule-2",
            tripId = "trip-1",
            title = "숙소 체크인",
            date = "2026.08.13",
            time = "15:00",
            order = 1,
            endTime = "16:00",
        ),
    )
    val expenses = listOf(
        TravelExpense("expense-1", "trip-1", "schedule-2", "호텔", "", 360_000, "local-user", participants.map { it.id }, "2026.08.13", "15:00"),
        TravelExpense("expense-2", "trip-1", "schedule-1", "카페", "", 33_000, "local-user", participants.map { it.id }, "2026.08.13", "10:30"),
        TravelExpense("expense-3", "trip-1", "schedule-1", "흑돼지 저녁", "", 114_000, "friend-1", participants.map { it.id }, "2026.08.12", "19:00"),
    )
    GayadiTheme {
        TravelLedgerScreen(
            tripName = "제주 여행",
            expenses = expenses,
            schedules = schedules,
            participants = participants,
            settlementSummary = ExpenseSettlementSummary(
                totalAmount = expenses.sumOf { it.amount },
                balances = listOf(
                    ParticipantExpenseBalance("local-user", 393_000, 253_500, 139_500),
                    ParticipantExpenseBalance("friend-1", 114_000, 253_500, -139_500),
                ),
                transfers = listOf(SettlementTransfer("friend-1", "local-user", 139_500)),
            ),
            onBack = {}, onAddExpense = {}, onEditExpense = { _, _ -> }, onDeleteExpense = {},
        )
    }
}

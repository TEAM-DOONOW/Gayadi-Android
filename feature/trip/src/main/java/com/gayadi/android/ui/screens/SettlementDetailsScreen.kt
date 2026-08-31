package com.gayadi.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gayadi.android.domain.model.ExpensePaymentSource
import com.gayadi.android.domain.model.TravelExpense
import com.gayadi.android.ui.theme.TextPrimary
import com.gayadi.android.ui.theme.TextSecondary
import java.text.NumberFormat
import java.util.Locale

@Composable
fun SettlementDetailsScreen(
    participantId: String,
    detailType: String,
    expenses: List<TravelExpense>,
    onBack: () -> Unit,
) {
    val isPaid = detailType == "paid"
    val details = remember(expenses, participantId, detailType) {
        expenses.mapNotNull { expense ->
            val amount = if (isPaid) {
                when {
                    expense.paymentSource == ExpensePaymentSource.PERSONAL && expense.payerId == participantId -> expense.amount
                    expense.paymentSource == ExpensePaymentSource.SHARED_FUND && participantId in expense.participantIds -> expense.shareFor(participantId)
                    else -> null
                }
            } else {
                expense.takeIf { participantId in it.participantIds }?.shareFor(participantId)
            }
            amount?.let { SettlementDetailItem(expense.id, expense.title, expense.date, it) }
        }
    }
    Column(Modifier.fillMaxSize().background(Color(0xFFF7F7F8)).statusBarsPadding()) {
        Row(
            Modifier.fillMaxWidth().padding(start = 8.dp, end = 20.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ChevronLeft, contentDescription = "뒤로")
            }
            Text(
                if (isPaid) "결제한 금액 내역" else "지출된 금액 내역",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
            )
        }
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp)) {
            Text("총 지출", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            Spacer(Modifier.size(5.dp))
            Text(
                details.sumOf(SettlementDetailItem::amount).toWon(),
                fontSize = 25.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
            )
        }
        if (details.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(bottom = 56.dp)) {
                    Box(
                        Modifier.size(54.dp).background(Color(0xFFE8ECF4), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ReceiptLong,
                            contentDescription = null,
                            tint = Color(0xFF666A78),
                            modifier = Modifier.size(26.dp),
                        )
                    }
                    Spacer(Modifier.size(14.dp))
                    Text("현재 내역이 없어요", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(details, key = SettlementDetailItem::id) { detail ->
                    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(detail.title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Spacer(Modifier.size(3.dp))
                                Text(detail.date, fontSize = 11.sp, color = TextSecondary)
                            }
                            Text(detail.amount.toWon(), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        }
                    }
                }
            }
        }
    }
}

private data class SettlementDetailItem(val id: String, val title: String, val date: String, val amount: Long)

private fun TravelExpense.shareFor(participantId: String): Long {
    val sortedIds = participantIds.sorted()
    val index = sortedIds.indexOf(participantId)
    if (index < 0) return 0L
    val base = amount / sortedIds.size
    return base + if (index < (amount % sortedIds.size).toInt()) 1L else 0L
}

private fun Long.toWon(): String = "KRW ${NumberFormat.getNumberInstance(Locale.KOREA).format(this)}"

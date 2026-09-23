package com.gayadi.android.notification

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gayadi.android.ui.theme.Background
import com.gayadi.android.ui.theme.TextPrimary
import com.gayadi.android.ui.theme.TextSecondary

internal data class ExpenseNotificationItem(
    val scheduleId: String,
    val title: String,
    val message: String,
)

internal fun loadExpenseNotifications(context: Context, tripId: String, scheduleIds: List<String>): List<ExpenseNotificationItem> {
    val scheduleByNotificationId = scheduleIds.associateBy { expenseReminderNotificationId(tripId, it) }
    return context.getSystemService(NotificationManager::class.java).activeNotifications
        .filter { it.notification.channelId == EXPENSE_REMINDER_CHANNEL_ID && it.id in scheduleByNotificationId }
        .sortedByDescending { it.postTime }
        .map { posted ->
            ExpenseNotificationItem(
                scheduleId = scheduleByNotificationId.getValue(posted.id),
                title = posted.notification.extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty(),
                message = posted.notification.extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty(),
            )
        }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ExpenseNotificationsBottomSheet(
    tripId: String,
    scheduleIds: List<String>,
    onDismiss: () -> Unit,
    onOpenExpense: (String) -> Unit,
) {
    val context = LocalContext.current
    var result by remember(tripId, scheduleIds) {
        mutableStateOf(runCatching { loadExpenseNotifications(context, tripId, scheduleIds) })
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Background,
        tonalElevation = 0.dp,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    ) {
        Text("알림", Modifier.padding(horizontal = 20.dp), fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        LazyColumn(contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 24.dp)) {
            when {
                result.isFailure -> item {
                    Text("알림을 불러오지 못했어요", color = TextSecondary)
                    TextButton(onClick = { result = runCatching { loadExpenseNotifications(context, tripId, scheduleIds) } }) {
                        Text("다시 시도")
                    }
                }
                result.getOrNull().isNullOrEmpty() -> item {
                    Text("새로운 알림이 없어요", Modifier.padding(vertical = 24.dp), color = TextSecondary)
                }
                else -> items(result.getOrThrow(), key = ExpenseNotificationItem::scheduleId) { notice ->
                    Column(
                        Modifier.fillMaxWidth().clickable(role = Role.Button) { onOpenExpense(notice.scheduleId) }
                            .padding(vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(notice.title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                        Text(notice.message, fontSize = 14.sp, color = TextSecondary)
                    }
                }
            }
        }
    }
}

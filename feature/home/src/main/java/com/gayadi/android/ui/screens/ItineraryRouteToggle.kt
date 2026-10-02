package com.gayadi.android.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.gayadi.android.ui.theme.PrimaryAction
import com.gayadi.android.ui.theme.TextTertiary

/**
 * 여행 계획 헤더에서 두 가지 일정 구성 방법을 설명하는 도움말입니다.
 */
@Composable
internal fun ItineraryRouteGuideButton(
    modifier: Modifier = Modifier,
) {
    var showHelp by remember { mutableStateOf(false) }
    var infoCenterFromEnd by remember { mutableStateOf(0) }
    var rowWidth by remember { mutableStateOf(0) }
    Row(
        modifier.onGloballyPositioned { rowWidth = it.size.width },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = { showHelp = true },
            modifier = Modifier
                .size(40.dp)
                .onGloballyPositioned {
                    val center = it.positionInParent().x + it.size.width / 2f
                    infoCenterFromEnd = (rowWidth - center).toInt()
                },
        ) {
            Icon(
                Icons.Outlined.Info,
                contentDescription = "여행 계획 방법 도움말",
                tint = TextTertiary,
                modifier = Modifier.size(18.dp),
            )
        }
        if (showHelp) {
            ItineraryRouteHelp(caretFromEndPx = infoCenterFromEnd, onDismiss = { showHelp = false })
        }
    }
}

@Composable
private fun ItineraryRouteHelp(caretFromEndPx: Int, onDismiss: () -> Unit) {
    val density = LocalDensity.current
    val caretHalfPx = with(density) { 7.dp.toPx() }
    Popup(
        alignment = Alignment.TopEnd,
        offset = IntOffset(x = 0, y = with(density) { 40.dp.roundToPx() }),
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true),
    ) {
        Column(
            Modifier
                .widthIn(max = 300.dp)
                .clickable(onClick = onDismiss)
                .semantics { contentDescription = "여행루트 도움말 닫기" },
            horizontalAlignment = Alignment.End,
        ) {
            // ⓘ를 가리키는 말풍선 꼬리
            Canvas(
                Modifier
                    .padding(end = with(density) { (caretFromEndPx - caretHalfPx).coerceAtLeast(0f).toDp() })
                    .size(width = 14.dp, height = 7.dp),
            ) {
                drawPath(
                    Path().apply {
                        moveTo(size.width / 2f, 0f)
                        lineTo(size.width, size.height)
                        lineTo(0f, size.height)
                        close()
                    },
                    color = PrimaryAction,
                )
            }
            Column(
                Modifier
                    .background(PrimaryAction, RoundedCornerShape(12.dp))
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text("어떻게 계획할까요?", style = MaterialTheme.typography.titleSmall, color = Color.White)
                Text(
                    "하나씩 고르기: 고른 장소를 기준으로 다음 후보를 이동시간순으로 추천해요.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White,
                )
                Text(
                    "모두 추천받기: 방문 순서와 머무는 시간까지 하루 동선을 한 번에 맞춰요.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White,
                )
                Text(
                    "눌러서 닫기",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.align(Alignment.End),
                )
            }
        }
    }
}

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gayadi.android.domain.model.Notice
import com.gayadi.android.domain.model.NoticeCategory
import com.gayadi.android.domain.model.NoticeSection
import com.gayadi.android.ui.components.GayadiLoadingScreen
import com.gayadi.android.ui.components.GayadiTopAppBar
import com.gayadi.android.ui.theme.GayadiTheme
import com.gayadi.android.ui.theme.TextPrimary
import com.gayadi.android.ui.theme.TextSecondary
import com.gayadi.android.ui.theme.TextTertiary

@Composable
fun NoticeListRoute(
    viewModel: NoticeListViewModel,
    onBack: () -> Unit,
    onOpenNotice: (String) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    NoticeListScreen(
        uiState = uiState,
        onBack = onBack,
        onOpenNotice = onOpenNotice,
        onRetry = viewModel::retry,
    )
}

@Composable
internal fun NoticeListScreen(
    uiState: NoticeListUiState,
    onBack: () -> Unit,
    onOpenNotice: (String) -> Unit,
    onRetry: () -> Unit,
) {
    if (uiState.isLoading) {
        GayadiLoadingScreen()
        return
    }

    Column(modifier = Modifier.fillMaxSize().background(Color.White)) {
        GayadiTopAppBar(title = "공지사항", onBack = onBack, showDivider = true)

        when {
            uiState.errorMessage != null -> NoticeListMessage(
                message = uiState.errorMessage,
                onRetry = onRetry,
            )
            uiState.isEmpty -> NoticeListMessage(
                message = "아직 등록된 업데이트 소식이 없어요",
                onRetry = null,
            )
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 12.dp, bottom = 16.dp),
            ) {
                items(uiState.notices, key = Notice::id) { notice ->
                    NoticeRow(notice = notice, onClick = { onOpenNotice(notice.id) })
                }
            }
        }
    }
}

@Composable
private fun NoticeListMessage(message: String, onRetry: (() -> Unit)?) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(message, fontSize = 14.sp, color = TextSecondary, textAlign = TextAlign.Center)
        onRetry?.let {
            Spacer(Modifier.height(16.dp))
            Button(onClick = it) { Text("다시 시도") }
        }
    }
}

@Composable
private fun NoticeRow(notice: Notice, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(Color.White)
            .padding(horizontal = 48.dp, vertical = 10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(notice.title, fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium, color = TextPrimary, maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Start)
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(notice.publishedAt.replace('-', '.'), fontSize = 12.sp, color = TextTertiary)
                    if (notice.isPinned) {
                        Spacer(Modifier.width(6.dp))
                        Box(Modifier.size(22.dp).clip(RoundedCornerShape(50)).background(Color(0xFFE5005A)), contentAlignment = Alignment.Center) {
                            Text("N", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
            Text("›", fontSize = 30.sp, color = TextPrimary)
        }
        Box(Modifier.fillMaxWidth().padding(top = 12.dp).height(1.dp).background(Color(0xFFE6E6E6)))
    }
}

@Composable
internal fun NoticeCategoryChip(category: NoticeCategory) {
    val background = Color(0xFFEAF1FF)
    val foreground = Color(0xFF3F65A8)
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(background)
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(category.label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = foreground)
    }
}

@Preview(showBackground = true)
@Composable
private fun NoticeListPreview() {
    GayadiTheme {
        NoticeListScreen(
            uiState = NoticeListUiState(
                notices = listOf(
                    Notice(
                        id = "1-2-0",
                        title = "여행 초대 코드로 바로 입장할 수 있어요",
                        category = NoticeCategory.UPDATE,
                        version = "1.2.0",
                        publishedAt = "2026-08-20",
                        summary = "친구가 보낸 초대 코드를 누르면 앱이 열리면서 곧바로 여행에 참여합니다.",
                        sections = listOf(NoticeSection("달라진 점", "초대 링크 지원")),
                        isPinned = true,
                    ),
                ),
                isLoading = false,
            ),
            onBack = {},
            onOpenNotice = {},
            onRetry = {},
        )
    }
}

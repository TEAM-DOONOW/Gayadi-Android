package com.gayadi.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import com.gayadi.android.ui.theme.Border
import com.gayadi.android.ui.theme.GayadiTheme
import com.gayadi.android.ui.theme.SurfaceCard
import com.gayadi.android.ui.theme.TagBlue
import com.gayadi.android.ui.theme.TagBlueText
import com.gayadi.android.ui.theme.TagGreen
import com.gayadi.android.ui.theme.TagGreenText
import com.gayadi.android.ui.theme.TagOrange
import com.gayadi.android.ui.theme.TagOrangeText
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
        GayadiTopAppBar(title = "업데이트", onBack = onBack, showDivider = true)

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
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(uiState.notices, key = Notice::id) { notice ->
                    NoticeCard(notice = notice, onClick = { onOpenNotice(notice.id) })
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
private fun NoticeCard(notice: Notice, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceCard)
            .border(1.dp, Border.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            NoticeCategoryChip(notice.category)
            if (notice.isPinned) {
                Spacer(Modifier.width(6.dp))
                Text("고정", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TextTertiary)
            }
            Spacer(Modifier.weight(1f))
            Text(notice.publishedAt, fontSize = 12.sp, color = TextTertiary)
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = notice.title,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = notice.summary,
            fontSize = 13.sp,
            lineHeight = 20.sp,
            color = TextSecondary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        notice.version?.let { version ->
            Spacer(Modifier.height(8.dp))
            Text("버전 $version", fontSize = 12.sp, color = TagBlueText)
        }
    }
}

@Composable
internal fun NoticeCategoryChip(category: NoticeCategory) {
    val (background, foreground) = when (category) {
        NoticeCategory.UPDATE -> TagBlue to TagBlueText
        NoticeCategory.NOTICE -> TagGreen to TagGreenText
        NoticeCategory.EVENT -> TagOrange to TagOrangeText
    }
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

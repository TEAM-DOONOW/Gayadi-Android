package com.gayadi.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
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
import com.gayadi.android.ui.theme.PrimaryBlue
import com.gayadi.android.ui.theme.TextPrimary
import com.gayadi.android.ui.theme.TextSecondary
import com.gayadi.android.ui.theme.TextTertiary

@Composable
fun NoticeDetailRoute(
    viewModel: NoticeDetailViewModel,
    onBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    NoticeDetailScreen(uiState = uiState, onBack = onBack, onRetry = viewModel::retry)
}

@Composable
internal fun NoticeDetailScreen(
    uiState: NoticeDetailUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
) {
    when {
        uiState.isLoading -> GayadiLoadingScreen()
        uiState.notice == null -> Column(
            modifier = Modifier.fillMaxSize().background(Color.White),
        ) {
            GayadiTopAppBar(title = "업데이트", onBack = onBack, showDivider = true)
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(uiState.errorMessage ?: "업데이트 소식을 불러오지 못했습니다.", color = TextSecondary)
                Spacer(Modifier.height(16.dp))
                Button(onClick = onRetry) { Text("다시 시도") }
            }
        }
        else -> {
            val notice = uiState.notice
            Column(modifier = Modifier.fillMaxSize().background(Color.White)) {
                GayadiTopAppBar(title = notice.title, onBack = onBack, showDivider = true)
                Column(
                    modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        NoticeCategoryChip(notice.category)
                        Spacer(Modifier.width(8.dp))
                        Text(notice.publishedAt, fontSize = 12.sp, color = TextTertiary)
                    }
                    Spacer(Modifier.height(14.dp))
                    Text(notice.summary, fontSize = 14.sp, lineHeight = 22.sp, color = TextSecondary)
                    notice.version?.let { version ->
                        Spacer(Modifier.height(12.dp))
                        Text("버전 $version", fontSize = 12.sp, color = PrimaryBlue)
                    }
                    Spacer(Modifier.height(24.dp))
                    notice.sections.forEach { section ->
                        Text(section.title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                        Spacer(Modifier.height(8.dp))
                        Text(section.body, fontSize = 14.sp, lineHeight = 22.sp, color = TextSecondary)
                        Spacer(Modifier.height(24.dp))
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun NoticeDetailPreview() {
    GayadiTheme {
        NoticeDetailScreen(
            uiState = NoticeDetailUiState(
                notice = Notice(
                    id = "1-2-0",
                    title = "여행 초대 코드로 바로 입장할 수 있어요",
                    category = NoticeCategory.UPDATE,
                    version = "1.2.0",
                    publishedAt = "2026-08-20",
                    summary = "친구가 보낸 초대 코드를 누르면 앱이 열리면서 곧바로 여행에 참여합니다.",
                    sections = listOf(
                        NoticeSection("달라진 점", "초대 링크를 누르면 참여 화면으로 바로 이동합니다."),
                        NoticeSection("참고", "이전 버전에서는 코드를 직접 입력해야 했어요."),
                    ),
                    isPinned = false,
                ),
                isLoading = false,
            ),
            onBack = {},
            onRetry = {},
        )
    }
}

package com.gayadi.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gayadi.android.ui.components.GayadiLoadingScreen
import com.gayadi.android.ui.components.GayadiTopAppBar
import com.gayadi.android.ui.theme.PrimaryBlue
import com.gayadi.android.ui.theme.TextPrimary
import com.gayadi.android.ui.theme.TextSecondary

@Composable
fun LegalDocumentRoute(
    viewModel: LegalDocumentViewModel,
    onBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LegalDocumentScreen(uiState = uiState, onBack = onBack, onRetry = viewModel::retry)
}

@Composable
internal fun LegalDocumentScreen(
    uiState: LegalDocumentUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
) {
    when {
        uiState.isLoading -> GayadiLoadingScreen()
        uiState.document == null -> Column(
            modifier = Modifier.fillMaxSize().background(Color.White),
        ) {
            LegalDocumentHeader(title = "법적 문서", onBack = onBack)
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(uiState.errorMessage ?: "문서를 불러오지 못했습니다.", color = TextSecondary)
                Spacer(Modifier.height(16.dp))
                Button(onClick = onRetry) { Text("다시 시도") }
            }
        }
        else -> {
            val document = uiState.document
            Column(modifier = Modifier.fillMaxSize().background(Color.White)) {
                LegalDocumentHeader(title = document.title.toDisplayTitle(), onBack = onBack)
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp),
                ) {
                    Spacer(Modifier.height(28.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("이전 내용 보기", fontSize = 13.sp, color = TextSecondary)
                        Spacer(Modifier.width(12.dp))
                        Row(
                            modifier = Modifier
                                .border(1.dp, Color(0xFF777777), RoundedCornerShape(10.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                document.effectiveDate.toKoreanDate(),
                                fontSize = 14.sp,
                                color = TextSecondary,
                            )
                            Spacer(Modifier.width(8.dp))
                            Icon(
                                Icons.Filled.UnfoldMore,
                                contentDescription = "시행일 선택",
                                tint = TextPrimary,
                            )
                        }
                    }
                    Spacer(Modifier.height(42.dp))
                    document.summary.takeIf(String::isNotBlank)?.let { summary ->
                        Text(summary, fontSize = 13.sp, lineHeight = 22.sp, color = TextSecondary)
                        Spacer(Modifier.height(28.dp))
                    }
                    document.sections.forEachIndexed { index, section ->
                        Text(
                            section.title,
                            fontSize = 18.sp,
                            lineHeight = 26.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                        )
                        Spacer(Modifier.height(26.dp))
                        Column(
                            modifier = Modifier.padding(start = 20.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            section.body.toLegalPoints().forEachIndexed { pointIndex, point ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.Top,
                                ) {
                                    Text(
                                        text = "${pointIndex + 1}.",
                                        fontSize = 13.sp,
                                        lineHeight = 22.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextPrimary,
                                        modifier = Modifier.width(24.dp),
                                    )
                                    Text(
                                        text = point,
                                        fontSize = 13.sp,
                                        lineHeight = 22.sp,
                                        color = TextSecondary,
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                            }
                        }
                        if (index != document.sections.lastIndex) {
                            Spacer(Modifier.height(28.dp))
                            HorizontalDivider(color = Color(0xFFEAEAEA))
                            Spacer(Modifier.height(28.dp))
                        }
                    }
                    Spacer(Modifier.height(28.dp))
                }
            }
        }
    }
}

@Composable
private fun LegalDocumentHeader(title: String, onBack: () -> Unit) {
    GayadiTopAppBar(title = title, onBack = onBack, showDivider = true)
}

private fun String.toDisplayTitle(): String = when {
    contains("개인정보") -> "개인정보 처리방침"
    contains("이용약관") -> "서비스 이용약관"
    else -> replaceFirst(Regex("^가야디\\s*"), "")
}

private fun String.toKoreanDate(): String {
    val parts = split("-")
    return if (parts.size == 3) {
        "${parts[0]}년 ${parts[1]}월 ${parts[2]}일"
    } else {
        this
    }
}

private fun String.toLegalPoints(): List<String> =
    lines()
        .flatMap { line -> line.trim().split(Regex("(?<=[.!?。])\\s+")) }
        .map(String::trim)
        .filter(String::isNotBlank)

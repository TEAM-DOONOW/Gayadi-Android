package com.gayadi.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gayadi.android.ui.components.GayadiLoadingScreen
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
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(uiState.errorMessage ?: "문서를 불러오지 못했습니다.", color = TextSecondary)
            Spacer(Modifier.height(16.dp))
            Button(onClick = onRetry) { Text("다시 시도") }
        }
        else -> {
            val document = uiState.document
            Column(modifier = Modifier.fillMaxSize().background(Color.White)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로")
                    }
                    Text(document.title, fontSize = 21.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                }
                HorizontalDivider(color = Color(0xFFE5E5E5))
                Column(
                    modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
                ) {
                    Text(document.summary, fontSize = 14.sp, lineHeight = 22.sp, color = TextSecondary)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "버전 ${document.version} · 시행일 ${document.effectiveDate}",
                        fontSize = 12.sp,
                        color = PrimaryBlue,
                    )
                    document.reviewNotice?.let { notice ->
                        Spacer(Modifier.height(12.dp))
                        Text(notice, fontSize = 12.sp, lineHeight = 18.sp, color = TextSecondary)
                    }
                    Spacer(Modifier.height(24.dp))
                    document.sections.forEach { section ->
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

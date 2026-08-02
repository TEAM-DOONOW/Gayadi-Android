package com.gayadi.android.feature.surveyresult.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gayadi.android.domain.model.SurveyResult
import com.gayadi.android.ui.theme.GayadiTheme
import com.gayadi.android.ui.theme.PrimaryBlue
import com.gayadi.android.ui.theme.TagBlue
import com.gayadi.android.ui.theme.TagBlueText
import com.gayadi.android.ui.theme.TagGreen
import com.gayadi.android.ui.theme.TagGreenText
import com.gayadi.android.ui.theme.TagPink
import com.gayadi.android.ui.theme.TagPinkText
import com.gayadi.android.ui.theme.TextPrimary
import com.gayadi.android.ui.theme.TextSecondary

@Composable
fun SurveyResultRoute(
    viewModel: SurveyResultViewModel,
    onStart: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    SurveyResultScreen(
        uiState = uiState,
        onRetry = viewModel::retry,
        onStart = onStart,
    )
}

@Composable
internal fun SurveyResultScreen(
    uiState: SurveyResultUiState,
    onRetry: () -> Unit,
    onStart: () -> Unit,
) {
    when {
        uiState.isLoading -> ResultLoadingScreen()
        uiState.result == null -> ResultErrorScreen(
            message = uiState.errorMessage ?: "결과를 불러오지 못했습니다.",
            onRetry = onRetry,
        )

        else -> ResultContent(result = uiState.result, onStart = onStart)
    }
}

@Composable
private fun ResultContent(result: SurveyResult, onStart: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().background(Color.White).verticalScroll(rememberScrollState()),
    ) {
        Text(
            text = "나의 여행 캐릭터",
            fontSize = 23.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
        )
        androidx.compose.material3.HorizontalDivider(color = Color(0xFFE5E5E5))
        Column(
            modifier = Modifier.padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            Box(
                modifier = Modifier.size(140.dp).clip(CircleShape).background(Color(0xFFF5F0E8)),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = result.emoji, fontSize = 72.sp)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "나의 여행 캐릭터는", fontSize = 14.sp, color = TextSecondary)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = result.name,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = PrimaryBlue,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = result.summary,
                fontSize = 14.sp,
                lineHeight = 21.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TagChip(result.code, TagBlue, TagBlueText)
                result.compatibleCode?.let { TagChip("잘 맞는 $it", TagGreen, TagGreenText) }
                result.oppositeCode?.let { TagChip("정반대 $it", TagPink, TagPinkText) }
            }
            result.traits?.let { traits ->
                Spacer(modifier = Modifier.height(24.dp))
                TraitCard(title = "이 유형은", items = listOf(traits))
            }
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = onStart,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(0.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
            ) {
                Text("가야디 시작하기", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun ResultLoadingScreen() {
    Box(
        modifier = Modifier.fillMaxSize().background(Color.White),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = PrimaryBlue)
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "여행 캐릭터를 찾고 있어요", color = TextSecondary)
        }
    }
}

@Composable
private fun ResultErrorScreen(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().background(Color.White).padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "결과를 불러오지 못했어요",
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = message, color = TextSecondary, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(20.dp))
        Button(onClick = onRetry, colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)) {
            Text("다시 시도")
        }
    }
}

@Composable
private fun TagChip(text: String, background: Color, textColor: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(background)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text(text = text, fontSize = 12.sp, color = textColor, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun TraitCard(title: String, items: List<String>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FB)),
    ) {
        Row(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                modifier = Modifier.width(72.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                items.forEach { item ->
                    Text(
                        text = "• $item",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        lineHeight = 20.sp,
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SurveyResultPreview() {
    GayadiTheme {
        ResultContent(
            result = SurveyResult(
                code = "SCA",
                emoji = "🔥",
                name = "도심 순삭 인싸",
                summary = "즉흥적으로 도시를 에너지 넘치게 누비는 인싸",
                traits = "계획은 최소, 그날 끌리는 곳으로 직행. 몸 사리지 않고 하루를 꽉 채움.",
                compatibleCode = "SNA",
                oppositeCode = "PNR",
            ),
            onStart = {},
        )
    }
}

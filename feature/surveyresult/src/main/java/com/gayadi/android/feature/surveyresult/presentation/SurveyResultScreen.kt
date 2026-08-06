package com.gayadi.android.feature.surveyresult.presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gayadi.android.domain.model.SurveyResult
import com.gayadi.android.ui.theme.GayadiTheme
import com.gayadi.android.ui.theme.PretendardFontFamily
import com.gayadi.android.ui.theme.PretendardSemiBoldFontFamily
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
@OptIn(ExperimentalLayoutApi::class)
private fun ResultContent(result: SurveyResult, onStart: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "여행 유형 검사 결과",
            fontSize = 21.sp,
            fontFamily = PretendardSemiBoldFontFamily,
            color = TextPrimary,
            modifier = Modifier.padding(horizontal = 20.dp),
        )
        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(
            color = Color(0xFFE5E5E5),
            modifier = Modifier.padding(horizontal = 20.dp),
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(28.dp))
            Box(
                modifier = Modifier
                    .size(208.dp)
                    .clip(RoundedCornerShape(0.dp))
                    .background(Color(0xFFF6F6F8)),
                contentAlignment = Alignment.Center,
            ) {
                val characterDrawable = characterDrawableFor(result.characterKey)
                if (characterDrawable == null) {
                    Text(text = result.emoji, fontSize = 72.sp)
                } else {
                    Image(
                        painter = painterResource(characterDrawable),
                        contentDescription = "${result.name} 캐릭터",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit,
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "나의 여행 유형은",
                fontSize = 14.sp,
                fontFamily = PretendardFontFamily,
                color = TextSecondary,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = result.name,
                fontSize = 24.sp,
                fontFamily = PretendardSemiBoldFontFamily,
                color = TextPrimary,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = result.summary,
                fontSize = 14.sp,
                lineHeight = 22.sp,
                fontFamily = PretendardFontFamily,
                color = TextSecondary,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(20.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TagChip(result.code)
                result.compatibleCode?.let { TagChip("잘 맞는 유형 · $it") }
                result.oppositeCode?.let { TagChip("반대 유형 · $it") }
            }
            result.traits?.let { traits ->
                Spacer(modifier = Modifier.height(28.dp))
                TraitCard(text = traits)
            }
            Spacer(modifier = Modifier.height(28.dp))
        }
        Button(
            onClick = onStart,
            modifier = Modifier.fillMaxWidth().height(55.dp),
            shape = RoundedCornerShape(0.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Black,
                contentColor = Color.White,
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
        ) {
            Text("가야디 시작하기", fontSize = 18.sp, fontFamily = PretendardSemiBoldFontFamily)
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
            CircularProgressIndicator(color = Color.Black)
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
        Button(onClick = onRetry, colors = ButtonDefaults.buttonColors(containerColor = Color.Black)) {
            Text("다시 시도")
        }
    }
}

@Composable
private fun TagChip(text: String) {
    Box(
        modifier = Modifier
            .border(1.dp, Color(0xFFD1D1D6), RoundedCornerShape(0.dp))
            .padding(horizontal = 10.dp, vertical = 7.dp),
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            color = TextPrimary,
            fontFamily = PretendardFontFamily,
        )
    }
}

@Composable
private fun TraitCard(text: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF6F6F8))
            .padding(18.dp),
    ) {
        Text(
            text = "이 유형은",
            fontSize = 15.sp,
            fontFamily = PretendardSemiBoldFontFamily,
            color = TextPrimary,
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = text,
            fontSize = 13.sp,
            lineHeight = 21.sp,
            fontFamily = PretendardFontFamily,
            color = TextSecondary,
        )
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
                characterKey = "character_sca",
            ),
            onStart = {},
        )
    }
}

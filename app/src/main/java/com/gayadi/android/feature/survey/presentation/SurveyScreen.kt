package com.gayadi.android.feature.survey.presentation

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gayadi.android.R
import com.gayadi.android.domain.model.SurveyQuestion
import com.gayadi.android.ui.theme.GayadiTheme
import com.gayadi.android.ui.theme.PretendardSemiBoldFontFamily
import com.gayadi.android.ui.theme.TextPrimary
import com.gayadi.android.ui.theme.TextSecondary

private val SurveyBlack = Color.Black
private val DisabledButton = Color(0xFFEDEDED)
private val DisabledButtonText = Color(0xFF9C9C9C)

@Composable
fun SurveyRoute(
    viewModel: SurveyViewModel,
    onComplete: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    SurveyScreen(
        uiState = uiState,
        onStart = viewModel::startSurvey,
        onOptionSelected = viewModel::selectOption,
        onNext = {
            if (viewModel.moveToNextQuestion()) onComplete()
        },
    )
}

@Composable
private fun SurveyScreen(
    uiState: SurveyUiState,
    onStart: () -> Unit,
    onOptionSelected: (Int) -> Unit,
    onNext: () -> Unit,
) {
    if (!uiState.hasStarted) {
        SurveyIntroScreen(
            questionCount = uiState.questions.size,
            onStart = onStart,
        )
        return
    }

    val question = uiState.currentQuestion ?: return
    val progress by animateFloatAsState(
        targetValue = uiState.progress,
        animationSpec = tween(durationMillis = 450),
        label = "surveyProgress",
    )
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "나의 여행 성향은?",
            fontSize = 21.sp,
            fontFamily = PretendardSemiBoldFontFamily,
            color = TextPrimary,
            modifier = Modifier.padding(horizontal = 20.dp),
        )
        Spacer(modifier = Modifier.height(18.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth().height(6.dp),
            color = SurveyBlack,
            trackColor = Color(0xFFF0F0F2),
        )
        Spacer(modifier = Modifier.height(44.dp))
        Text(
            text = "${uiState.currentIndex + 1}/${uiState.questions.size}",
            fontSize = 22.sp,
            fontFamily = PretendardSemiBoldFontFamily,
            color = TextSecondary,
            modifier = Modifier.padding(horizontal = 20.dp),
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = question.title.replace("\n", " "),
            fontSize = 20.sp,
            lineHeight = 28.sp,
            fontFamily = PretendardSemiBoldFontFamily,
            color = TextPrimary,
            modifier = Modifier.padding(horizontal = 20.dp),
        )
        Spacer(modifier = Modifier.height(30.dp))
        Column(
            modifier = Modifier.padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            question.options.forEachIndexed { index, option ->
                SurveyOptionCard(
                    text = option,
                    isSelected = uiState.selectedOption == index,
                    onClick = { onOptionSelected(index) },
                )
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        Box(modifier = Modifier.fillMaxWidth().height(300.dp)) {
            Image(
                painter = painterResource(R.drawable.thinking_ganadi),
                contentDescription = null,
                modifier = Modifier
                    .size(850.dp)
                    .align(Alignment.BottomEnd)
                    .offset(x = 60.dp),
                contentScale = ContentScale.Fit,
            )
        }
        Button(
            onClick = onNext,
            enabled = uiState.selectedOption != null,
            modifier = Modifier.fillMaxWidth().height(55.dp),
            shape = RoundedCornerShape(0.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = SurveyBlack,
                contentColor = Color.White,
                disabledContainerColor = DisabledButton,
                disabledContentColor = DisabledButtonText,
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
        ) {
            Text(
                text = if (uiState.isLastQuestion) "결과 보기" else "다음",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun SurveyIntroScreen(questionCount: Int, onStart: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Image(
                painter = painterResource(R.drawable.travel_ganadi),
                contentDescription = "가야디 캐릭터",
                modifier = Modifier.size(260.dp),
                contentScale = ContentScale.Fit,
            )
            Spacer(modifier = Modifier.height(30.dp))
            Text(
                text = "여행을 할 때 우리는 어떤 모습일까?",
                fontSize = 22.sp,
                lineHeight = 30.sp,
                fontFamily = PretendardSemiBoldFontFamily,
                color = TextPrimary,
            )
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "${questionCount}개의 질문으로\n나의 여행 유형을 찾아보세요",
                fontSize = 16.sp,
                lineHeight = 24.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center,
            )
        }
        Button(
            onClick = onStart,
            modifier = Modifier.fillMaxWidth().height(55.dp),
            shape = RoundedCornerShape(0.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Black, contentColor = Color.White),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
        ) {
            Text("테스트 시작하기", fontSize = 18.sp, fontFamily = PretendardSemiBoldFontFamily)
        }
    }
}

@Composable
private fun SurveyOptionCard(text: String, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(0.dp))
            .background(if (isSelected) SurveyBlack else Color(0xFFF6F6F8))
            .semantics { selected = isSelected }
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isSelected) Color.White else TextPrimary,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            textAlign = TextAlign.Center,
        )
    }
}

@Preview(showBackground = true, heightDp = 800)
@Composable
private fun SurveyPreview() {
    GayadiTheme {
        SurveyScreen(
            uiState = SurveyUiState(
                questions = listOf(SurveyQuestion(1, "여행을 가게 된다면?", listOf("계획한다", "즉흥적으로 떠난다"))),
                hasStarted = true,
            ),
            onStart = {},
            onOptionSelected = {},
            onNext = {},
        )
    }
}

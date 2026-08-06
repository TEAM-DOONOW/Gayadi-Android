package com.gayadi.android.feature.survey.presentation

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Timer
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gayadi.android.feature.survey.R
import com.gayadi.android.domain.model.SurveyOption
import com.gayadi.android.domain.model.SurveyQuestion
import com.gayadi.android.ui.theme.GayadiTheme
import com.gayadi.android.ui.theme.PretendardFontFamily
import com.gayadi.android.ui.theme.PretendardSemiBoldFontFamily
import com.gayadi.android.ui.theme.TextPrimary
import com.gayadi.android.ui.theme.TextSecondary

private val SurveyBlack = Color.Black
private val DisabledButton = Color(0xFFEDEDED)
private val DisabledButtonText = Color(0xFF9C9C9C)

private val SurveySpeechBubbleShape = GenericShape { size, _ ->
    val bodyBottom = size.height * 0.78f
    val radius = size.height * 0.18f
    moveTo(radius, 0f)
    lineTo(size.width - radius, 0f)
    quadraticTo(size.width, 0f, size.width, radius)
    lineTo(size.width, bodyBottom - radius)
    quadraticTo(size.width, bodyBottom, size.width - radius, bodyBottom)
    lineTo(size.width * 0.78f, bodyBottom)
    lineTo(size.width * 0.88f, size.height)
    lineTo(size.width * 0.62f, bodyBottom)
    lineTo(radius, bodyBottom)
    quadraticTo(0f, bodyBottom, 0f, bodyBottom - radius)
    lineTo(0f, radius)
    quadraticTo(0f, 0f, radius, 0f)
    close()
}

@Composable
/** Connects the survey ViewModel to its stateless Compose screen. */
fun SurveyRoute(
    viewModel: SurveyViewModel,
    onComplete: (String) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    SurveyScreen(
        uiState = uiState,
        onStart = { viewModel.onEvent(SurveyUiEvent.Start) },
        onOptionSelected = { viewModel.onEvent(SurveyUiEvent.OptionSelected(it)) },
        onRetry = { viewModel.onEvent(SurveyUiEvent.Retry) },
        onNext = {
            viewModel.onEvent(SurveyUiEvent.Next)?.let(onComplete)
        },
    )
}

/** Renders the survey for the supplied state without owning business state. */
@Composable
internal fun SurveyScreen(
    uiState: SurveyUiState,
    onStart: () -> Unit,
    onOptionSelected: (Int) -> Unit,
    onRetry: () -> Unit,
    onNext: () -> Unit,
) {
    if (uiState.isLoading) {
        SurveyLoadingScreen()
        return
    }

    if (uiState.isEmpty) {
        SurveyEmptyScreen(
            message = uiState.errorMessage ?: "잠시 후 다시 시도해주세요.",
            onRetry = onRetry,
        )
        return
    }

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
                    text = option.text,
                    isSelected = uiState.selectedOption == index,
                    onClick = { onOptionSelected(index) },
                )
            }
        }
        uiState.resultErrorMessage?.let { message ->
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
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
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(x = 100.dp, y = 84.dp)
                    .width(200.dp)
                    .height(100.dp)
                    .background(Color.White, SurveySpeechBubbleShape)
                    .border(1.dp, Color(0xFF9C9C9C), SurveySpeechBubbleShape),
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(78.dp).padding(horizontal = 18.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = surveyEncouragement(uiState.currentIndex, uiState.questions.size),
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        fontFamily = PretendardSemiBoldFontFamily,
                        color = TextPrimary,
                        textAlign = TextAlign.Center,
                    )
                }
            }
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

/** Displays progress while the Firestore survey is loading. */
@Composable
private fun SurveyLoadingScreen() {
    Box(
        modifier = Modifier.fillMaxSize().background(Color.White),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = SurveyBlack)
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "여행 성향 질문을 불러오고 있어요", color = TextSecondary)
        }
    }
}

/** Displays a recoverable state when survey content is unavailable. */
@Composable
private fun SurveyEmptyScreen(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "설문을 불러오지 못했어요",
            fontSize = 20.sp,
            fontFamily = PretendardSemiBoldFontFamily,
            color = TextPrimary,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onRetry,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(0.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Black,
                contentColor = Color.White,
            ),
        ) {
            Text("다시 시도", fontFamily = PretendardSemiBoldFontFamily)
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
            Spacer(modifier = Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Timer,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "1~2분 소요",
                    fontSize = 14.sp,
                    fontFamily = PretendardFontFamily,
                    color = TextSecondary,
                )
            }
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

private fun surveyEncouragement(currentIndex: Int, questionCount: Int): String {
    if (questionCount <= 0) return "편하게 골라보세요!"
    val progress = (currentIndex + 1f) / questionCount
    return when {
        currentIndex == questionCount - 1 -> "마지막 질문이에요!"
        progress >= 0.75f -> "거의 다 왔어요!"
        progress >= 0.5f -> "벌써 절반이나 왔어요!"
        progress >= 0.25f -> "좋아요, 나를 알아가는 중이에요!"
        else -> "편하게 골라보세요!"
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
                definition = com.gayadi.android.domain.model.SurveyDefinition(
                    id = "travel-personality-v1",
                    title = "여행 성향 판단 설문조사",
                    resultCodeOrder = listOf("preparation", "place", "energy"),
                    questions = listOf(
                        SurveyQuestion(
                            id = "q01",
                            order = 1,
                            dimension = "preparation",
                            title = "여행을 가게 된다면?",
                            options = listOf(
                                SurveyOption("a", "계획한다", "P"),
                                SurveyOption("b", "즉흥적으로 떠난다", "S"),
                            ),
                        ),
                    ),
                    results = emptyMap(),
                ),
                isLoading = false,
                hasStarted = true,
            ),
            onStart = {},
            onOptionSelected = {},
            onRetry = {},
            onNext = {},
        )
    }
}

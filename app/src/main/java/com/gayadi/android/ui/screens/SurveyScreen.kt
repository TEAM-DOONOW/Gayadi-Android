package com.gayadi.android.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gayadi.android.R
import com.gayadi.android.ui.theme.GayadiTheme
import com.gayadi.android.ui.theme.PretendardSemiBoldFontFamily
import com.gayadi.android.ui.theme.TextPrimary
import com.gayadi.android.ui.theme.TextSecondary

private val SurveyBlack = Color.Black
private val DisabledButton = Color(0xFFEDEDED)
private val DisabledButtonText = Color(0xFF9C9C9C)

private data class SurveyQuestion(
    val question: String,
    val description: String,
    val options: List<String>,
)

private val mockQuestions = listOf(
    SurveyQuestion(
        question = "여행을 가게 된다면\n가장 먼저 무엇을 하나요?",
        description = "평소 여행을 준비하는 방식을 골라주세요.",
        options = listOf(
            "여행 일정과 동선을 꼼꼼하게 계획한다.",
            "맛집이나 유명한 관광지를 먼저 찾아본다.",
            "숙소만 예약하고 나머지는 즉흥적으로 결정한다.",
            "같이 가는 사람들과 무엇을 할지 먼저 이야기한다.",
        ),
    ),
    SurveyQuestion(
        question = "여행 중 예상치 못한\n상황이 생기면 어떻게 하나요?",
        description = "가장 나다운 대처 방법을 선택해주세요.",
        options = listOf(
            "미리 세워둔 대안 일정을 바로 꺼낸다.",
            "현지인 추천이나 리뷰를 검색해 본다.",
            "그냥 흐름에 맡기고 즐긴다.",
            "동행들과 상의해서 함께 결정한다.",
        ),
    ),
    SurveyQuestion(
        question = "나에게 여행에서\n가장 중요한 것은 무엇인가요?",
        description = "한 가지만 고른다면 무엇인지 알려주세요.",
        options = listOf(
            "계획대로 움직이는 안정감",
            "새로운 경험과 발견",
            "편안한 휴식과 여유",
            "함께하는 사람들과의 시간",
        ),
    ),
)

@Composable
fun SurveyScreen(onComplete: () -> Unit) {
    var hasStarted by remember { mutableStateOf(false) }
    var currentIndex by remember { mutableIntStateOf(0) }
    var selectedOption by remember(currentIndex) { mutableStateOf<Int?>(null) }

    if (!hasStarted) {
        SurveyIntroScreen(onStart = { hasStarted = true })
        return
    }

    val question = mockQuestions[currentIndex]
    val progress by animateFloatAsState(
        targetValue = (currentIndex + 1f) / mockQuestions.size,
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
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp),
            color = SurveyBlack,
            trackColor = Color(0xFFF0F0F2),
        )

        Spacer(modifier = Modifier.height(44.dp))

        Text(
            text = "${currentIndex + 1}/${mockQuestions.size}",
            fontSize = 22.sp,
            fontFamily = PretendardSemiBoldFontFamily,
            color = TextSecondary,
            modifier = Modifier.padding(horizontal = 20.dp),
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = question.question.replace("\n", " "),
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
                    isSelected = selectedOption == index,
                    onClick = { selectedOption = index },
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp),
        ) {
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

        val isEnabled = selectedOption != null
        Button(
            onClick = {
                if (currentIndex < mockQuestions.lastIndex) {
                    currentIndex++
                } else {
                    onComplete()
                }
            },
            enabled = isEnabled,
            modifier = Modifier
                .fillMaxWidth()
                .height(55.dp),
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
                text = if (currentIndex < mockQuestions.lastIndex) "다음" else "결과 보기",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }

    }
}

@Composable
private fun SurveyIntroScreen(onStart: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 24.dp),
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
                text = "${mockQuestions.size}개의 질문으로\n나의 여행 유형을 찾아보세요",
                fontSize = 16.sp,
                lineHeight = 24.sp,
                color = TextSecondary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }

        Button(
            onClick = onStart,
            modifier = Modifier
                .fillMaxWidth()
                .height(55.dp),
            shape = RoundedCornerShape(0.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Black,
                contentColor = Color.White,
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
        ) {
            Text(
                text = "테스트 시작하기",
                fontSize = 18.sp,
                fontFamily = PretendardSemiBoldFontFamily,
            )
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
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}

@Preview(showBackground = true, heightDp = 800)
@Composable
private fun SurveyPreview() {
    GayadiTheme { SurveyScreen(onComplete = {}) }
}

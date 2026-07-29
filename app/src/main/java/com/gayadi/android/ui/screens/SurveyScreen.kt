package com.gayadi.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gayadi.android.ui.theme.GayadiTheme
import com.gayadi.android.ui.theme.PrimaryBlue
import com.gayadi.android.ui.theme.TextPrimary
import com.gayadi.android.ui.theme.TextSecondary

private data class SurveyQuestion(
    val question: String,
    val options: List<String>,
)

private val mockQuestions = listOf(
    SurveyQuestion(
        "여행을 가게 된다면 먼저 할 일은?",
        listOf(
            "여행 일정과 동선을 꼼꼼하게 계획한다.",
            "맛집이나 유명한 관광지를 먼저 찾아본다.",
            "숙소만 예약하고 나머지는 즉흥적으로 결정한다.",
            "같이 가는 사람들의 무엇을 할지 먼저 이야기한다.",
        ),
    ),
    SurveyQuestion(
        "여행 중 예상치 못한 상황이 생기면?",
        listOf(
            "미리 세워둔 대안 일정을 바로 꺼낸다.",
            "현지인 추천이나 리뷰를 검색해 본다.",
            "그냥 흐름에 맡기고 즐긴다.",
            "동행들과 상의해서 함께 결정한다.",
        ),
    ),
    SurveyQuestion(
        "여행에서 가장 중요한 것은?",
        listOf(
            "계획대로 움직이는 안정감",
            "새로운 경험과 발견",
            "편안한 휴식과 여유",
            "함께하는 사람들과의 시간",
        ),
    ),
)

@Composable
fun SurveyScreen(onComplete: () -> Unit) {
    var currentIndex by remember { mutableIntStateOf(0) }
    var selectedOption by remember { mutableStateOf<Int?>(null) }

    val question = mockQuestions[currentIndex]
    val progress = (currentIndex + 1f) / mockQuestions.size

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(horizontal = 24.dp),
    ) {
        Spacer(modifier = Modifier.height(60.dp))

        Text(
            text = "나의 여행 성향은?",
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            color = TextSecondary,
        )

        Spacer(modifier = Modifier.height(16.dp))

        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp)),
            color = PrimaryBlue,
            trackColor = Color(0xFFE5E5EA),
        )

        Spacer(modifier = Modifier.height(40.dp))

        Text(
            text = question.question,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
        )

        Spacer(modifier = Modifier.height(24.dp))

        question.options.forEachIndexed { index, option ->
            val isSelected = selectedOption == index
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) PrimaryBlue else Color(0xFFE0E0E0),
                        shape = RoundedCornerShape(12.dp),
                    )
                    .background(if (isSelected) Color(0xFFEBF5FF) else Color.White)
                    .clickable { selectedOption = index }
                    .padding(horizontal = 16.dp, vertical = 16.dp),
            ) {
                Text(
                    text = option,
                    fontSize = 14.sp,
                    color = if (isSelected) PrimaryBlue else TextPrimary,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = {
                if (currentIndex < mockQuestions.size - 1) {
                    currentIndex++
                    selectedOption = null
                } else {
                    onComplete()
                }
            },
            enabled = selectedOption != null,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
        ) {
            Text(
                text = if (currentIndex < mockQuestions.size - 1) "다음" else "결과 보기",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Preview(showBackground = true)
@Composable
private fun SurveyPreview() {
    GayadiTheme { SurveyScreen(onComplete = {}) }
}

package com.gayadi.android.ui.screens

import androidx.compose.animation.Crossfade
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Weekend
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gayadi.android.R
import com.gayadi.android.ui.theme.GayadiTheme
import com.gayadi.android.ui.theme.PretendardFontFamily
import com.gayadi.android.ui.theme.PretendardSemiBoldFontFamily
import com.gayadi.android.ui.theme.TextSecondary
import kotlinx.coroutines.delay

private val LoginPaper = Color(0xFFFBF8F2)

private val CharacterSpeechBubbleShape = GenericShape { size, _ ->
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

private data class Recommendation(
    val title: String,
    val message: String,
    val icon: ImageVector,
    val iconTint: Color,
)

private val recommendations = listOf(
    Recommendation(
        title = "오늘 서울은 비가 올 예정이에요",
        message = "실내 데이트 코스를 추천해줄게!",
        icon = Icons.Rounded.Cloud,
        iconTint = Color(0xFFAEC9D8),
    ),
    Recommendation(
        title = "이번 주말은 날씨가 맑아요",
        message = "가볍게 떠나는 나들이 코스 어때요?",
        icon = Icons.Rounded.Weekend,
        iconTint = Color(0xFF8DBA9D),
    ),
    Recommendation(
        title = "저녁 약속을 고민하고 있나요?",
        message = "분위기 좋은 맛집 코스를 골라봤어요",
        icon = Icons.Rounded.Restaurant,
        iconTint = Color(0xFFE7A183),
    ),
)

@Composable
fun LoginScreen(onStart: () -> Unit) {
    var currentRecommendation by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(3_500)
            currentRecommendation = (currentRecommendation + 1) % recommendations.size
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LoginPaper),
    ) {
        Image(
            painter = painterResource(R.drawable.login_travel_background_no_plane),
            contentDescription = null,
            modifier = Modifier.fillMaxSize().offset(y = (-12).dp),
            contentScale = ContentScale.Fit,
            alpha = 0.68f,
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(modifier = Modifier.height(20.dp))
                Image(
                    painter = painterResource(R.drawable.gayadi_text_v2),
                    contentDescription = "Gayadi",
                    modifier = Modifier.fillMaxWidth(0.9f).height(110.dp),
                    contentScale = ContentScale.Fit,
                )
                Text(
                    text = buildAnnotatedString {
                        append("오늘은 어디 갈까?\n")
                        withStyle(SpanStyle(color = Color.Black, fontWeight = FontWeight.Bold)) {
                            append("가야디")
                        }
                        append("와 같이 떠나보자! 🐾")
                    },
                    fontFamily = PretendardFontFamily,
                    fontSize = 16.sp,
                    lineHeight = 18.sp,
                    textAlign = TextAlign.Center,
                    color = Color(0xFF202124),
                )
                Spacer(modifier = Modifier.height(2.dp))
                Box(
                    modifier = Modifier.fillMaxWidth().height(292.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        painter = painterResource(R.drawable.ganadi),
                        contentDescription = "가야디",
                        modifier = Modifier.size(274.dp).offset(y = 54.dp),
                        contentScale = ContentScale.Fit,
                    )
                    Text(
                        text = "오늘은\n어디 갈까?",
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .offset(x = 2.dp, y = 34.dp)
                            .width(128.dp)
                            .height(100.dp)
                            .background(Color.White, CharacterSpeechBubbleShape)
                            .border(1.dp, Color(0xFF9C9C9C), CharacterSpeechBubbleShape)
                            .padding(start = 20.dp, top = 14.dp, end = 20.dp, bottom = 26.dp),
                        fontFamily = PretendardSemiBoldFontFamily,
                        fontSize = 15.sp,
                        lineHeight = 22.sp,
                        textAlign = TextAlign.Center,
                        color = Color(0xFF282828),
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(LoginPaper.copy(alpha = 0.96f)),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Crossfade(
                    targetState = currentRecommendation,
                    animationSpec = tween(durationMillis = 500),
                    label = "recommendationCard",
                ) { index ->
                    WeatherCard(
                        recommendation = recommendations[index],
                        onClick = {
                            currentRecommendation = (currentRecommendation + 1) % recommendations.size
                        },
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    recommendations.indices.forEach { index ->
                        Icon(
                            imageVector = Icons.Filled.Circle,
                            contentDescription = if (index == currentRecommendation) "${index + 1}번째 추천" else null,
                            tint = if (index == currentRecommendation) Color.Black else Color(0xFFD4D4D4),
                            modifier = Modifier.size(10.dp),
                        )
                    }
                }
                Spacer(modifier = Modifier.height(34.dp))
                Button(
                    onClick = onStart,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(2.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Black, contentColor = Color.White),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                ) {
                    Text(text = "시작하기", fontSize = 15.sp, fontFamily = PretendardSemiBoldFontFamily)
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "로그인하고 나만의 여행을 저장해요!",
                    fontFamily = PretendardFontFamily,
                    fontSize = 12.sp,
                    color = TextSecondary,
                )
            }
        }
    }
}

@Composable
private fun WeatherCard(recommendation: Recommendation, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.9f), RoundedCornerShape(18.dp))
            .border(1.dp, Color(0xFFBFC1C3), RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = recommendation.icon,
            contentDescription = null,
            tint = recommendation.iconTint,
            modifier = Modifier.size(46.dp),
        )
        Column(modifier = Modifier.weight(1f).padding(horizontal = 14.dp)) {
            Text(
                text = recommendation.title,
                fontFamily = PretendardFontFamily,
                fontSize = 13.sp,
                color = Color(0xFF282828),
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = recommendation.message,
                fontFamily = PretendardSemiBoldFontFamily,
                fontSize = 14.sp,
                color = Color(0xFF282828),
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = "추천 보기",
            tint = Color.Black,
            modifier = Modifier.size(24.dp),
        )
    }
}

@Preview(showBackground = true, heightDp = 800)
@Composable
private fun LoginPreview() {
    GayadiTheme { LoginScreen(onStart = {}) }
}

package com.gayadi.android.feature.surveyresult.presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gayadi.android.domain.model.CompatibleTravelType
import com.gayadi.android.domain.model.SurveyResult
import com.gayadi.android.domain.model.TravelRole
import com.gayadi.android.ui.components.GayadiLoadingScreen
import com.gayadi.android.ui.components.TravelResultDetails
import com.gayadi.android.ui.components.rememberMinimumLoadingVisibility
import com.gayadi.android.ui.theme.GayadiTheme
import com.gayadi.android.ui.theme.PretendardFontFamily
import com.gayadi.android.ui.theme.PretendardSemiBoldFontFamily
import com.gayadi.android.ui.theme.PrimaryAction
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
    val showLoading = rememberMinimumLoadingVisibility(uiState.isLoading)
    when {
        showLoading -> GayadiLoadingScreen()
        uiState.result == null -> ResultErrorScreen(
            message = uiState.errorMessage ?: "결과를 불러오지 못했습니다.",
            onRetry = onRetry,
        )

        else -> ResultContent(
            result = uiState.result,
            nickname = uiState.nickname,
            introduction = uiState.introduction,
            onStart = onStart,
        )
    }
}

@Composable
private fun ResultContent(
    result: SurveyResult,
    nickname: String?,
    introduction: String?,
    onStart: () -> Unit,
) {
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
                .verticalScroll(rememberScrollState()),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(modifier = Modifier.height(28.dp))
                Box(
                    modifier = Modifier.size(100.dp).clip(CircleShape).background(Color(0xFFF3F3F5)),
                    contentAlignment = Alignment.Center,
                ) {
                    val characterDrawable = characterDrawableFor(result.characterKey)
                    Image(
                        painter = painterResource(characterDrawable),
                        contentDescription = "${result.name} 캐릭터",
                        modifier = Modifier.size(80.dp),
                        contentScale = ContentScale.Fit,
                    )
                }
                Spacer(modifier = Modifier.height(18.dp))
                Text(
                    text = nickname ?: "여행자",
                    fontSize = 18.sp,
                    fontFamily = PretendardSemiBoldFontFamily,
                    color = TextPrimary,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = introduction ?: "나만의 방식으로 여행을 즐기는 여행자예요.",
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    fontFamily = PretendardFontFamily,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(28.dp))
            }

            Box(
                modifier = Modifier.fillMaxWidth().height(12.dp).background(Color(0xFFF0F0F5)),
            )

            Box(
                modifier = Modifier.fillMaxWidth().background(Color(0xFFF0F0F5)),
            ) {
                val sectionShape = RoundedCornerShape(topStart = 48.dp, topEnd = 48.dp)
                Card(
                    modifier = Modifier.fillMaxWidth().clip(sectionShape),
                    shape = sectionShape,
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Spacer(modifier = Modifier.height(30.dp))
                        Text(
                            text = result.name,
                            fontSize = 22.sp,
                            lineHeight = 30.sp,
                            fontFamily = PretendardSemiBoldFontFamily,
                            color = TextPrimary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = result.summary.asBalancedTwoLines(),
                            fontSize = 14.sp,
                            lineHeight = 22.sp,
                            fontFamily = PretendardFontFamily,
                            color = TextSecondary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.widthIn(max = 270.dp),
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        TravelResultDetails(result)
                        Spacer(modifier = Modifier.height(28.dp))
                    }
                }
            }
        }
        Button(
            onClick = onStart,
            modifier = Modifier.fillMaxWidth().height(55.dp),
            shape = RectangleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = PrimaryAction,
                contentColor = Color.White,
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
        ) {
            Text("가야디 시작하기", fontSize = 16.sp, fontFamily = PretendardSemiBoldFontFamily)
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
        Button(
            onClick = onRetry,
            shape = RectangleShape,
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryAction),
        ) {
            Text("다시 시도")
        }
    }
}

/** Greets the user by nickname, falling back to a neutral line before onboarding is filled in. */
private fun greetingFor(nickname: String?): String =
    if (nickname.isNullOrBlank()) "나의 여행 유형은" else "${nickname}님의 여행 유형은"

/** Breaks a single-sentence summary at the word boundary closest to its midpoint. */
private fun String.asBalancedTwoLines(): String {
    val midpoint = length / 2
    val breakAt = indices
        .filter { this[it].isWhitespace() }
        .minByOrNull { kotlin.math.abs(it - midpoint) }
        ?: return this
    return replaceRange(breakAt, breakAt + 1, "\n")
}

@Preview(showBackground = true)
@Composable
private fun SurveyResultPreview() {
    GayadiTheme {
        ResultContent(
            result = SurveyResult(
                code = "SCA",
                emoji = "🔥",
                name = "그날 끌리는 대로, 인싸잉",
                summary = "계획은 최소로, 눈앞의 도시를 에너지 넘치게 누비는 여행 스타일",
                hashtags = listOf("# 즉흥 출발", "# 도시 탐험", "# 하루 꽉꽉", "# 에너지 만렙"),
                strengths = listOf(
                    "그날의 분위기에 맞춰 바로 움직여요.",
                    "처음 가는 골목도 망설이지 않아요.",
                    "현지에서 얻은 정보를 잘 활용해요.",
                    "밤낮없이 하루를 꽉 채워요.",
                ),
                weaknesses = listOf(
                    "인기 장소는 대기가 길 수 있어요.",
                    "동선이 겹쳐 시간을 낭비할 수 있어요.",
                    "예산을 초과하기 쉬워요.",
                    "무리한 일정으로 다음 날이 힘들어져요.",
                ),
                characterKey = "character_sca",
                compatibleTypes = listOf(
                    CompatibleTravelType("PCA", "🏙️", "도시 구석까지 훑어, 완주잉"),
                    CompatibleTravelType("SNA", "🏄", "일단 뛰어들어, 파도잉"),
                ),
                travelRole = TravelRole(
                    icon = "⚡",
                    title = "분위기 점화 담당",
                    description = "새로운 제안을 먼저 꺼내고 모두가 움직일 에너지를 만들어요.",
                ),
            ),
            nickname = "민수",
            introduction = "새로운 곳을 찾아 즐겁게 여행하는 여행자예요.",
            onStart = {},
        )
    }
}

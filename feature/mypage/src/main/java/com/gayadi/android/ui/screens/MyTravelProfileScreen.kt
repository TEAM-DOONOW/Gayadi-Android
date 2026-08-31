package com.gayadi.android.ui.screens

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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gayadi.android.domain.model.UserProfile
import com.gayadi.android.ui.components.UserCharacterAvatar
import com.gayadi.android.ui.components.GayadiLoadingScreen
import com.gayadi.android.ui.components.GayadiTopAppBar
import com.gayadi.android.ui.components.TravelResultDetails
import com.gayadi.android.ui.theme.PrimaryAction
import com.gayadi.android.ui.theme.TextPrimary
import com.gayadi.android.ui.theme.TextSecondary

@Composable
fun MyTravelProfileScreen(
    uiState: ProfileUiState,
    resultUiState: TravelProfileResultUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onResultRetry: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().background(Color.White)) {
        GayadiTopAppBar(title = "내 여행 프로필", onBack = onBack)

        when {
            uiState.isLoading || (uiState.profile != null && resultUiState.isLoading) -> {
                GayadiLoadingScreen(modifier = Modifier.weight(1f))
            }
            uiState.errorMessage != null -> ProfileUnavailable(
                message = uiState.errorMessage,
                onRetry = onRetry,
                modifier = Modifier.weight(1f),
            )
            uiState.profile == null -> ProfileUnavailable(
                message = "저장된 여행 프로필이 없어요.",
                onRetry = onRetry,
                modifier = Modifier.weight(1f),
            )
            resultUiState.result == null -> ProfileUnavailable(
                message = resultUiState.errorMessage ?: "여행 유형 결과를 불러오지 못했어요.",
                onRetry = onResultRetry,
                modifier = Modifier.weight(1f),
            )
            else -> TravelProfileContent(
                profile = uiState.profile,
                result = resultUiState.result,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun TravelProfileContent(
    profile: UserProfile,
    result: com.gayadi.android.domain.model.SurveyResult,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(32.dp))
            UserCharacterAvatar(
                characterKey = profile.characterKey,
                contentDescription = "${profile.nickname} 여행 캐릭터",
                modifier = Modifier.size(104.dp),
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = profile.nickname,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = profile.introduction.takeIf(String::isNotBlank)
                    ?: "나만의 방식으로 여행을 즐기는 여행자예요.",
                fontSize = 14.sp,
                lineHeight = 21.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(30.dp))
        }
        Box(
            modifier = Modifier.fillMaxWidth().height(12.dp).background(Color(0xFFF0F0F5)),
        )
        Box(
            modifier = Modifier.fillMaxWidth().background(Color(0xFFF0F0F5)),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                    .background(Color.White)
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(modifier = Modifier.height(28.dp))
                Text(
                    text = result.name,
                    fontSize = 21.sp,
                    lineHeight = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = result.summary,
                    fontSize = 14.sp,
                    lineHeight = 22.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(26.dp))
                TravelResultDetails(result)
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun ProfileUnavailable(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("여행 프로필을 불러올 수 없어요", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        Spacer(modifier = Modifier.height(8.dp))
        Text(message, fontSize = 13.sp, color = TextSecondary, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(20.dp))
        Button(
            onClick = onRetry,
            shape = RectangleShape,
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryAction),
        ) {
            Text("다시 시도", color = Color.White)
        }
    }
}

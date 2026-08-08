package com.gayadi.android.feature.basicinfo.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gayadi.android.ui.theme.GayadiTheme
import com.gayadi.android.ui.theme.PretendardSemiBoldFontFamily
import com.gayadi.android.ui.theme.PrimaryAction
import com.gayadi.android.ui.theme.TextPrimary
import com.gayadi.android.ui.theme.TextSecondary
import com.gayadi.android.ui.components.GayadiCompactTextField

@Composable
/** Connects the basic information ViewModel to its stateless screen. */
fun BasicInfoRoute(
    viewModel: BasicInfoViewModel,
    onStartSurvey: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(uiState.saveCompleted) {
        if (uiState.saveCompleted) {
            viewModel.consumeSaveCompleted()
            onStartSurvey()
        }
    }
    BasicInfoScreen(
        uiState = uiState,
        onNicknameChanged = { viewModel.onEvent(BasicInfoUiEvent.NicknameChanged(it)) },
        onIntroductionChanged = { viewModel.onEvent(BasicInfoUiEvent.IntroductionChanged(it)) },
        onSubmit = { viewModel.onEvent(BasicInfoUiEvent.Submit) },
    )
}

@Composable
private fun BasicInfoScreen(
    uiState: BasicInfoUiState,
    onNicknameChanged: (String) -> Unit,
    onIntroductionChanged: (String) -> Unit,
    onSubmit: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "기본 정보 입력",
            fontSize = 21.sp,
            fontFamily = PretendardSemiBoldFontFamily,
            color = TextPrimary,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(color = Color(0xFFE5E5E5))
        Spacer(modifier = Modifier.height(31.dp))
        Text(
            text = "닉네임",
            fontSize = 17.sp,
            fontFamily = PretendardSemiBoldFontFamily,
            color = TextPrimary,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(6.dp))
        GayadiCompactTextField(
            label = "닉네임",
            value = uiState.nickname,
            onValueChange = onNicknameChanged,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "${uiState.nickname.length}/10",
            modifier = Modifier.fillMaxWidth(),
            color = TextSecondary,
            fontSize = 12.sp,
            textAlign = TextAlign.End,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "한 줄 소개",
            fontSize = 17.sp,
            fontFamily = PretendardSemiBoldFontFamily,
            color = TextPrimary,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(6.dp))
        GayadiCompactTextField(
            label = "한 줄 소개",
            value = uiState.introduction,
            onValueChange = onIntroductionChanged,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "${uiState.introduction.length}/20",
            modifier = Modifier.fillMaxWidth(),
            color = TextSecondary,
            fontSize = 12.sp,
            textAlign = TextAlign.End,
        )
        uiState.errorMessage?.let { message ->
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                color = Color(0xFFD32F2F),
                fontSize = 13.sp,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        Button(
            onClick = onSubmit,
            enabled = uiState.canSubmit,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(0.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = PrimaryAction,
                contentColor = Color.White,
                disabledContainerColor = Color(0xFFEDEDED),
                disabledContentColor = Color(0xFF9C9C9C),
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
        ) {
            Text(
                text = if (uiState.isSaving) "저장 중..." else "여행 유형 검사하러 가기",
                fontSize = 16.sp,
                fontFamily = PretendardSemiBoldFontFamily,
            )
        }
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Preview(showBackground = true)
@Composable
private fun BasicInfoPreview() {
    GayadiTheme {
        BasicInfoScreen(BasicInfoUiState(), {}, {}, {})
    }
}

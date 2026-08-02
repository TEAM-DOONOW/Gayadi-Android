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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gayadi.android.ui.theme.GayadiTheme
import com.gayadi.android.ui.theme.PretendardSemiBoldFontFamily
import com.gayadi.android.ui.theme.TextPrimary
import com.gayadi.android.ui.theme.TextSecondary

@Composable
fun BasicInfoRoute(
    viewModel: BasicInfoViewModel,
    onStartSurvey: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    BasicInfoScreen(
        uiState = uiState,
        onNicknameChanged = viewModel::onNicknameChanged,
        onIntroductionChanged = viewModel::onIntroductionChanged,
        onSubmit = {
            if (viewModel.submit()) onStartSurvey()
        },
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
        CompactTextField(
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
        CompactTextField(
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
        Spacer(modifier = Modifier.weight(1f))
        Button(
            onClick = onSubmit,
            enabled = uiState.canSubmit,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(0.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Black,
                contentColor = Color.White,
                disabledContainerColor = Color(0xFFEDEDED),
                disabledContentColor = Color(0xFF9C9C9C),
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
        ) {
            Text(
                text = "여행 유형 검사하러 가기",
                fontSize = 16.sp,
                fontFamily = PretendardSemiBoldFontFamily,
            )
        }
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun CompactTextField(
    value: String,
    onValueChange: (String) -> Unit,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
) {
    var isFocused by remember { mutableStateOf(false) }
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .onFocusChanged { isFocused = it.isFocused },
        singleLine = true,
        textStyle = TextStyle(
            fontFamily = PretendardSemiBoldFontFamily,
            fontSize = 13.sp,
            color = TextPrimary,
        ),
        keyboardOptions = keyboardOptions,
        cursorBrush = SolidColor(Color.Black),
        decorationBox = { innerTextField ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .border(
                        width = 1.dp,
                        color = if (isFocused) Color(0xFF8E8E93) else Color(0xFFD1D1D6),
                        shape = RoundedCornerShape(0.dp),
                    )
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                innerTextField()
            }
        },
    )
}

@Preview(showBackground = true)
@Composable
private fun BasicInfoPreview() {
    GayadiTheme {
        BasicInfoScreen(BasicInfoUiState(), {}, {}, {})
    }
}

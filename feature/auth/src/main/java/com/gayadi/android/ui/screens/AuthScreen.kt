package com.gayadi.android.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gayadi.android.feature.auth.R
import com.gayadi.android.ui.theme.GayadiTheme
import com.gayadi.android.ui.theme.PretendardFontFamily
import com.gayadi.android.ui.theme.PretendardSemiBoldFontFamily
import com.gayadi.android.ui.theme.PrimaryAction
import com.gayadi.android.ui.theme.TextPrimary
import com.gayadi.android.ui.theme.TextSecondary

@Composable
fun AuthRoute(
    viewModel: AuthViewModel,
    onAuthenticated: (AuthCompletion) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(uiState.completion) {
        uiState.completion?.let { completion ->
            viewModel.consumeCompletion()
            onAuthenticated(completion)
        }
    }
    AuthScreen(
        uiState = uiState,
        onModeSelected = viewModel::selectMode,
        onEmailChanged = viewModel::updateEmail,
        onPasswordChanged = viewModel::updatePassword,
        onNicknameChanged = viewModel::updateNickname,
        onSubmit = viewModel::submit,
    )
}

@Composable
private fun AuthScreen(
    uiState: AuthUiState,
    onModeSelected: (AuthMode) -> Unit,
    onEmailChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onNicknameChanged: (String) -> Unit,
    onSubmit: () -> Unit,
) {
    var passwordVisible by remember { mutableStateOf(false) }
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = PrimaryAction,
        unfocusedBorderColor = Color(0xFFD9D9DE),
        focusedLabelColor = PrimaryAction,
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(42.dp))
        Image(
            painter = painterResource(R.drawable.gayadi_text_v2),
            contentDescription = "Gayadi",
            modifier = Modifier.fillMaxWidth(0.58f).height(74.dp),
            contentScale = ContentScale.Fit,
        )
        Text(
            text = "여행을 함께 계획하고 기록해요",
            fontFamily = PretendardFontFamily,
            fontSize = 14.sp,
            color = TextSecondary,
        )
        Spacer(Modifier.height(34.dp))
        TabRow(
            selectedTabIndex = if (uiState.mode == AuthMode.LOGIN) 0 else 1,
            containerColor = Color.Transparent,
            contentColor = PrimaryAction,
        ) {
            AuthMode.entries.forEach { mode ->
                Tab(
                    selected = uiState.mode == mode,
                    onClick = { onModeSelected(mode) },
                    text = {
                        Text(
                            text = if (mode == AuthMode.LOGIN) "로그인" else "회원가입",
                            fontFamily = PretendardSemiBoldFontFamily,
                        )
                    },
                )
            }
        }
        Spacer(Modifier.height(24.dp))
        OutlinedTextField(
            value = uiState.email,
            onValueChange = onEmailChanged,
            label = { Text("이메일") },
            singleLine = true,
            enabled = !uiState.isSubmitting,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next,
            ),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(4.dp),
            colors = fieldColors,
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = uiState.password,
            onValueChange = onPasswordChanged,
            label = { Text("비밀번호") },
            supportingText = if (uiState.mode == AuthMode.SIGN_UP) {
                { Text("6~72자") }
            } else null,
            singleLine = true,
            enabled = !uiState.isSubmitting,
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = if (passwordVisible) "비밀번호 숨기기" else "비밀번호 보기",
                    )
                }
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = if (uiState.mode == AuthMode.LOGIN) ImeAction.Done else ImeAction.Next,
            ),
            keyboardActions = KeyboardActions(onDone = { onSubmit() }),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(4.dp),
            colors = fieldColors,
        )
        if (uiState.mode == AuthMode.SIGN_UP) {
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = uiState.nickname,
                onValueChange = onNicknameChanged,
                label = { Text("닉네임") },
                supportingText = { Text("${uiState.nickname.length}/10") },
                singleLine = true,
                enabled = !uiState.isSubmitting,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onSubmit() }),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(4.dp),
                colors = fieldColors,
            )
        }
        uiState.errorMessage?.let { message ->
            Spacer(Modifier.height(12.dp))
            Text(
                text = message,
                color = Color(0xFFBA1A1A),
                fontFamily = PretendardFontFamily,
                fontSize = 13.sp,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Spacer(Modifier.height(28.dp))
        Button(
            onClick = onSubmit,
            enabled = uiState.canSubmit,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(4.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = PrimaryAction,
                contentColor = Color.White,
                disabledContainerColor = Color(0xFFE8E8EB),
                disabledContentColor = Color(0xFF9295A5),
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
        ) {
            Text(
                text = when {
                    uiState.isSubmitting -> "처리 중..."
                    uiState.mode == AuthMode.LOGIN -> "로그인"
                    else -> "가입하기"
                },
                fontFamily = PretendardSemiBoldFontFamily,
                fontSize = 16.sp,
            )
        }
        Spacer(Modifier.height(24.dp))
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = painterResource(R.drawable.ganadi),
                contentDescription = null,
                modifier = Modifier.size(72.dp),
                contentScale = ContentScale.Fit,
            )
            Text(
                text = if (uiState.mode == AuthMode.LOGIN) "다시 만나서 반가워요" else "가야디와 첫 여행을 시작해요",
                color = TextPrimary,
                fontFamily = PretendardFontFamily,
                fontSize = 13.sp,
            )
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Preview(showBackground = true, heightDp = 800)
@Composable
private fun AuthScreenPreview() {
    GayadiTheme {
        AuthScreen(
            uiState = AuthUiState(mode = AuthMode.SIGN_UP, email = "user@example.com", nickname = "가야디"),
            onModeSelected = {},
            onEmailChanged = {},
            onPasswordChanged = {},
            onNicknameChanged = {},
            onSubmit = {},
        )
    }
}

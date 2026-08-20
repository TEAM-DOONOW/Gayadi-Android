package com.gayadi.android.ui.screens

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gayadi.android.domain.model.InquiryCategory
import com.gayadi.android.ui.components.GayadiTopAppBar
import com.gayadi.android.ui.theme.GayadiTheme
import com.gayadi.android.ui.theme.PrimaryAction
import com.gayadi.android.ui.theme.TagBlue
import com.gayadi.android.ui.theme.TagBlueText
import com.gayadi.android.ui.theme.TagRedText
import com.gayadi.android.ui.theme.TextPrimary
import com.gayadi.android.ui.theme.TextSecondary
import com.gayadi.android.ui.theme.TextTertiary

@Composable
fun InquiryRoute(
    viewModel: InquiryViewModel,
    onBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    InquiryScreen(
        uiState = uiState,
        onBack = onBack,
        onCategoryChange = viewModel::updateCategory,
        onTitleChange = viewModel::updateTitle,
        onMessageChange = viewModel::updateMessage,
        onContactEmailChange = viewModel::updateContactEmail,
        onSubmit = viewModel::submit,
        onWriteAnother = viewModel::reset,
    )
}

@Composable
internal fun InquiryScreen(
    uiState: InquiryUiState,
    onBack: () -> Unit,
    onCategoryChange: (InquiryCategory) -> Unit,
    onTitleChange: (String) -> Unit,
    onMessageChange: (String) -> Unit,
    onContactEmailChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onWriteAnother: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().background(Color.White)) {
        GayadiTopAppBar(title = "문의하기", onBack = onBack, showDivider = true)

        if (uiState.isSubmitted) {
            InquirySubmittedContent(onBack = onBack, onWriteAnother = onWriteAnother)
            return@Column
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            Spacer(Modifier.height(16.dp))
            Text(
                text = "불편한 점이나 제안을 남겨 주시면 입력하신 이메일로 답변드려요.",
                fontSize = 13.sp,
                lineHeight = 20.sp,
                color = TextSecondary,
            )

            Spacer(Modifier.height(24.dp))
            InquiryFieldLabel("문의 유형")
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                InquiryCategory.entries.forEach { category ->
                    InquiryCategoryChip(
                        category = category,
                        selected = category == uiState.category,
                        onClick = { onCategoryChange(category) },
                    )
                }
            }

            Spacer(Modifier.height(20.dp))
            InquiryFieldLabel("제목")
            Spacer(Modifier.height(8.dp))
            InquiryTextField(
                value = uiState.title,
                onValueChange = onTitleChange,
                placeholder = "무엇에 대한 문의인가요?",
                singleLine = true,
            )
            InquiryCounter(uiState.title.length, InquiryViewModel.TITLE_MAX_LENGTH)

            Spacer(Modifier.height(20.dp))
            InquiryFieldLabel("내용")
            Spacer(Modifier.height(8.dp))
            InquiryTextField(
                value = uiState.message,
                onValueChange = onMessageChange,
                placeholder = "언제, 어떤 화면에서 문제가 있었는지 알려주시면 도움이 돼요.",
                singleLine = false,
                modifier = Modifier.heightIn(min = 160.dp),
            )
            InquiryCounter(uiState.message.length, InquiryViewModel.MESSAGE_MAX_LENGTH)

            Spacer(Modifier.height(20.dp))
            InquiryFieldLabel("답변받을 이메일")
            Spacer(Modifier.height(8.dp))
            InquiryTextField(
                value = uiState.contactEmail,
                onValueChange = onContactEmailChange,
                placeholder = "gayadi@example.com",
                singleLine = true,
                keyboardType = KeyboardType.Email,
            )

            uiState.errorMessage?.let { message ->
                Spacer(Modifier.height(12.dp))
                Text(message, fontSize = 13.sp, color = TagRedText)
            }

            Spacer(Modifier.height(24.dp))
        }

        Column(
            modifier = Modifier
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .navigationBarsPadding()
                .imePadding(),
        ) {
            Button(
                onClick = onSubmit,
                enabled = uiState.canSubmit,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryAction,
                    disabledContainerColor = PrimaryAction.copy(alpha = 0.3f),
                ),
            ) {
                if (uiState.isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.height(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text("문의 보내기", fontSize = 15.sp, color = Color.White, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
private fun InquirySubmittedContent(onBack: () -> Unit, onWriteAnother: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("문의를 보냈어요", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
        Spacer(Modifier.height(10.dp))
        Text(
            text = "확인 후 입력하신 이메일로 답변드릴게요.\n보통 영업일 기준 2~3일이 걸려요.",
            fontSize = 13.sp,
            lineHeight = 20.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(28.dp))
        Button(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryAction),
        ) {
            Text("설정으로 돌아가기", fontSize = 15.sp, color = Color.White, fontWeight = FontWeight.Medium)
        }
        Text(
            text = "문의 하나 더 남기기",
            fontSize = 13.sp,
            color = TextTertiary,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onWriteAnother)
                .padding(vertical = 14.dp),
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun InquiryFieldLabel(text: String) {
    Text(text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
}

@Composable
private fun InquiryCounter(current: Int, max: Int) {
    Text(
        text = "$current / $max",
        fontSize = 11.sp,
        color = TextTertiary,
        textAlign = TextAlign.End,
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
    )
}

@Composable
private fun InquiryTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    singleLine: Boolean,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = { Text(placeholder, fontSize = 14.sp, color = TextTertiary) },
        singleLine = singleLine,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        shape = RoundedCornerShape(8.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White,
            focusedBorderColor = PrimaryAction,
            unfocusedBorderColor = Color(0xFFE0E1E7),
        ),
    )
}

@Composable
private fun InquiryCategoryChip(
    category: InquiryCategory,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) TagBlue else Color.White)
            .border(
                width = 1.dp,
                color = if (selected) TagBlueText else Color(0xFFE0E1E7),
                shape = RoundedCornerShape(16.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
    ) {
        Text(
            text = category.label,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) TagBlueText else TextSecondary,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun InquiryPreview() {
    GayadiTheme {
        InquiryScreen(
            uiState = InquiryUiState(
                category = InquiryCategory.FEATURE,
                title = "일정에 메모를 남기고 싶어요",
                message = "일정마다 짧은 메모를 붙일 수 있으면 좋겠어요.",
                contactEmail = "traveler@example.com",
            ),
            onBack = {},
            onCategoryChange = {},
            onTitleChange = {},
            onMessageChange = {},
            onContactEmailChange = {},
            onSubmit = {},
            onWriteAnother = {},
        )
    }
}

package com.gayadi.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import com.gayadi.android.ui.theme.PrimaryAction
import com.gayadi.android.ui.theme.TextPrimary
import com.gayadi.android.ui.theme.TextSecondary
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSharedFundBottomSheet(onDismiss: () -> Unit, onConfirm: (Long) -> Unit) {
    var amountText by rememberSaveable { mutableStateOf("") }
    val amountInTenThousands = amountText.toLongOrNull()
    val amount = amountInTenThousands?.let { runCatching { Math.multiplyExact(it, 10_000L) }.getOrNull() }
    val formattedAmount = amountInTenThousands?.let { NumberFormat.getNumberInstance(Locale.KOREA).format(it) }.orEmpty()
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Box(
                Modifier.padding(top = 10.dp, bottom = 8.dp).size(width = 40.dp, height = 4.dp)
                    .background(Color(0xFFD7D8DC), RoundedCornerShape(2.dp)),
            )
        },
    ) {
        Column(
            Modifier.fillMaxWidth().imePadding().navigationBarsPadding()
                .padding(start = 24.dp, top = 8.dp, end = 24.dp, bottom = 32.dp),
        ) {
            Text("공동 여행 경비 추가", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Spacer(Modifier.height(10.dp))
            Text("얼마를 추가할까요?", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = TextSecondary)
            Spacer(Modifier.height(28.dp))
            BasicTextField(
                value = formattedAmount,
                onValueChange = { input -> amountText = input.filter(Char::isDigit).take(10) },
                modifier = Modifier.fillMaxWidth().height(58.dp).focusRequester(focusRequester),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                textStyle = TextStyle(
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    textAlign = TextAlign.End,
                ),
                cursorBrush = SolidColor(PrimaryAction),
                decorationBox = { innerTextField ->
                    Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
                            if (formattedAmount.isEmpty()) {
                                Text("0", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = Color(0xFFB7B8C0))
                            }
                            innerTextField()
                        }
                        Spacer(Modifier.size(8.dp))
                        Text("만원", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    }
                },
            )
            HorizontalDivider(thickness = 2.dp, color = if (amountText.isNotEmpty()) PrimaryAction else Color(0xFFE2E3E6))
            Spacer(Modifier.height(28.dp))
            Button(
                onClick = { amount?.takeIf { it > 0L }?.let(onConfirm) },
                enabled = amount != null && amount > 0L,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryAction, contentColor = Color.White),
            ) {
                Text("추가하기", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

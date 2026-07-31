package com.gayadi.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gayadi.android.ui.theme.GayadiTheme
import com.gayadi.android.ui.theme.PretendardSemiBoldFontFamily
import com.gayadi.android.ui.theme.TextPrimary

@Composable
fun BasicInfoScreen(onStartSurvey: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var intro by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(80.dp))

        Text(
            text = "기본 정보 입력",
            fontSize = 26.sp,
            fontFamily = PretendardSemiBoldFontFamily,
            color = TextPrimary,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(48.dp))

        Text(
            text = "닉네임",
            fontSize = 18.sp,
            fontFamily = PretendardSemiBoldFontFamily,
            color = TextPrimary,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            textStyle = TextStyle(
                fontFamily = PretendardSemiBoldFontFamily,
                fontSize = 13.sp,
            ),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF8E8E93),
                unfocusedBorderColor = Color(0xFFD1D1D6),
                cursorColor = Color.Black,
            ),
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "한 줄 소개",
            fontSize = 18.sp,
            fontFamily = PretendardSemiBoldFontFamily,
            color = TextPrimary,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = intro,
            onValueChange = { intro = it },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            textStyle = TextStyle(
                fontFamily = PretendardSemiBoldFontFamily,
                fontSize = 13.sp,
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF8E8E93),
                unfocusedBorderColor = Color(0xFFD1D1D6),
                cursorColor = Color.Black,
            ),
        )

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = onStartSurvey,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
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

@Preview(showBackground = true)
@Composable
private fun BasicInfoPreview() {
    GayadiTheme { BasicInfoScreen(onStartSurvey = {}) }
}

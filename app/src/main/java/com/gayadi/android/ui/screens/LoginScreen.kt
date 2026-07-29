package com.gayadi.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gayadi.android.ui.theme.GayadiTheme
import com.gayadi.android.ui.theme.KakaoText
import com.gayadi.android.ui.theme.KakaoYellow
import com.gayadi.android.ui.theme.TextTertiary

@Composable
fun LoginScreen(
    onKakaoLogin: () -> Unit,
    onGoogleLogin: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = "가야디",
            fontSize = 42.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color(0xFF5B9BD5),
        )

        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .size(160.dp)
                .clip(CircleShape)
                .background(Color(0xFFE8F4FD)),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = "🐶", fontSize = 80.sp)
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = onKakaoLogin,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = KakaoYellow,
                contentColor = KakaoText,
            ),
        ) {
            Text(text = "💬  카카오 계정으로 시작하기", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = onGoogleLogin,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(12.dp)),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White,
                contentColor = Color(0xFF333333),
            ),
        ) {
            Text(text = "G  구글 계정으로 로그인하기", fontSize = 16.sp, fontWeight = FontWeight.Medium)
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "가입 시 이용약관 및 개인정보 처리방침에 동의하게 됩니다.",
            fontSize = 11.sp,
            color = TextTertiary,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Preview(showBackground = true)
@Composable
private fun LoginPreview() {
    GayadiTheme { LoginScreen(onKakaoLogin = {}, onGoogleLogin = {}) }
}

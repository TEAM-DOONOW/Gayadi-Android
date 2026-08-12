package com.gayadi.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gayadi.android.ui.theme.TextPrimary
import com.gayadi.android.ui.theme.TextSecondary

enum class LegalDocument(val title: String, val body: String) {
    TERMS(
        title = "서비스 이용약관",
        body = """제1조 목적
이 약관은 가야디가 제공하는 여행 관련 서비스의 이용 조건과 절차를 정하는 것을 목적으로 합니다.

제2조 서비스 이용
사용자는 관계 법령과 본 약관을 준수하여 서비스를 이용해야 합니다. 서비스의 안정적인 운영을 방해하거나 타인의 권리를 침해해서는 안 됩니다.

제3조 서비스 변경 및 중단
서비스 개선 또는 운영상 필요한 경우 제공 내용이 변경되거나 일시 중단될 수 있습니다.

제4조 책임
가야디는 관련 법령이 허용하는 범위에서 서비스 이용과 관련한 책임을 부담합니다.

시행일: 2026년 8월 12일""",
    ),
    PRIVACY(
        title = "개인정보처리방침",
        body = """1. 수집하는 개인정보
가야디는 서비스 제공을 위해 닉네임, 여행 성향, 여행 일정 등 사용자가 입력한 정보를 처리할 수 있습니다.

2. 이용 목적
수집한 정보는 사용자 프로필 구성, 맞춤형 여행 정보 제공 및 서비스 품질 개선을 위해 이용합니다.

3. 보유 및 파기
개인정보는 이용 목적을 달성하거나 회원 탈퇴 시 지체 없이 파기합니다. 관련 법령에 따라 보관이 필요한 경우에는 해당 기간 동안 안전하게 보관합니다.

4. 이용자의 권리
사용자는 자신의 개인정보를 조회·수정하거나 삭제 및 처리 정지를 요청할 수 있습니다.

시행일: 2026년 8월 12일""",
    ),
}

@Composable
fun LegalDocumentScreen(document: LegalDocument, onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().background(Color.White)) {
        Spacer(modifier = Modifier.height(36.dp))
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로")
            }
            Text(document.title, fontSize = 21.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        }
        Text(
            text = document.body,
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
            fontSize = 14.sp,
            lineHeight = 23.sp,
            color = TextSecondary,
        )
    }
}

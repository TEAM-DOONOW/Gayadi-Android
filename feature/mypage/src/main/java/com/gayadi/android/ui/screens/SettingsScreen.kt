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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.gayadi.android.ui.theme.PrimaryBlue
import com.gayadi.android.ui.theme.TagPink
import com.gayadi.android.ui.theme.TagPinkText
import com.gayadi.android.ui.theme.TextPrimary
import com.gayadi.android.ui.theme.TextSecondary
import com.gayadi.android.ui.theme.TextTertiary
import com.gayadi.android.ui.components.UserCharacterAvatar
import com.gayadi.android.domain.model.UserProfile

@Composable
fun SettingsScreen(
    uiState: ProfileUiState,
    onBack: () -> Unit,
    onEditProfile: () -> Unit,
    onTermsOfService: () -> Unit,
    onPrivacyPolicy: () -> Unit,
    onLogout: () -> Unit,
    onDeleteAccount: () -> Unit,
) {
    var locationPermission by remember { mutableStateOf(true) }
    var darkMode by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .verticalScroll(rememberScrollState()),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로")
            }
            Text("설정", fontSize = 23.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
        }

        androidx.compose.material3.HorizontalDivider(color = Color(0xFFE5E5E5))

        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            Spacer(modifier = Modifier.height(8.dp))

            uiState.errorMessage?.let { message ->
                Text(message, color = Color(0xFFE53935), fontSize = 13.sp)
                Spacer(modifier = Modifier.height(8.dp))
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF8F9FB))
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val profile = uiState.profile
                UserCharacterAvatar(
                    characterKey = profile?.characterKey,
                    contentDescription = "${profile?.nickname.orEmpty()} 여행 성향 캐릭터",
                    modifier = Modifier.size(48.dp),
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(profile?.nickname ?: "여행자", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    Text(profile?.travelStyleName ?: "여행 성향 미설정", fontSize = 12.sp, color = TextSecondary)
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(TagPink)
                        .clickable(onClick = onEditProfile)
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    Text("편집", fontSize = 12.sp, color = TagPinkText, fontWeight = FontWeight.Medium)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text("계정", fontSize = 13.sp, color = TextTertiary, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(8.dp))

            SettingsRow("알림 설정", trailing = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = TextTertiary) })
            HorizontalDivider(color = Color(0xFFF0F0F0))
            SettingsRow("위치 권한", trailing = {
                Switch(
                    checked = locationPermission,
                    onCheckedChange = { locationPermission = it },
                    colors = SwitchDefaults.colors(checkedTrackColor = PrimaryBlue),
                )
            })

            Spacer(modifier = Modifier.height(24.dp))

            Text("앱", fontSize = 13.sp, color = TextTertiary, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(8.dp))

            SettingsRow("다크 모드", trailing = {
                Switch(
                    checked = darkMode,
                    onCheckedChange = { darkMode = it },
                    colors = SwitchDefaults.colors(checkedTrackColor = PrimaryBlue),
                )
            })
            HorizontalDivider(color = Color(0xFFF0F0F0))
            SettingsRow("언어", trailing = { Text("Ko", fontSize = 14.sp, color = TextSecondary) })
            HorizontalDivider(color = Color(0xFFF0F0F0))
            SettingsRow("버전 정보", trailing = { Text("1.0.0", fontSize = 14.sp, color = TextSecondary) })
            HorizontalDivider(color = Color(0xFFF0F0F0))
            SettingsRow(
                label = "이용약관",
                onClick = onTermsOfService,
                trailing = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = TextTertiary) },
            )
            HorizontalDivider(color = Color(0xFFF0F0F0))
            SettingsRow(
                label = "개인정보처리방침",
                onClick = onPrivacyPolicy,
                trailing = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = TextTertiary) },
            )

            Spacer(modifier = Modifier.height(32.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, Color(0xFFFFEBEE), RoundedCornerShape(12.dp))
                    .clickable(onClick = onLogout)
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("로그아웃", fontSize = 15.sp, color = Color(0xFFE53935), fontWeight = FontWeight.Medium)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "회원 탈퇴",
                fontSize = 13.sp,
                color = TextTertiary,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onDeleteAccount)
                    .padding(vertical = 8.dp),
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SettingsRow(
    label: String,
    onClick: (() -> Unit)? = null,
    trailing: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick == null) Modifier else Modifier.clickable(onClick = onClick))
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, fontSize = 15.sp, color = TextPrimary)
        trailing()
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsPreview() {
    GayadiTheme {
        SettingsScreen(
            uiState = ProfileUiState(UserProfile("가야디", "여행가")),
            onBack = {},
            onEditProfile = {},
            onTermsOfService = {},
            onPrivacyPolicy = {},
            onLogout = {},
            onDeleteAccount = {},
        )
    }
}

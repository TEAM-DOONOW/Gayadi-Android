package com.gayadi.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gayadi.android.ui.theme.GayadiTheme
import com.gayadi.android.ui.theme.PrimaryAction
import com.gayadi.android.ui.theme.TagPink
import com.gayadi.android.ui.theme.TagPinkText
import com.gayadi.android.ui.theme.TextPrimary
import com.gayadi.android.ui.theme.TextSecondary
import com.gayadi.android.ui.theme.TextTertiary
import com.gayadi.android.ui.components.UserCharacterAvatar
import com.gayadi.android.domain.model.UserProfile
import com.gayadi.android.feature.mypage.R

@Composable
fun SettingsScreen(
    uiState: ProfileUiState,
    onBack: () -> Unit,
    onOpenTravelProfile: () -> Unit,
    onOpenTerms: () -> Unit,
    onOpenPrivacyPolicy: () -> Unit,
    onLogout: () -> Unit,
    onDeleteAccount: () -> Unit,
) {
    var locationPermission by remember { mutableStateOf(true) }
    var darkMode by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
    ) {
        Spacer(modifier = Modifier.height(36.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로")
            }
            Text("설정", fontSize = 21.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            uiState.errorMessage?.let { message ->
                Text(message, color = Color(0xFFE53935), fontSize = 13.sp)
                Spacer(modifier = Modifier.height(8.dp))
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpenTravelProfile)
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val profile = uiState.profile
                UserCharacterAvatar(
                    characterKey = profile?.characterKey,
                    contentDescription = "${profile?.nickname.orEmpty()} 여행 성향 캐릭터",
                    modifier = Modifier.size(56.dp),
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(profile?.nickname ?: "여행자", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(18.dp))
                            .background(TagPink)
                            .border(1.dp, TagPinkText.copy(alpha = 0.35f), RoundedCornerShape(18.dp))
                            .padding(horizontal = 12.dp, vertical = 2.dp),
                    ) {
                        Text(
                            text = profile?.travelStyleName ?: "여행 성향 미설정",
                            fontSize = 11.sp,
                            letterSpacing = 0.3.sp,
                            color = TagPinkText,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "내 여행 프로필 보기",
                    tint = TextTertiary,
                )
            }

            Spacer(modifier = Modifier.height(22.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color(0xFFE6E6EA)),
            )
            Spacer(modifier = Modifier.height(22.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(R.drawable.padlock),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("계정", fontSize = 18.sp, color = TextPrimary, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(8.dp))

            SettingsRow("알림 설정", trailing = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = TextTertiary) })
            SettingsRow("위치 권한", trailing = {
                Switch(
                    checked = locationPermission,
                    onCheckedChange = { locationPermission = it },
                    colors = SwitchDefaults.colors(checkedTrackColor = PrimaryAction),
                )
            })

            Spacer(modifier = Modifier.height(22.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color(0xFFE6E6EA)),
            )
            Spacer(modifier = Modifier.height(22.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(R.drawable.application),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("앱", fontSize = 18.sp, color = TextPrimary, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(8.dp))

            SettingsRow("다크 모드", trailing = {
                Switch(
                    checked = darkMode,
                    onCheckedChange = { darkMode = it },
                    colors = SwitchDefaults.colors(checkedTrackColor = PrimaryAction),
                )
            })
            SettingsRow("언어", trailing = { Text("Ko", fontSize = 14.sp, color = TextSecondary) })
            SettingsRow("버전 정보", trailing = { Text("1.0.0", fontSize = 14.sp, color = TextSecondary) })
            SettingsRow("서비스 이용약관", onClick = onOpenTerms, trailing = {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = TextTertiary)
            })
            SettingsRow("개인정보처리방침", onClick = onOpenPrivacyPolicy, trailing = {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = TextTertiary)
            })

            Spacer(modifier = Modifier.height(32.dp))
        }

        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RectangleShape)
                    .background(PrimaryAction)
                    .border(1.dp, PrimaryAction, RectangleShape)
                    .clickable(onClick = onLogout)
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("로그아웃", fontSize = 15.sp, color = Color.White, fontWeight = FontWeight.Medium)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "회원 탈퇴",
                fontSize = 13.sp,
                color = TextTertiary,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onDeleteAccount)
                    .padding(vertical = 10.dp),
            )
        }
    }
}

@Composable
private fun SettingsRow(label: String, onClick: (() -> Unit)? = null, trailing: @Composable () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 4.dp, vertical = 16.dp),
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
            onOpenTravelProfile = {},
            onOpenTerms = {},
            onOpenPrivacyPolicy = {},
            onLogout = {},
            onDeleteAccount = {},
        )
    }
}

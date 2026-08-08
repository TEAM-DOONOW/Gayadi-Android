package com.gayadi.android.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.gayadi.android.ui.components.BottomNavBar
import com.gayadi.android.ui.components.BottomTab
import com.gayadi.android.ui.components.UserCharacterAvatar
import com.gayadi.android.domain.model.UserProfile
import com.gayadi.android.ui.theme.GayadiTheme
import com.gayadi.android.ui.theme.PrimaryBlue
import com.gayadi.android.ui.theme.TextPrimary
import com.gayadi.android.ui.theme.TextSecondary

@Composable
fun MyPageScreen(
    uiState: ProfileUiState,
    onNavigateHome: () -> Unit,
    onNavigateSettings: () -> Unit,
) {
    val profile = uiState.profile
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            Spacer(modifier = Modifier.height(56.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("마이페이지", fontSize = 23.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                IconButton(onClick = onNavigateSettings) {
                    Icon(Icons.Outlined.Settings, contentDescription = "설정", tint = TextSecondary)
                }
            }

            androidx.compose.material3.HorizontalDivider(color = Color(0xFFE5E5E5))

            Spacer(modifier = Modifier.height(24.dp))

            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                UserCharacterAvatar(
                    characterKey = profile?.characterKey,
                    contentDescription = "${profile?.nickname.orEmpty()} 여행 성향 캐릭터",
                    modifier = Modifier.size(80.dp),
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(profile?.nickname ?: "여행자", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary)

                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFFEBF5FF))
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                ) {
                    Text(
                        profile?.travelStyleName ?: "여행 성향을 확인해 보세요",
                        fontSize = 13.sp,
                        color = PrimaryBlue,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FB)),
            ) {
                Row(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "이런점이\n좋아요",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        modifier = Modifier.width(72.dp),
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        profile.profileStrengths().forEach {
                            Text("• $it", fontSize = 12.sp, color = TextSecondary, lineHeight = 20.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FB)),
            ) {
                Row(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "이런점은\n보완해야해요",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        modifier = Modifier.width(72.dp),
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        profile.profileWeaknesses().forEach {
                            Text("• $it", fontSize = 12.sp, color = TextSecondary, lineHeight = 20.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        BottomNavBar(
            currentTab = BottomTab.MY_PAGE,
            showMyPage = true,
            onTabSelected = { tab ->
                if (tab == BottomTab.OUR_TRIP) onNavigateHome()
            },
        )
    }
}

private fun UserProfile?.profileStrengths(): List<String> =
    this?.strengths?.takeIf { it.isNotEmpty() } ?: listOf("설문을 완료하면 강점을 알려드려요.")

private fun UserProfile?.profileWeaknesses(): List<String> =
    this?.weaknesses?.takeIf { it.isNotEmpty() } ?: listOf("설문을 완료하면 보완점을 알려드려요.")

@Preview(showBackground = true)
@Composable
private fun MyPagePreview() {
    GayadiTheme {
        MyPageScreen(
            uiState = ProfileUiState(UserProfile("가야디", "여행가")),
            onNavigateHome = {},
            onNavigateSettings = {},
        )
    }
}

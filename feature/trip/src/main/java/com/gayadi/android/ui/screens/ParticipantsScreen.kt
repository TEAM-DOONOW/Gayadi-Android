package com.gayadi.android.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gayadi.android.domain.model.TravelParticipant
import com.gayadi.android.feature.trip.R
import com.gayadi.android.ui.components.GayadiTopAppBar
import com.gayadi.android.ui.components.UserCharacterAvatar
import com.gayadi.android.ui.theme.TextPrimary
import com.gayadi.android.ui.theme.TextSecondary
import kotlinx.coroutines.launch

@Composable
fun ParticipantsScreen(
    tripName: String,
    inviteCode: String,
    cities: List<String>,
    currentUserId: String,
    participants: List<TravelParticipant>,
    onBack: () -> Unit,
    onRemove: (String) -> Unit,
    onPublishInvite: suspend () -> Result<Unit> = { Result.success(Unit) },
) {
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var inviteMessage by remember { mutableStateOf<String?>(null) }

    val host = participants.find { it.id == currentUserId }
    val guests = participants.filterNot { it.id == currentUserId }

    Column(Modifier.fillMaxSize().background(Color(0xFFF7F7F9))) {
        GayadiTopAppBar(
            title = "참여자 관리",
            onBack = onBack,
            containerColor = Color(0xFFF7F7F9),
        )
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
            Spacer(Modifier.height(18.dp))
            if (inviteCode.isNotBlank()) {
                InviteCodeLetter(
                    tripName = tripName,
                    inviteCode = inviteCode,
                    inviteMessage = inviteMessage,
                    onCopy = {
                        coroutineScope.launch {
                            onPublishInvite().fold(
                                onSuccess = {
                                    clipboard.setText(AnnotatedString(inviteCode))
                                    inviteMessage = "초대 코드를 복사했어요"
                                },
                                onFailure = { inviteMessage = it.message ?: "초대 코드를 등록하지 못했어요" },
                            )
                        }
                    },
                    onShare = {
                        coroutineScope.launch {
                            onPublishInvite().fold(
                                onSuccess = {
                                    inviteMessage = null
                                    shareTripInviteToKakao(context, tripName, cities, inviteCode)
                                },
                                onFailure = { inviteMessage = it.message ?: "초대 코드를 등록하지 못했어요" },
                            )
                        }
                    },
                )
                Spacer(Modifier.height(24.dp))
            }

            Text("함께하는 여행 메이트", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            Spacer(Modifier.height(8.dp))
            host?.let { ParticipantRow(participant = it, isHost = true) }
            guests.forEach { participant ->
                ParticipantRow(participant = participant, onRemove = { onRemove(participant.id) })
            }
            if (guests.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = Color(0xFFB3B5BF),
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("아직 초대된 여행 메이트가 없어요", color = TextSecondary, fontSize = 14.sp)
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun InviteCodeLetter(
    tripName: String,
    inviteCode: String,
    inviteMessage: String?,
    onCopy: () -> Unit,
    onShare: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.fillMaxWidth().height(275.dp).clipToBounds()) {
            Image(
                painter = painterResource(R.drawable.trip_gayadi_letter),
                contentDescription = "여행 초대 편지",
                modifier = Modifier.fillMaxWidth().height(205.dp).align(Alignment.BottomCenter),
                contentScale = ContentScale.FillBounds,
            )
            Image(
                painter = painterResource(R.drawable.ganadi_hello),
                contentDescription = "편지 위에서 인사하는 가나디",
                modifier = Modifier.width(230.dp).height(183.dp).align(Alignment.TopCenter).offset(y = (-18).dp),
                contentScale = ContentScale.Fit,
            )
            Column(
                modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter).padding(bottom = 42.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("함께 여행을 떠나볼까요?", fontSize = 19.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(Modifier.height(6.dp))
                Text(tripName, fontSize = 16.sp, color = TextSecondary)
                Spacer(Modifier.height(11.dp))
                Column(
                    modifier = Modifier.offset(y = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("여행 초대 코드", fontSize = 13.sp, color = TextPrimary)
                    Spacer(Modifier.height(4.dp))
                    Text(inviteCode, fontSize = 27.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = onCopy,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(0.dp),
            ) {
                Text("코드 복사")
            }
            Button(
                onClick = onShare,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(0.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFEE500),
                    contentColor = Color(0xFF191919),
                ),
            ) {
                Image(
                    painter = painterResource(R.drawable.kakaotalk),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text("카카오톡 공유하기", fontSize = 12.sp)
            }
        }
        inviteMessage?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, fontSize = 12.sp, color = TextSecondary)
        }
    }
}

@Composable
private fun ParticipantRow(
    participant: TravelParticipant,
    isHost: Boolean = false,
    onRemove: (() -> Unit)? = null,
) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        UserCharacterAvatar(participant.characterKey, "${participant.nickname} 캐릭터", Modifier.size(42.dp))
        Column(Modifier.weight(1f)) {
            Text(participant.nickname, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            Text("ID ${participant.id}", fontSize = 11.sp, color = TextSecondary)
        }
        if (isHost) {
            Surface(shape = RoundedCornerShape(50), color = Color(0xFFFFE98A)) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = Color(0xFF8A6500),
                    )
                    Text("방장", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF6D5000))
                }
            }
        } else {
            OutlinedButton(onClick = { onRemove?.invoke() }) { Text("내보내기") }
        }
    }
}

package com.gayadi.android.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gayadi.android.feature.home.R
import com.gayadi.android.ui.components.UserCharacterAvatar
import com.gayadi.android.ui.theme.PrimaryAction
import com.gayadi.android.ui.theme.TextPrimary
import com.gayadi.android.ui.theme.TextSecondary
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@Composable
internal fun TravelOverviewCard(
    progress: Float,
    participantCount: Int,
    myCharacterKey: String?,
    friendCharacterKeys: List<String?>,
    onParticipants: () -> Unit,
) {
    val visibleFriendCharacterKeys = friendCharacterKeys
        .filter { it != myCharacterKey }
        .distinct()
        .take(2)
    val displayedParticipantCount = (1 + visibleFriendCharacterKeys.size)
        .coerceAtMost(participantCount)
    val remainingParticipantCount = (participantCount - displayedParticipantCount)
        .coerceAtLeast(0)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(176.dp),
    ) {
        Image(
            painter = painterResource(R.drawable.gayadi_letter),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds,
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 16.dp),
        ) {
            Text(
                "우리 여행 진행률",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
            )
            Spacer(Modifier.height(8.dp))
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
            ) {
                val progressOffset = (maxWidth - 40.dp) * progress - 8.dp
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .height(10.dp)
                        .align(Alignment.TopCenter)
                        .offset(y = 13.dp)
                        .clip(CircleShape),
                    color = PrimaryAction,
                    trackColor = Color(0xFFD9D9DE),
                    drawStopIndicator = {},
                )
                Image(
                    painter = painterResource(R.drawable.car),
                    contentDescription = "여행 진행 위치",
                    modifier = Modifier
                        .size(40.dp)
                        .offset(
                            x = progressOffset,
                            y = (-8).dp,
                        ),
                    contentScale = ContentScale.Fit,
                )
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .offset(
                            x = progressOffset,
                            y = 15.dp,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "${(progress * 100).toInt()}%",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryAction,
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Column(
                Modifier
                    .fillMaxWidth()
                    .minimumInteractiveComponentSize()
                    .semantics {
                        role = Role.Button
                        contentDescription = "함께하는 친구 ${participantCount}명 보기"
                    }
                    .clickable(onClick = onParticipants),
            ) {
                Text(
                    "함께 하는 친구",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                )
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    UserCharacterAvatar(myCharacterKey, "내 캐릭터", Modifier.requiredSize(30.dp))
                    Spacer(Modifier.width(4.dp))
                    visibleFriendCharacterKeys.forEach { key ->
                        UserCharacterAvatar(key, "함께하는 친구", Modifier.requiredSize(30.dp))
                        Spacer(Modifier.width(4.dp))
                    }
                    Box(
                        Modifier
                            .requiredSize(30.dp)
                            .background(Color(0xFFECECF1), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            if (remainingParticipantCount == 0) "+" else "+$remainingParticipantCount",
                            fontSize = 10.sp,
                            color = TextSecondary,
                        )
                    }
                }
            }
        }
    }
}

internal fun calculateTripProgress(
    startDate: String,
    endDate: String,
    today: LocalDate = LocalDate.now(),
): Float = runCatching {
    val formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd")
    val start = LocalDate.parse(startDate, formatter)
    val end = LocalDate.parse(endDate, formatter)
    when {
        end.isBefore(start) -> 0f
        today.isBefore(start) -> 0f
        !today.isBefore(end) -> 1f
        else -> {
            val totalDays = ChronoUnit.DAYS.between(start, end)
            if (totalDays == 0L) 1f
            else ChronoUnit.DAYS.between(start, today).toFloat() / totalDays
        }
    }
}.getOrDefault(0f)

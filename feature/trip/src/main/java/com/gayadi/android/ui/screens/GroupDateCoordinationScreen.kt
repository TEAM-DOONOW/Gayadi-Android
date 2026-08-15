package com.gayadi.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gayadi.android.domain.model.TravelParticipant
import com.gayadi.android.domain.model.TravelTrip
import com.gayadi.android.ui.theme.PretendardFontFamily
import com.gayadi.android.ui.theme.PretendardSemiBoldFontFamily
import com.gayadi.android.ui.theme.TextPrimary
import com.gayadi.android.ui.components.GayadiTopAppBar
import com.gayadi.android.ui.components.UserCharacterAvatar
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

private val GroupAccent = Color(0xFF343548)
private val CommonDate = Color(0xFFBFDCC5)
private val ParticipantColors = listOf(
    Color(0xFFFFE7A3),
    Color(0xFFBFD9FF),
    Color(0xFFFFC7D6),
    Color(0xFFD8C7FF),
    Color(0xFFFFD1AA),
)
private val savedDateFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd")

@Composable
fun GroupDateCoordinationScreen(
    trip: TravelTrip?,
    currentUserId: String,
    participants: List<TravelParticipant>,
    onBack: () -> Unit,
    onSubmit: (String, List<String>) -> Unit,
    onFinalize: (String, String) -> Unit,
) {
    if (trip == null) return
    val context = LocalContext.current
    val otherParticipants = remember(currentUserId, participants) {
        participants.filterNot { it.id == currentUserId }
    }
    val members = remember(currentUserId, otherParticipants) {
        listOf(currentUserId to "나") + otherParticipants.map { it.id to it.nickname }
    }
    val memberColors = remember(members) {
        members.mapIndexed { index, member -> member.first to ParticipantColors[index % ParticipantColors.size] }.toMap()
    }
    var activeMemberId by remember(currentUserId) { mutableStateOf(currentUserId) }
    var visibleMonth by remember { mutableStateOf(YearMonth.now()) }
    var selectedDates by remember(activeMemberId, trip.dateAvailability) {
        mutableStateOf(trip.dateAvailability[activeMemberId].orEmpty().toSet())
    }
    var finalRange by remember { mutableStateOf<Set<String>>(emptySet()) }
    val submittedIds = trip.dateAvailability.keys
    val hostOnly = otherParticipants.isEmpty()
    val allSubmitted = otherParticipants.isNotEmpty() && members.all { it.first in submittedIds }
    val commonDates = if (allSubmitted) {
        members.map { trip.dateAvailability[it.first].orEmpty().toSet() }
            .reduceOrNull(Set<String>::intersect).orEmpty()
    } else emptySet()

    Column(Modifier.fillMaxSize().background(Color(0xFFFAFAFB))) {
        GayadiTopAppBar(
            title = "가능한 날짜 정하기",
            onBack = onBack,
            containerColor = Color(0xFFFAFAFB),
        )
        Column(Modifier.padding(horizontal = 20.dp)) {
            Spacer(Modifier.height(18.dp))
            Text(
                if (allSubmitted) "모두 가능한 날짜 중 여행 기간을 선택해 주세요"
                else "가능한 날짜를 모두 선택한 뒤 제출해 주세요",
                fontFamily = PretendardSemiBoldFontFamily,
                fontSize = 16.sp,
                color = TextPrimary,
            )
            Spacer(Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                members.forEach { (id, name) ->
                    val participant = participants.firstOrNull { it.id == id }
                    ParticipantProfile(
                        name = name,
                        characterKey = participant?.characterKey,
                        selected = id == activeMemberId,
                        submitted = id in submittedIds,
                        color = memberColors.getValue(id),
                        onClick = {
                            activeMemberId = id
                            finalRange = emptySet()
                        },
                    )
                }
                InviteFriendProfile(
                    onClick = {
                        shareTripInviteToKakao(context, trip.name, trip.cities, trip.inviteCode)
                    },
                )
            }
            Spacer(Modifier.height(20.dp))
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White)
                    .padding(14.dp),
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { visibleMonth = visibleMonth.minusMonths(1) }) {
                        Icon(Icons.Default.ChevronLeft, "이전 달")
                    }
                    Text(
                        "${visibleMonth.year}년 ${visibleMonth.monthValue}월",
                        modifier = Modifier.weight(1f),
                        fontFamily = PretendardSemiBoldFontFamily,
                        fontSize = 18.sp,
                        color = TextPrimary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                    IconButton(onClick = { visibleMonth = visibleMonth.plusMonths(1) }) {
                        Icon(Icons.Default.ChevronRight, "다음 달")
                    }
                }
                Row(Modifier.fillMaxWidth()) {
                    listOf("일", "월", "화", "수", "목", "금", "토").forEach {
                        Text(it, Modifier.weight(1f).padding(vertical = 10.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center, color = Color(0xFF777781))
                    }
                }
                val leading = visibleMonth.atDay(1).dayOfWeek.value % 7
                val cells = (0 until 42).map { index ->
                    (index - leading + 1).takeIf { it in 1..visibleMonth.lengthOfMonth() }
                }
                cells.chunked(7).forEach { week ->
                    Row(Modifier.fillMaxWidth()) {
                        week.forEach { day ->
                            val date = day?.let(visibleMonth::atDay)
                            val key = date?.format(savedDateFormatter)
                            val enabled = date != null && !date.isBefore(LocalDate.now())
                            val selected = key != null && key in selectedDates
                            val common = key != null && key in commonDates
                            val final = key != null && key in finalRange
                            val highlightedDates = when {
                                final -> finalRange
                                common -> commonDates
                                selected -> selectedDates
                                else -> emptySet()
                            }
                            val highlightShape = key?.let { connectedDateShape(it, highlightedDates) } ?: CircleShape
                            Box(
                                modifier = Modifier.weight(1f).aspectRatio(1f).padding(vertical = 2.dp)
                                    .background(
                                        when {
                                            final -> GroupAccent
                                            common -> CommonDate
                                            selected -> memberColors.getValue(activeMemberId)
                                            else -> Color.Transparent
                                        },
                                        highlightShape,
                                    )
                                    .clickable(enabled = enabled) {
                                        if (allSubmitted && common) {
                                            finalRange = buildContinuousRange(commonDates, finalRange, key!!)
                                        } else if (!allSubmitted) {
                                            selectedDates = if (selected) selectedDates - key!! else selectedDates + key!!
                                        }
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    day?.toString().orEmpty(),
                                    color = when {
                                        final -> Color.White
                                        enabled -> TextPrimary
                                        else -> Color(0xFFC5C5CA)
                                    },
                                    fontFamily = PretendardFontFamily,
                                )
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                when {
                    allSubmitted && commonDates.isEmpty() -> "겹치는 날짜가 없어요. 가능한 날짜를 다시 조정해 주세요."
                    allSubmitted -> "초록색은 모든 참여자가 가능한 날짜예요."
                    else -> "${submittedIds.size}/${members.size}명 제출 완료"
                },
                modifier = if (!allSubmitted) Modifier.fillMaxWidth() else Modifier,
                color = Color(0xFF777781),
                fontFamily = PretendardFontFamily,
                fontSize = 13.sp,
                textAlign = if (!allSubmitted) androidx.compose.ui.text.style.TextAlign.End
                else androidx.compose.ui.text.style.TextAlign.Start,
            )
            Spacer(Modifier.weight(1f))
            Button(
                onClick = {
                    if (hostOnly) {
                        val sorted = selectedDates.sorted()
                        onFinalize(sorted.first(), sorted.last())
                    } else if (allSubmitted) {
                        val sorted = finalRange.sorted()
                        onFinalize(sorted.first(), sorted.last())
                    } else {
                        onSubmit(activeMemberId, selectedDates.toList())
                    }
                },
                enabled = if (allSubmitted) finalRange.isNotEmpty() else selectedDates.isNotEmpty(),
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(2.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = GroupAccent,
                    disabledContainerColor = Color(0xFFE8E8EB),
                ),
            ) {
                Text(
                    if (allSubmitted) "여행 날짜 확정하기" else "가능한 날짜 제출하기",
                    fontFamily = PretendardSemiBoldFontFamily,
                    fontSize = 15.sp,
                )
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ParticipantProfile(
    name: String,
    characterKey: String?,
    selected: Boolean,
    submitted: Boolean,
    color: Color,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box {
            UserCharacterAvatar(
                characterKey = characterKey,
                contentDescription = "$name 프로필",
                modifier = Modifier
                    .size(44.dp)
                    .then(if (selected) Modifier.border(2.dp, GroupAccent, CircleShape) else Modifier),
            )
            if (submitted) {
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .background(color, CircleShape)
                        .align(Alignment.BottomEnd),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("✓", fontSize = 9.sp, color = TextPrimary)
                }
            }
        }
        Spacer(Modifier.height(5.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Box(Modifier.size(8.dp).background(color, CircleShape))
            Text(name, fontFamily = PretendardFontFamily, fontSize = 11.sp, color = TextPrimary)
        }
    }
}

@Composable
private fun InviteFriendProfile(onClick: () -> Unit) {
    Column(
        modifier = Modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .border(1.dp, Color(0xFFB8B9C0), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Default.Add, contentDescription = "친구 초대하기", tint = GroupAccent)
        }
        Spacer(Modifier.height(5.dp))
        Text("친구 초대하기", fontFamily = PretendardFontFamily, fontSize = 11.sp, color = TextPrimary)
    }
}

private fun connectedDateShape(date: String, dates: Set<String>): androidx.compose.ui.graphics.Shape {
    val localDate = LocalDate.parse(date, savedDateFormatter)
    val hasPrevious = localDate.dayOfWeek.value != 7 &&
        localDate.minusDays(1).format(savedDateFormatter) in dates
    val hasNext = localDate.dayOfWeek.value != 6 &&
        localDate.plusDays(1).format(savedDateFormatter) in dates
    return when {
        hasPrevious && hasNext -> RoundedCornerShape(0.dp)
        hasPrevious -> RoundedCornerShape(topEnd = 50.dp, bottomEnd = 50.dp)
        hasNext -> RoundedCornerShape(topStart = 50.dp, bottomStart = 50.dp)
        else -> CircleShape
    }
}

private fun buildContinuousRange(commonDates: Set<String>, current: Set<String>, clicked: String): Set<String> {
    val anchor = current.minOrNull() ?: return setOf(clicked)
    val start = minOf(anchor, clicked)
    val end = maxOf(anchor, clicked)
    val range = generateSequence(LocalDate.parse(start, savedDateFormatter)) { it.plusDays(1) }
        .takeWhile { !it.isAfter(LocalDate.parse(end, savedDateFormatter)) }
        .map { it.format(savedDateFormatter) }
        .toSet()
    return if (range.all { it in commonDates }) range else setOf(clicked)
}

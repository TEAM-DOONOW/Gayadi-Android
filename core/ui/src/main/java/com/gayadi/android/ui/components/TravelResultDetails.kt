package com.gayadi.android.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gayadi.android.domain.model.CompatibleTravelType
import com.gayadi.android.domain.model.SurveyResult
import com.gayadi.android.domain.model.TravelRole
import com.gayadi.android.ui.theme.PretendardFontFamily
import com.gayadi.android.ui.theme.PretendardSemiBoldFontFamily
import com.gayadi.android.ui.theme.TagBlue
import com.gayadi.android.ui.theme.TagBlueText
import com.gayadi.android.ui.theme.TagGreen
import com.gayadi.android.ui.theme.TagGreenText
import com.gayadi.android.ui.theme.TagPurple
import com.gayadi.android.ui.theme.TagPurpleText
import com.gayadi.android.ui.theme.TextPrimary
import com.gayadi.android.ui.theme.TextSecondary

/** Shared result details rendered identically on the survey result and saved profile screens. */
@Composable
@OptIn(ExperimentalLayoutApi::class)
fun TravelResultDetails(result: SurveyResult, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        if (result.hashtags.isNotEmpty()) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                result.hashtags.forEachIndexed { index, tag ->
                    val (background, textColor) = hashtagPalette(index)
                    HashtagChip(text = tag, background = background, textColor = textColor)
                }
            }
        }
        if (result.strengths.isNotEmpty()) {
            Spacer(modifier = Modifier.height(28.dp))
            InsightCard(title = "이런점이\n좋아요", items = result.strengths)
        }
        if (result.weaknesses.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            InsightCard(title = "이런점은\n보완해야해요", items = result.weaknesses)
        }
        if (result.compatibleTypes.isNotEmpty()) {
            Spacer(modifier = Modifier.height(24.dp))
            CompatibleTypesCard(result.compatibleTypes)
        }
        result.travelRole?.let { role ->
            Spacer(modifier = Modifier.height(12.dp))
            TravelRoleCard(
                role = role,
                characterKey = result.characterKey,
                characterName = result.name,
            )
        }
    }
}

private fun hashtagPalette(index: Int): Pair<Color, Color> = when (index % 3) {
    0 -> TagPurple to TagPurpleText
    1 -> TagGreen to TagGreenText
    else -> TagBlue to TagBlueText
}

@Composable
private fun HashtagChip(text: String, background: Color, textColor: Color) {
    val shape = RoundedCornerShape(8.dp)
    Box(
        modifier = Modifier
            .clip(shape)
            .background(background)
            .border(1.dp, textColor.copy(alpha = 0.35f), shape)
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(text = text, fontSize = 11.sp, fontFamily = PretendardFontFamily, color = textColor)
    }
}

@Composable
private fun InsightCard(title: String, items: List<String>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(0.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFDFCFA)),
        border = BorderStroke(1.dp, Color(0xFFEDEDED)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier.height(IntrinsicSize.Min).padding(horizontal = 16.dp, vertical = 18.dp),
        ) {
            Text(
                text = title,
                fontSize = 13.sp,
                lineHeight = 19.sp,
                fontFamily = PretendardSemiBoldFontFamily,
                color = TextPrimary,
                modifier = Modifier.width(80.dp),
            )
            Box(modifier = Modifier.width(1.dp).fillMaxHeight().background(Color(0xFFE5E5E5)))
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items.forEach { item ->
                    Text(
                        text = "• $item",
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        fontFamily = PretendardFontFamily,
                        color = TextSecondary,
                    )
                }
            }
        }
    }
}

@Composable
private fun CompatibleTypesCard(types: List<CompatibleTravelType>) {
    ResultDetailCard(title = "잘 맞는 여행 유형") {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            types.forEach { type ->
                CharacterDetailRow(
                    characterKey = type.code.toCharacterKey(),
                    characterName = type.name,
                    title = type.name,
                )
            }
        }
    }
}

@Composable
private fun TravelRoleCard(role: TravelRole, characterKey: String?, characterName: String) {
    ResultDetailCard(title = "여행에서 맡는 역할") {
        CharacterDetailRow(
            characterKey = characterKey,
            characterName = characterName,
            title = role.title,
            description = role.description,
        )
    }
}

@Composable
private fun ResultDetailCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFDFCFA)),
        border = BorderStroke(1.dp, Color(0xFFEDEDED)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontFamily = PretendardSemiBoldFontFamily,
                color = TextSecondary,
            )
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun CharacterDetailRow(
    characterKey: String?,
    characterName: String,
    title: String,
    description: String? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFF6F6F8))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        UserCharacterAvatar(
            characterKey = characterKey,
            contentDescription = "$characterName 캐릭터",
            modifier = Modifier.size(56.dp),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                fontFamily = PretendardSemiBoldFontFamily,
                color = TextPrimary,
            )
            description?.let {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = it,
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    fontFamily = PretendardFontFamily,
                    color = TextSecondary,
                )
            }
        }
    }
}

private fun String.toCharacterKey(): String = "character_${lowercase()}"

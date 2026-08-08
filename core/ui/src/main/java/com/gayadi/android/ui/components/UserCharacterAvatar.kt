package com.gayadi.android.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.gayadi.android.core.designsystem.R

/** Maps every persisted survey character key to a bundled shared drawable. */
@DrawableRes
fun characterDrawableFor(characterKey: String?): Int = when (characterKey) {
    "character_pna" -> R.drawable.character_pna
    "character_pnr" -> R.drawable.character_pnr
    "character_pca" -> R.drawable.character_pca
    "character_pcr" -> R.drawable.character_pcr
    "character_sna" -> R.drawable.character_sna
    "character_snr" -> R.drawable.character_snr
    "character_sca" -> R.drawable.character_sca
    "character_scr" -> R.drawable.character_scr
    else -> R.drawable.character_sca
}

/** Shared profile avatar used by every profile-aware feature. */
@Composable
fun UserCharacterAvatar(
    characterKey: String?,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.clip(CircleShape).background(Color(0xFFF0F0F0)),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(characterDrawableFor(characterKey)),
            contentDescription = contentDescription,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit,
        )
    }
}

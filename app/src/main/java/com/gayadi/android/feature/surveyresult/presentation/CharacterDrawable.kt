package com.gayadi.android.feature.surveyresult.presentation

import androidx.annotation.DrawableRes
import com.gayadi.android.R

/**
 * Maps the Firestore `characterKey` to the character illustration bundled in the APK.
 *
 * Returns null when the key is missing or unknown so the caller can fall back to the
 * result emoji. The mapping is explicit rather than name-based lookup so resource
 * shrinking keeps every referenced drawable.
 */
@DrawableRes
internal fun characterDrawableFor(characterKey: String?): Int? = when (characterKey) {
    "character_pna" -> R.drawable.character_pna
    "character_pnr" -> R.drawable.character_pnr
    "character_pca" -> R.drawable.character_pca
    "character_pcr" -> R.drawable.character_pcr
    "character_sna" -> R.drawable.character_sna
    "character_snr" -> R.drawable.character_snr
    "character_sca" -> R.drawable.character_sca
    "character_scr" -> R.drawable.character_scr
    else -> null
}

package com.gayadi.android.feature.surveyresult.presentation

import androidx.annotation.DrawableRes
import com.gayadi.android.ui.components.characterDrawableFor as sharedCharacterDrawableFor

/**
 * Maps the Firestore `characterKey` to the character illustration bundled in the APK.
 *
 * Returns null when the key is missing or unknown so the caller can fall back to the
 * result emoji. The mapping is explicit rather than name-based lookup so resource
 * shrinking keeps every referenced drawable.
 */
@DrawableRes
internal fun characterDrawableFor(characterKey: String?): Int =
    sharedCharacterDrawableFor(characterKey)

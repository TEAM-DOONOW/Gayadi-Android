package com.gayadi.android.ui.components

import com.gayadi.android.core.designsystem.R
import org.junit.Assert.assertEquals
import org.junit.Test

class UserCharacterAvatarTest {
    @Test
    fun unknownCharacterKey_usesSafeDefaultCharacter() {
        assertEquals(R.drawable.character_sca, characterDrawableFor("unknown"))
        assertEquals(R.drawable.character_sca, characterDrawableFor(null))
    }
}

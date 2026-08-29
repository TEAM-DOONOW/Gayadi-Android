package com.gayadi.android.navigation

import com.gayadi.android.domain.model.UserProfile
import org.junit.Assert.assertEquals
import org.junit.Test

class StartupDestinationTest {
    @Test
    fun newUserStartsOnLogin() {
        assertEquals(Routes.LOGIN, resolveStartupDestination(null))
        assertEquals(Routes.LOGIN, resolveStartupDestination(UserProfile("", "")))
    }

    @Test
    fun profileWithoutIntroductionResumesBasicInfo() {
        assertEquals(Routes.BASIC_INFO, resolveStartupDestination(UserProfile("미르", "")))
    }

    @Test
    fun basicProfileResumesSurvey() {
        assertEquals(Routes.SURVEY, resolveStartupDestination(UserProfile("미르", "여행 좋아요")))
    }

    @Test
    fun completedProfileSkipsOnboarding() {
        val profile = UserProfile("미르", "여행 좋아요", characterKey = "character_pca")

        assertEquals(Routes.MY_TRIP, resolveStartupDestination(profile))
    }
}

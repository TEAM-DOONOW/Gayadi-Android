package com.gayadi.android.ui.screens

import com.gayadi.android.domain.model.BasicInfo
import com.gayadi.android.domain.model.SurveyResult
import com.gayadi.android.domain.model.UserProfile
import com.gayadi.android.domain.repository.ProfileRepository
import com.gayadi.android.domain.usecase.GetUserProfileUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlinx.coroutines.Dispatchers

class RealtimeHomeViewModelTest {
    @Test
    fun suggestion_acceptAndRejectAreAppliedOnlyOnce() {
        val viewModel = RealtimeHomeViewModel(
            GetUserProfileUseCase(FakeProfileRepository()),
            Dispatchers.Unconfined,
        )

        viewModel.openRescheduleSuggestion()
        assertTrue(viewModel.uiState.value.showRescheduleSheet)

        viewModel.acceptRescheduleSuggestion()
        assertEquals(RescheduleDecision.ACCEPTED, viewModel.uiState.value.rescheduleDecision)
        assertFalse(viewModel.uiState.value.showRescheduleSheet)

        viewModel.rejectRescheduleSuggestion()
        assertEquals(RescheduleDecision.ACCEPTED, viewModel.uiState.value.rescheduleDecision)

        viewModel.openRescheduleSuggestion()
        assertFalse(viewModel.uiState.value.showRescheduleSheet)
    }
}

private class FakeProfileRepository : ProfileRepository {
    override suspend fun saveBasicInfo(basicInfo: BasicInfo) = Unit
    override suspend fun getBasicInfo(): BasicInfo? = null
    override suspend fun saveSurveyResult(result: SurveyResult): Result<Unit> = Result.success(Unit)
    override suspend fun getProfile(): UserProfile = UserProfile("가야디", "여행가")
    override suspend fun clearProfile(): Result<Unit> = Result.success(Unit)
}

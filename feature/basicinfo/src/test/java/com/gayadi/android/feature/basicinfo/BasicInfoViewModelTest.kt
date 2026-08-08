package com.gayadi.android.feature.basicinfo

import com.gayadi.android.domain.model.BasicInfo
import com.gayadi.android.domain.model.SurveyResult
import com.gayadi.android.domain.model.UserProfile
import com.gayadi.android.domain.repository.ProfileRepository
import com.gayadi.android.domain.usecase.SaveBasicInfoUseCase
import com.gayadi.android.feature.basicinfo.presentation.BasicInfoUiEvent
import com.gayadi.android.feature.basicinfo.presentation.BasicInfoViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlinx.coroutines.Dispatchers

/** Verifies basic information state transitions and validation. */
class BasicInfoViewModelTest {
    /** Input events enforce limits and enable valid submission. */
    @Test
    fun inputEvents_limitTextAndSubmitValidForm() {
        val repository = RecordingProfileRepository()
        val viewModel = BasicInfoViewModel(SaveBasicInfoUseCase(repository), Dispatchers.Unconfined)

        viewModel.onEvent(BasicInfoUiEvent.NicknameChanged("12345678901"))
        viewModel.onEvent(BasicInfoUiEvent.IntroductionChanged("123456789012345678901"))

        assertEquals(10, viewModel.uiState.value.nickname.length)
        assertEquals(20, viewModel.uiState.value.introduction.length)
        assertTrue(viewModel.uiState.value.canSubmit)
        assertTrue(viewModel.onEvent(BasicInfoUiEvent.Submit))
        assertEquals(viewModel.uiState.value.nickname, repository.saved?.nickname)
    }

    /** Empty required fields block submission. */
    @Test
    fun submitEvent_rejectsEmptyForm() {
        val viewModel = BasicInfoViewModel(SaveBasicInfoUseCase(RecordingProfileRepository()), Dispatchers.Unconfined)

        assertFalse(viewModel.onEvent(BasicInfoUiEvent.Submit))
    }
}

private class RecordingProfileRepository : ProfileRepository {
    var saved: BasicInfo? = null
    override fun saveBasicInfo(basicInfo: BasicInfo) { saved = basicInfo }
    override fun getBasicInfo(): BasicInfo? = saved
    override fun saveSurveyResult(result: SurveyResult) = Unit
    override fun getProfile(): UserProfile? = saved?.let {
        UserProfile(nickname = it.nickname, introduction = it.introduction)
    }
    override fun clearProfile(): Result<Unit> = Result.success(Unit)
}

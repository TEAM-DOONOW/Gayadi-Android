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
        viewModel.onEvent(BasicInfoUiEvent.Submit)
        assertEquals(viewModel.uiState.value.nickname, repository.saved?.nickname)
        assertTrue(viewModel.uiState.value.saveCompleted)
        assertFalse(viewModel.uiState.value.isSaving)
    }

    /** Empty required fields block submission. */
    @Test
    fun submitEvent_rejectsEmptyForm() {
        val viewModel = BasicInfoViewModel(SaveBasicInfoUseCase(RecordingProfileRepository()), Dispatchers.Unconfined)

        viewModel.onEvent(BasicInfoUiEvent.Submit)

        assertFalse(viewModel.uiState.value.saveCompleted)
    }

    @Test
    fun submitEvent_reportsPersistenceFailure() {
        val repository = RecordingProfileRepository(failure = IllegalStateException("저장 실패"))
        val viewModel = BasicInfoViewModel(SaveBasicInfoUseCase(repository), Dispatchers.Unconfined)
        viewModel.onEvent(BasicInfoUiEvent.NicknameChanged("가야디"))
        viewModel.onEvent(BasicInfoUiEvent.IntroductionChanged("여행가"))

        viewModel.onEvent(BasicInfoUiEvent.Submit)

        assertFalse(viewModel.uiState.value.saveCompleted)
        assertFalse(viewModel.uiState.value.isSaving)
        assertEquals("저장 실패", viewModel.uiState.value.errorMessage)
    }
}

private class RecordingProfileRepository(
    private val failure: Throwable? = null,
) : ProfileRepository {
    var saved: BasicInfo? = null
    override fun saveBasicInfo(basicInfo: BasicInfo) {
        failure?.let { throw it }
        saved = basicInfo
    }
    override fun getBasicInfo(): BasicInfo? = saved
    override fun saveSurveyResult(result: SurveyResult): Result<Unit> = Result.success(Unit)
    override fun getProfile(): UserProfile? = saved?.let {
        UserProfile(nickname = it.nickname, introduction = it.introduction)
    }
    override fun clearProfile(): Result<Unit> = Result.success(Unit)
}

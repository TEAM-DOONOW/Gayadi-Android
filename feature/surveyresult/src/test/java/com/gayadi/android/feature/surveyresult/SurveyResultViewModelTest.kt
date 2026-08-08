package com.gayadi.android.feature.surveyresult

import com.gayadi.android.domain.FakeSurveyRepository
import com.gayadi.android.domain.createSurveyDefinition
import com.gayadi.android.domain.model.BasicInfo
import com.gayadi.android.domain.model.SurveyResult
import com.gayadi.android.domain.model.UserProfile
import com.gayadi.android.domain.repository.ProfileRepository
import com.gayadi.android.domain.usecase.GetBasicInfoUseCase
import com.gayadi.android.domain.usecase.GetSurveyResultUseCase
import com.gayadi.android.domain.usecase.SaveSurveyResultToProfileUseCase
import com.gayadi.android.feature.surveyresult.presentation.SurveyResultViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test
import kotlinx.coroutines.Dispatchers

/** Profile stub returning whatever onboarding state a test needs. */
private class FakeProfileRepository(
    private val basicInfo: BasicInfo?,
    private val saveResult: Result<Unit> = Result.success(Unit),
) : ProfileRepository {
    var savedResult: SurveyResult? = null
    override suspend fun saveBasicInfo(basicInfo: BasicInfo) = Unit

    override suspend fun getBasicInfo(): BasicInfo? = basicInfo
    override suspend fun saveSurveyResult(result: SurveyResult): Result<Unit> {
        if (saveResult.isSuccess) savedResult = result
        return saveResult
    }
    override suspend fun getProfile(): UserProfile? = basicInfo?.let {
        UserProfile(nickname = it.nickname, introduction = it.introduction)
    }
    override suspend fun clearProfile(): Result<Unit> = Result.success(Unit)
}

private fun profileRepository(nickname: String?) =
    FakeProfileRepository(nickname?.let { BasicInfo(nickname = it, introduction = "") })

/** Verifies Firestore-backed result loading, nickname greeting, and retry behavior. */
class SurveyResultViewModelTest {
    @Test
    fun initialization_loadsRequestedResult() {
        val definition = createSurveyDefinition()
        val profileRepository = profileRepository("민수")
        val viewModel = SurveyResultViewModel(
            "SCA",
            GetSurveyResultUseCase(FakeSurveyRepository(Result.success(definition))),
            GetBasicInfoUseCase(profileRepository),
            SaveSurveyResultToProfileUseCase(profileRepository),
            Dispatchers.Unconfined,
        )

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals("SCA", viewModel.uiState.value.result?.code)
        assertEquals("민수", viewModel.uiState.value.nickname)
        assertEquals("SCA", profileRepository.savedResult?.code)
    }

    @Test
    fun initialization_withoutSavedProfile_leavesNicknameNull() {
        val profileRepository = profileRepository(null)
        val viewModel = SurveyResultViewModel(
            "SCA",
            GetSurveyResultUseCase(FakeSurveyRepository(Result.success(createSurveyDefinition()))),
            GetBasicInfoUseCase(profileRepository),
            SaveSurveyResultToProfileUseCase(profileRepository),
            Dispatchers.Unconfined,
        )

        assertNull(viewModel.uiState.value.nickname)
    }

    @Test
    fun initialization_withBlankNickname_leavesNicknameNull() {
        val profileRepository = profileRepository("   ")
        val viewModel = SurveyResultViewModel(
            "SCA",
            GetSurveyResultUseCase(FakeSurveyRepository(Result.success(createSurveyDefinition()))),
            GetBasicInfoUseCase(profileRepository),
            SaveSurveyResultToProfileUseCase(profileRepository),
            Dispatchers.Unconfined,
        )

        assertNull(viewModel.uiState.value.nickname)
    }

    @Test
    fun initialization_whenProfileSaveFails_showsErrorAndStopsLoading() {
        val profileRepository = FakeProfileRepository(
            BasicInfo("민수", "여행가"),
            Result.failure(IllegalStateException("프로필 저장 실패")),
        )
        val viewModel = SurveyResultViewModel(
            "SCA",
            GetSurveyResultUseCase(FakeSurveyRepository(Result.success(createSurveyDefinition()))),
            GetBasicInfoUseCase(profileRepository),
            SaveSurveyResultToProfileUseCase(profileRepository),
            Dispatchers.Unconfined,
        )

        assertFalse(viewModel.uiState.value.isLoading)
        assertNull(viewModel.uiState.value.result)
        assertNull(profileRepository.savedResult)
        assertEquals("프로필 저장 실패", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun retry_recoversFromFailure() {
        val repository = FakeSurveyRepository(Result.failure(IllegalStateException("network")))
        val profileRepository = profileRepository("민수")
        val viewModel = SurveyResultViewModel(
            "SCA",
            GetSurveyResultUseCase(repository),
            GetBasicInfoUseCase(profileRepository),
            SaveSurveyResultToProfileUseCase(profileRepository),
            Dispatchers.Unconfined,
        )

        assertNull(viewModel.uiState.value.result)
        assertEquals("network", viewModel.uiState.value.errorMessage)

        repository.surveyResult = Result.success(createSurveyDefinition())
        viewModel.retry()

        assertEquals("SCA", viewModel.uiState.value.result?.code)
        assertEquals("민수", viewModel.uiState.value.nickname)
    }
}

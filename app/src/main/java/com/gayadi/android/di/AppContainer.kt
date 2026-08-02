package com.gayadi.android.di

import com.gayadi.android.data.repository.InMemoryProfileRepository
import com.gayadi.android.data.repository.MockSurveyRepository
import com.gayadi.android.domain.repository.ProfileRepository
import com.gayadi.android.domain.repository.SurveyRepository
import com.gayadi.android.domain.usecase.GetSurveyQuestionsUseCase
import com.gayadi.android.domain.usecase.SaveBasicInfoUseCase

class AppContainer {
    private val profileRepository: ProfileRepository = InMemoryProfileRepository()
    private val surveyRepository: SurveyRepository = MockSurveyRepository()

    val saveBasicInfoUseCase = SaveBasicInfoUseCase(profileRepository)
    val getSurveyQuestionsUseCase = GetSurveyQuestionsUseCase(surveyRepository)
}

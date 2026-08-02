package com.gayadi.android.di

import com.gayadi.android.data.repository.InMemoryProfileRepository
import com.gayadi.android.data.repository.MockSurveyRepository
import com.gayadi.android.data.datasource.InMemoryProfileLocalDataSource
import com.gayadi.android.data.datasource.MockSurveyDataSource
import com.gayadi.android.domain.repository.ProfileRepository
import com.gayadi.android.domain.repository.SurveyRepository
import com.gayadi.android.domain.usecase.GetSurveyQuestionsUseCase
import com.gayadi.android.domain.usecase.SaveBasicInfoUseCase

/** Application composition root that wires data implementations to domain use cases. */
class AppContainer {
    private val profileRepository: ProfileRepository =
        InMemoryProfileRepository(InMemoryProfileLocalDataSource())
    private val surveyRepository: SurveyRepository =
        MockSurveyRepository(MockSurveyDataSource())

    /** Use case used to persist onboarding profile input. */
    val saveBasicInfoUseCase = SaveBasicInfoUseCase(profileRepository)

    /** Use case used to retrieve travel survey questions. */
    val getSurveyQuestionsUseCase = GetSurveyQuestionsUseCase(surveyRepository)
}

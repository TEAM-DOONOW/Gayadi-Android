package com.gayadi.android.di

import com.gayadi.android.data.repository.InMemoryProfileRepository
import com.gayadi.android.data.datasource.InMemoryProfileLocalDataSource
import com.gayadi.android.data.datasource.FirestoreSurveyDataSource
import com.gayadi.android.data.repository.DefaultSurveyRepository
import com.gayadi.android.domain.repository.ProfileRepository
import com.gayadi.android.domain.repository.SurveyRepository
import com.gayadi.android.domain.usecase.CalculateSurveyResultUseCase
import com.gayadi.android.domain.usecase.GetSurveyResultUseCase
import com.gayadi.android.domain.usecase.GetSurveyUseCase
import com.gayadi.android.domain.usecase.SaveBasicInfoUseCase
import com.google.firebase.firestore.FirebaseFirestore

/** Application composition root that wires data implementations to domain use cases. */
class AppContainer {
    private val profileRepository: ProfileRepository =
        InMemoryProfileRepository(InMemoryProfileLocalDataSource())
    private val surveyRepository: SurveyRepository =
        DefaultSurveyRepository(FirestoreSurveyDataSource(FirebaseFirestore.getInstance()))

    /** Use case used to persist onboarding profile input. */
    val saveBasicInfoUseCase = SaveBasicInfoUseCase(profileRepository)

    /** Use case used to retrieve the Firestore-backed travel survey. */
    val getSurveyUseCase = GetSurveyUseCase(surveyRepository)

    /** Pure use case used to calculate one of the eight survey results. */
    val calculateSurveyResultUseCase = CalculateSurveyResultUseCase()

    /** Use case used to retrieve one result card from Firestore. */
    val getSurveyResultUseCase = GetSurveyResultUseCase(surveyRepository)
}

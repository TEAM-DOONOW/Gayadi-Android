package com.gayadi.android.di

import com.gayadi.android.data.repository.InMemoryProfileRepository
import com.gayadi.android.data.repository.FileTravelRepository
import com.gayadi.android.data.datasource.FileProfileLocalDataSource
import com.gayadi.android.data.datasource.FirestoreSurveyDataSource
import com.gayadi.android.data.repository.DefaultSurveyRepository
import com.gayadi.android.domain.repository.ProfileRepository
import com.gayadi.android.domain.repository.SurveyRepository
import com.gayadi.android.domain.usecase.CalculateSurveyResultUseCase
import com.gayadi.android.domain.usecase.ClearUserProfileUseCase
import com.gayadi.android.domain.usecase.GetBasicInfoUseCase
import com.gayadi.android.domain.usecase.GetSurveyResultUseCase
import com.gayadi.android.domain.usecase.GetSurveyUseCase
import com.gayadi.android.domain.usecase.SaveBasicInfoUseCase
import com.gayadi.android.domain.usecase.GetUserProfileUseCase
import com.gayadi.android.domain.usecase.SaveSurveyResultToProfileUseCase
import com.gayadi.android.domain.usecase.GetTravelStateUseCase
import com.gayadi.android.domain.usecase.SaveTravelStateUseCase
import com.google.firebase.firestore.FirebaseFirestore
import java.io.File

/** Application composition root that wires data implementations to domain use cases. */
class AppContainer(profileFile: File, travelFile: File) {
    private val profileRepository: ProfileRepository =
        InMemoryProfileRepository(FileProfileLocalDataSource(profileFile))
    private val surveyRepository: SurveyRepository =
        DefaultSurveyRepository(FirestoreSurveyDataSource(FirebaseFirestore.getInstance()))
    private val travelRepository = FileTravelRepository(travelFile)

    /** Use case used to persist onboarding profile input. */
    val saveBasicInfoUseCase = SaveBasicInfoUseCase(profileRepository)

    /** Use case used to read the saved nickname for the result greeting. */
    val getBasicInfoUseCase = GetBasicInfoUseCase(profileRepository)

    /** Use case used by profile-aware screens. */
    val getUserProfileUseCase = GetUserProfileUseCase(profileRepository)

    /** Use case used to remove local profile data after account deletion. */
    val clearUserProfileUseCase = ClearUserProfileUseCase(profileRepository)

    /** Use case used to attach the completed survey to the local profile. */
    val saveSurveyResultToProfileUseCase = SaveSurveyResultToProfileUseCase(profileRepository)

    /** Reads all locally persisted trip, invitation, schedule, and favorite state. */
    val getTravelStateUseCase = GetTravelStateUseCase(travelRepository)

    /** Atomically persists the complete Android-local travel aggregate. */
    val saveTravelStateUseCase = SaveTravelStateUseCase(travelRepository)

    /** Use case used to retrieve the Firestore-backed travel survey. */
    val getSurveyUseCase = GetSurveyUseCase(surveyRepository)

    /** Pure use case used to calculate one of the eight survey results. */
    val calculateSurveyResultUseCase = CalculateSurveyResultUseCase()

    /** Use case used to retrieve one result card from Firestore. */
    val getSurveyResultUseCase = GetSurveyResultUseCase(surveyRepository)
}

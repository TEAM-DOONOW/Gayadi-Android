package com.gayadi.android.di

import com.gayadi.android.data.repository.InMemoryProfileRepository
import com.gayadi.android.data.repository.FileTravelRepository
import com.gayadi.android.data.repository.DefaultTourRepository
import com.gayadi.android.data.datasource.HttpTourApiDataSource
import com.gayadi.android.data.datasource.HttpAuthApiDataSource
import com.gayadi.android.data.datasource.HttpProfileApiDataSource
import com.gayadi.android.data.datasource.FileProfileLocalDataSource
import com.gayadi.android.data.datasource.RestSurveyDataSource
import com.gayadi.android.data.datasource.RestSessionApiDataSource
import com.gayadi.android.data.datasource.RestInquiryDataSource
import com.gayadi.android.data.datasource.GayadiApiClient
import com.gayadi.android.data.repository.RestSurveySubmissionRepository
import com.gayadi.android.domain.usecase.SubmitSurveyUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Dispatchers
import com.gayadi.android.data.repository.DefaultSurveyRepository
import com.gayadi.android.data.repository.DefaultLegalDocumentRepository
import com.gayadi.android.data.repository.DefaultInquiryRepository
import com.gayadi.android.data.repository.DefaultNoticeRepository
import com.gayadi.android.data.repository.DefaultAuthRepository
import com.gayadi.android.data.repository.AuthenticatedProfileRepository
import com.gayadi.android.data.repository.EncryptedFileAuthSessionStore
import com.gayadi.android.data.repository.FirestoreTripInviteRepository
import com.gayadi.android.data.datasource.RestPublicContentDataSource
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
import com.gayadi.android.domain.usecase.GetLegalDocumentUseCase
import com.gayadi.android.domain.usecase.GetNoticeUseCase
import com.gayadi.android.domain.usecase.GetNoticesUseCase
import com.gayadi.android.domain.usecase.JoinTripByInviteCodeUseCase
import com.gayadi.android.domain.usecase.PublishTripInviteUseCase
import com.gayadi.android.domain.usecase.ObserveSharedTripInviteUseCase
import com.gayadi.android.domain.usecase.RemoveSharedTripParticipantUseCase
import com.gayadi.android.domain.usecase.SubmitSharedTripAvailabilityUseCase
import com.gayadi.android.domain.usecase.FinalizeSharedTripDatesUseCase
import com.gayadi.android.domain.usecase.SaveTravelStateUseCase
import com.gayadi.android.domain.usecase.SubmitInquiryUseCase
import com.gayadi.android.domain.usecase.SignInWithGoogleUseCase
import com.gayadi.android.domain.usecase.UpdateTravelStateUseCase
import com.gayadi.android.domain.usecase.GetTourPlacesUseCase
import com.gayadi.android.domain.usecase.GetNearbyTourPlacesUseCase
import com.gayadi.android.domain.usecase.SearchTourPlacesUseCase
import com.google.firebase.firestore.FirebaseFirestore
import java.io.File
import java.util.UUID

/** Application composition root that wires data implementations to domain use cases. */
class AppContainer(
    profileFile: File,
    travelFile: File,
    tourApiBaseUrl: String,
    appVersion: String = DEFAULT_APP_VERSION,
) {
    private val firestore = FirebaseFirestore.getInstance()
    private val localProfileRepository: ProfileRepository =
        InMemoryProfileRepository(FileProfileLocalDataSource(profileFile))
    private val publicContentDataSource = RestPublicContentDataSource(tourApiBaseUrl)
    private val legalDocumentRepository =
        DefaultLegalDocumentRepository(publicContentDataSource)
    private val noticeRepository = DefaultNoticeRepository(publicContentDataSource)
    private val travelRepository = FileTravelRepository(travelFile)
    private val tourRepository = DefaultTourRepository(com.gayadi.android.data.datasource.ServerPlaceApiDataSource(GayadiApiClient(tourApiBaseUrl)))
    val authRepository: com.gayadi.android.domain.repository.AuthRepository = DefaultAuthRepository(
        HttpAuthApiDataSource(tourApiBaseUrl),
        EncryptedFileAuthSessionStore(File(travelFile.parentFile, "auth-session")),
    )
    private val apiScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val api = GayadiApiClient(tourApiBaseUrl, authRepository)
    val friendshipGateway: com.gayadi.android.domain.repository.FriendshipGateway = com.gayadi.android.data.remote.travel.ServerFriendshipGateway(api)
    val travelGateway: com.gayadi.android.domain.repository.TravelGateway = com.gayadi.android.data.remote.travel.ServerTravelGateway(api)
    private val surveyRepository: SurveyRepository =
        DefaultSurveyRepository(RestSurveyDataSource(api, apiScope))
    val submitSurveyUseCase = SubmitSurveyUseCase(RestSurveySubmissionRepository(api))
    private val profileRepository: ProfileRepository = AuthenticatedProfileRepository(
        localRepository = localProfileRepository,
        apiDataSource = HttpProfileApiDataSource(api),
        authRepository = authRepository,
    )
    private val installationId = loadInstallationId(File(travelFile.parentFile, "installation-id"))
    private val tripInviteRepository = FirestoreTripInviteRepository(firestore, installationId)
    private val inquiryRepository =
        DefaultInquiryRepository(RestInquiryDataSource(api, apiScope))

    /** Use case used to persist onboarding profile input. */
    val saveBasicInfoUseCase = SaveBasicInfoUseCase(profileRepository)

    /** Use case used to read the saved nickname for the result greeting. */
    val getBasicInfoUseCase = GetBasicInfoUseCase(profileRepository)

    /** Use case used by profile-aware screens. */
    val getUserProfileUseCase = GetUserProfileUseCase(profileRepository)

    /** Deletes the backend account and clears its cached profile. */
    val clearUserProfileUseCase = ClearUserProfileUseCase(profileRepository)

    /** Use case used to attach the completed survey to the local profile. */
    val saveSurveyResultToProfileUseCase = SaveSurveyResultToProfileUseCase(profileRepository)

    /** Reads all locally persisted trip, invitation, schedule, and favorite state. */
    val getTravelStateUseCase = GetTravelStateUseCase(travelRepository)

    /** Atomically persists the complete Android-local travel aggregate. */
    val saveTravelStateUseCase = SaveTravelStateUseCase(travelRepository)

    /** Atomically updates the Android-local travel aggregate without lost writes. */
    val updateTravelStateUseCase = UpdateTravelStateUseCase(travelRepository)

    /** Resolves a persisted trip invite code and joins the local user to that trip. */
    val joinTripByInviteCodeUseCase = JoinTripByInviteCodeUseCase(travelRepository, travelGateway = travelGateway)

    /** Publishes one local trip so another installation can resolve and join its invite code. */
    val publishTripInviteUseCase = PublishTripInviteUseCase(tripInviteRepository)

    val observeSharedTripInviteUseCase = ObserveSharedTripInviteUseCase(tripInviteRepository)
    val removeSharedTripParticipantUseCase = RemoveSharedTripParticipantUseCase(tripInviteRepository)
    val submitSharedTripAvailabilityUseCase = SubmitSharedTripAvailabilityUseCase(tripInviteRepository)
    val finalizeSharedTripDatesUseCase = FinalizeSharedTripDatesUseCase(tripInviteRepository)

    /** Use case used to retrieve the backend travel survey. */
    val getSurveyUseCase = GetSurveyUseCase(surveyRepository)

    /** Pure use case used to calculate one of the eight survey results. */
    val calculateSurveyResultUseCase = CalculateSurveyResultUseCase()

    /** Use case used to retrieve one result card from the backend. */
    val getSurveyResultUseCase = GetSurveyResultUseCase(surveyRepository)

    /** Loads the published terms or privacy policy from the backend. */
    val getLegalDocumentUseCase = GetLegalDocumentUseCase(legalDocumentRepository)

    /** Loads the backend update notices shown in the settings screen. */
    val getNoticesUseCase = GetNoticesUseCase(noticeRepository)

    /** Loads one backend update notice for its detail screen. */
    val getNoticeUseCase = GetNoticeUseCase(noticeRepository)

    /** Sends a support inquiry written by the user to the backend. */
    val submitInquiryUseCase = SubmitInquiryUseCase(inquiryRepository)

    /** Exchanges a Google ID Token for a Gayadi API session. */
    val signInWithGoogleUseCase = SignInWithGoogleUseCase(authRepository)

    /** Loads and caches the tourism places exposed by the Gayadi backend. */
    val getTourPlacesUseCase = GetTourPlacesUseCase(tourRepository)
    val getNearbyTourPlacesUseCase = GetNearbyTourPlacesUseCase(tourRepository)
    val searchTourPlacesUseCase = SearchTourPlacesUseCase(tourRepository)

    /** Revokes the current backend session before clearing account data on this device. */
    suspend fun logout(): Result<Unit> = try {
        val session = requireNotNull(authRepository.currentSession()) { "로그인 세션이 없어요." }
        RestSessionApiDataSource(api).logout(session.refreshToken)
        authRepository.clearSession()
        localProfileRepository.clearProfile().getOrThrow()
        Result.success(Unit)
    } catch (cancelled: kotlinx.coroutines.CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        Result.failure(error)
    }

    private companion object {
        const val DEFAULT_APP_VERSION = "1.0.0"

        fun loadInstallationId(file: File): String {
            val existing = file.takeIf(File::exists)?.readText()?.trim().orEmpty()
            if (existing.isNotBlank()) return existing
            val generated = UUID.randomUUID().toString()
            file.parentFile?.mkdirs()
            file.writeText(generated)
            return generated
        }
    }
}

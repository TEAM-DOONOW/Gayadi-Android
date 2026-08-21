package com.gayadi.android.navigation

import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.gayadi.android.domain.model.LegalDocumentType
import com.gayadi.android.ui.screens.InquiryRoute
import com.gayadi.android.ui.screens.InquiryViewModel
import com.gayadi.android.ui.screens.LegalDocumentRoute
import com.gayadi.android.ui.screens.LegalDocumentViewModel
import com.gayadi.android.ui.screens.MyPageScreen
import com.gayadi.android.ui.screens.MyTravelProfileScreen
import com.gayadi.android.ui.screens.NoticeDetailRoute
import com.gayadi.android.ui.screens.NoticeDetailViewModel
import com.gayadi.android.ui.screens.NoticeListRoute
import com.gayadi.android.ui.screens.NoticeListViewModel
import com.gayadi.android.ui.screens.ProfileViewModel
import com.gayadi.android.ui.screens.SettingsScreen
import com.gayadi.android.ui.screens.TravelProfileResultViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal fun NavGraphBuilder.myPageGraph(context: AppNavigationContext) = with(context) {
    composable(Routes.MY_PAGE) {
        val profileViewModel: ProfileViewModel = viewModel(
            factory = ProfileViewModel.factory(appContainer.getUserProfileUseCase),
        )
        val profileUiState by profileViewModel.uiState.collectAsStateWithLifecycle()
        MyPageScreen(
            uiState = profileUiState,
            onNavigateHome = {
                selectedTripId?.let { navController.navigate(Routes.realtimeHome(it)) }
                    ?: navController.navigate(Routes.MY_TRIP)
            },
            onNavigateSettings = { navController.navigate(Routes.SETTINGS) },
        )
    }
    composable(Routes.SETTINGS) {
        val returnToLogin = {
            navController.navigate(Routes.LOGIN) {
                popUpTo(navController.graph.id) { inclusive = true }
            }
        }
        SettingsScreen(
            uiState = sharedProfileUiState,
            onBack = { navController.popBackStack() },
            onOpenTravelProfile = { navController.navigate(Routes.MY_TRAVEL_PROFILE) },
            onOpenNotices = { navController.navigate(Routes.NOTICES) },
            onOpenInquiry = { navController.navigate(Routes.INQUIRY) },
            onOpenTerms = {
                navController.navigate(Routes.legalDocument(LegalDocumentType.TERMS_OF_SERVICE.documentId))
            },
            onOpenPrivacyPolicy = {
                navController.navigate(Routes.legalDocument(LegalDocumentType.PRIVACY_POLICY.documentId))
            },
            onLogout = returnToLogin,
            onDeleteAccount = {
                appScope.launch(Dispatchers.IO) {
                    appContainer.clearUserProfileUseCase().fold(
                        onSuccess = {
                            tripViewModel.clearAllTravelData().fold(
                                onSuccess = {
                                    withContext(Dispatchers.Main) {
                                        sharedProfileViewModel.reload()
                                        returnToLogin()
                                    }
                                },
                                onFailure = { error ->
                                    sharedProfileViewModel.showError(
                                        error.message ?: "여행 데이터를 삭제하지 못했어요",
                                    )
                                },
                            )
                        },
                        onFailure = { error ->
                            sharedProfileViewModel.showError(error.message ?: "프로필을 삭제하지 못했어요")
                        },
                    )
                }
            },
        )
    }
    composable(Routes.MY_TRAVEL_PROFILE) {
        val resultCode = sharedProfileUiState.profile?.resultCode
        val resultViewModel: TravelProfileResultViewModel = viewModel(
            key = "travel-profile-result-${resultCode.orEmpty()}",
            factory = TravelProfileResultViewModel.factory(
                resultCode = resultCode,
                getSurveyResult = appContainer.getSurveyResultUseCase,
            ),
        )
        val resultUiState by resultViewModel.uiState.collectAsStateWithLifecycle()
        MyTravelProfileScreen(
            uiState = sharedProfileUiState,
            resultUiState = resultUiState,
            onBack = { navController.popBackStack() },
            onRetry = sharedProfileViewModel::reload,
            onResultRetry = resultViewModel::retry,
        )
    }
    composable(Routes.NOTICES) {
        val noticeListViewModel: NoticeListViewModel = viewModel(
            factory = NoticeListViewModel.factory(appContainer.getNoticesUseCase),
        )
        NoticeListRoute(
            viewModel = noticeListViewModel,
            onBack = { navController.popBackStack() },
            onOpenNotice = { noticeId -> navController.navigate(Routes.noticeDetail(noticeId)) },
        )
    }
    composable(
        route = Routes.NOTICE_DETAIL,
        arguments = listOf(navArgument("noticeId") { type = NavType.StringType }),
    ) { backStackEntry ->
        val noticeId = requireNotNull(backStackEntry.arguments?.getString("noticeId"))
        val noticeDetailViewModel: NoticeDetailViewModel = viewModel(
            key = "notice-$noticeId",
            factory = NoticeDetailViewModel.factory(noticeId, appContainer.getNoticeUseCase),
        )
        NoticeDetailRoute(
            viewModel = noticeDetailViewModel,
            onBack = { navController.popBackStack() },
        )
    }
    composable(Routes.INQUIRY) {
        val inquiryViewModel: InquiryViewModel = viewModel(
            factory = InquiryViewModel.factory(appContainer.submitInquiryUseCase),
        )
        InquiryRoute(viewModel = inquiryViewModel, onBack = { navController.popBackStack() })
    }
    composable(
        route = Routes.LEGAL_DOCUMENT,
        arguments = listOf(navArgument("documentId") { type = NavType.StringType }),
    ) { backStackEntry ->
        val documentId = requireNotNull(backStackEntry.arguments?.getString("documentId"))
        val type = requireNotNull(LegalDocumentType.fromDocumentId(documentId)) {
            "지원하지 않는 법적 문서입니다: $documentId"
        }
        val legalViewModel: LegalDocumentViewModel = viewModel(
            key = "legal-$documentId",
            factory = LegalDocumentViewModel.factory(type, appContainer.getLegalDocumentUseCase),
        )
        LegalDocumentRoute(viewModel = legalViewModel, onBack = { navController.popBackStack() })
    }
}

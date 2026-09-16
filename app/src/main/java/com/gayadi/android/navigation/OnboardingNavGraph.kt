package com.gayadi.android.navigation

import android.util.Log
import com.gayadi.android.domain.model.LegalDocumentType
import com.gayadi.android.domain.error.rethrowCancellation
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialInterruptedException
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.gayadi.android.feature.basicinfo.presentation.BasicInfoRoute
import com.gayadi.android.feature.basicinfo.presentation.BasicInfoViewModel
import com.gayadi.android.feature.survey.presentation.SurveyRoute
import com.gayadi.android.feature.survey.presentation.SurveyViewModel
import com.gayadi.android.feature.surveyresult.presentation.SurveyResultRoute
import com.gayadi.android.feature.surveyresult.presentation.SurveyResultViewModel
import com.gayadi.android.ui.components.GayadiLoadingScreen
import com.gayadi.android.ui.screens.LoginScreen
import com.gayadi.android.BuildConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

internal fun NavGraphBuilder.onboardingGraph(context: AppNavigationContext) = with(context) {
    composable(Routes.STARTUP) {
        GayadiLoadingScreen()
        LaunchedEffect(sharedProfileUiState.isLoading, travelUiState.isLoading) {
            if (!sharedProfileUiState.isLoading && !travelUiState.isLoading) {
                navController.navigate(resolveStartupDestination(sharedProfileUiState.profile)) {
                    popUpTo(Routes.STARTUP) { inclusive = true }
                }
            }
        }
    }
    composable(Routes.LOGIN) {
        val androidContext = LocalContext.current
        var isLoginInProgress by remember { mutableStateOf(false) }
        var loginError by remember { mutableStateOf<String?>(null) }

        LoginScreen(
            isLoginInProgress = isLoginInProgress,
            loginError = loginError,
            onGoogleLogin = {
                if (!isLoginInProgress) {
                    appScope.launch {
                        isLoginInProgress = true
                        loginError = null
                        try {
                            val idToken = requestGoogleIdToken(
                                context = androidContext,
                                webClientId = BuildConfig.GOOGLE_WEB_CLIENT_ID,
                            )
                            Log.i(AUTH_LOG_TAG, "Google credential received")
                            appContainer.signInWithGoogleUseCase(idToken)
                            Log.i(AUTH_LOG_TAG, "Gayadi auth session received")
                            val profile = appContainer.getUserProfileUseCase()
                            sharedProfileViewModel.reload()
                            tripViewModel.retry()
                            placeViewModel.retry()
                            navController.navigate(
                                resolveAuthenticatedDestination(profile),
                            ) {
                                popUpTo(Routes.LOGIN) { inclusive = true }
                            }
                            Log.i(AUTH_LOG_TAG, "Post-login navigation requested")
                        } catch (exception: GetCredentialCancellationException) {
                            Log.i(
                                AUTH_LOG_TAG,
                                "Google credential flow cancelled: ${exception.message}",
                            )
                            loginError = GOOGLE_LOGIN_CANCELLED_MESSAGE
                        } catch (exception: GetCredentialInterruptedException) {
                            Log.i(
                                AUTH_LOG_TAG,
                                "Google credential flow interrupted: ${exception.message}",
                            )
                            loginError = GOOGLE_LOGIN_CANCELLED_MESSAGE
                        } catch (cancelled: CancellationException) {
                            throw cancelled
                        } catch (exception: Exception) {
                            exception.rethrowCancellation()
                            Log.e(
                                AUTH_LOG_TAG,
                                "Google login failed: ${exception::class.java.simpleName}",
                                exception,
                            )
                            loginError = googleLoginUserMessage(exception)
                        } finally {
                            isLoginInProgress = false
                        }
                    }
                }
            },
            onKakaoLogin = { loginError = "카카오 로그인은 준비 중이에요. Google 로그인을 이용해 주세요." },
            onOpenPrivacyPolicy = {
                navController.navigate(Routes.legalDocument(LegalDocumentType.PRIVACY_POLICY.documentId))
            },
            onOpenTerms = {
                navController.navigate(Routes.legalDocument(LegalDocumentType.TERMS_OF_SERVICE.documentId))
            },
        )
    }
    composable(Routes.BASIC_INFO) {
        val basicInfoViewModel: BasicInfoViewModel = viewModel(
            factory = BasicInfoViewModel.factory(appContainer.saveBasicInfoUseCase),
        )
        BasicInfoRoute(
            viewModel = basicInfoViewModel,
            onStartSurvey = { navController.navigate(Routes.SURVEY) },
        )
    }
    composable(Routes.SURVEY) {
        val surveyViewModel: SurveyViewModel = viewModel(
            factory = SurveyViewModel.factory(
                appContainer.getSurveyUseCase,
                appContainer.calculateSurveyResultUseCase,
                appContainer.submitSurveyUseCase,
            ),
        )
        SurveyRoute(
            viewModel = surveyViewModel,
            onComplete = { resultCode ->
                navController.navigate(Routes.surveyResult(resultCode)) {
                    popUpTo(Routes.SURVEY) { inclusive = true }
                }
            },
        )
    }
    composable(
        route = Routes.SURVEY_RESULT,
        arguments = listOf(navArgument("resultCode") { type = NavType.StringType }),
    ) { backStackEntry ->
        val resultCode = requireNotNull(backStackEntry.arguments?.getString("resultCode"))
        val resultViewModel: SurveyResultViewModel = viewModel(
            factory = SurveyResultViewModel.factory(
                resultCode,
                appContainer.getSurveyResultUseCase,
                appContainer.getBasicInfoUseCase,
                appContainer.saveSurveyResultToProfileUseCase,
            ),
        )
        SurveyResultRoute(
            viewModel = resultViewModel,
            onStart = {
                sharedProfileViewModel.reload()
                navController.navigate(Routes.MY_TRIP) {
                    popUpTo(Routes.LOGIN) { inclusive = true }
                }
            },
        )
    }
}

private const val AUTH_LOG_TAG = "GayadiGoogleAuth"

package com.gayadi.android.navigation

import androidx.compose.runtime.LaunchedEffect
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
import com.gayadi.android.ui.screens.AuthRoute
import com.gayadi.android.ui.screens.AuthViewModel

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
        val authViewModel: AuthViewModel = viewModel(
            factory = AuthViewModel.factory(appContainer.authRepository),
        )
        AuthRoute(
            viewModel = authViewModel,
            onAuthenticated = { completion ->
                sharedProfileViewModel.reload()
                tripViewModel.retry()
                val destination = when {
                    completion.isNewAccount -> Routes.BASIC_INFO
                    completion.session.user.introduction.isNullOrBlank() -> Routes.BASIC_INFO
                    completion.session.user.characterKey.isNullOrBlank() -> Routes.SURVEY
                    else -> Routes.MY_TRIP
                }
                navController.navigate(destination) {
                    popUpTo(Routes.LOGIN) { inclusive = true }
                }
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
                appContainer.submitSurveyAnswersUseCase,
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

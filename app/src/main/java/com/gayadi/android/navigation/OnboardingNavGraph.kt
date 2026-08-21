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
import com.gayadi.android.ui.screens.LoginScreen

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
        LoginScreen(
            onStart = { navController.navigate(Routes.BASIC_INFO) },
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

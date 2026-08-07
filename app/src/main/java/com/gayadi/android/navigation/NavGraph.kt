package com.gayadi.android.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.gayadi.android.di.AppContainer
import com.gayadi.android.feature.basicinfo.presentation.BasicInfoRoute
import com.gayadi.android.feature.basicinfo.presentation.BasicInfoViewModel
import com.gayadi.android.feature.survey.presentation.SurveyRoute
import com.gayadi.android.feature.survey.presentation.SurveyViewModel
import com.gayadi.android.feature.surveyresult.presentation.SurveyResultRoute
import com.gayadi.android.feature.surveyresult.presentation.SurveyResultViewModel
import com.gayadi.android.ui.screens.FriendAddScreen
import com.gayadi.android.ui.screens.LoginScreen
import com.gayadi.android.ui.screens.MyPageScreen
import com.gayadi.android.ui.screens.MyTripScreen
import com.gayadi.android.ui.screens.TripCreateScreen
import com.gayadi.android.ui.screens.TripViewModel
import com.gayadi.android.ui.screens.PlaceDetailScreen
import com.gayadi.android.ui.screens.PlaceSearchScreen
import com.gayadi.android.ui.screens.RealtimeHomeScreen
import com.gayadi.android.ui.screens.SettingsScreen

@Composable
fun GayadiNavHost(appContainer: AppContainer) {
    val navController = rememberNavController()
    val tripViewModel: TripViewModel = viewModel()
    val trips by tripViewModel.trips.collectAsStateWithLifecycle()

    NavHost(navController = navController, startDestination = Routes.LOGIN) {
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
                ),
            )
            SurveyResultRoute(
                viewModel = resultViewModel,
                onStart = {
                    navController.navigate(Routes.MY_TRIP) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
            )
        }
        composable(Routes.FRIEND_ADD) {
            FriendAddScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.PLACE_SEARCH) {
            PlaceSearchScreen(
                onBack = { navController.popBackStack() },
                onPlaceClick = { id -> navController.navigate(Routes.placeDetail(id)) },
            )
        }
        composable(Routes.PLACE_DETAIL) {
            PlaceDetailScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.MY_TRIP) {
            MyTripScreen(
                trips = trips,
                onAddTrip = { navController.navigate(Routes.TRIP_CREATE) },
                onDeleteTrip = tripViewModel::deleteTrip,
                onNavigateHome = { navController.navigate(Routes.REALTIME_HOME) { popUpTo(Routes.REALTIME_HOME) { inclusive = true } } },
            )
        }
        composable(Routes.TRIP_CREATE) {
            TripCreateScreen(
                onBack = { navController.popBackStack() },
                onCreate = { trip ->
                    tripViewModel.addTrip(trip)
                    navController.popBackStack()
                },
            )
        }
        composable(Routes.REALTIME_HOME) {
            RealtimeHomeScreen(
                onNavigateMyTrip = { navController.navigate(Routes.MY_TRIP) { popUpTo(Routes.REALTIME_HOME) { inclusive = true } } },
                onNavigateMyPage = { navController.navigate(Routes.MY_PAGE) { popUpTo(Routes.REALTIME_HOME) { inclusive = true } } },
            )
        }
        composable(Routes.MY_PAGE) {
            MyPageScreen(
                onNavigateHome = { navController.navigate(Routes.REALTIME_HOME) { popUpTo(Routes.REALTIME_HOME) { inclusive = true } } },
                onNavigateSettings = { navController.navigate(Routes.SETTINGS) },
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}

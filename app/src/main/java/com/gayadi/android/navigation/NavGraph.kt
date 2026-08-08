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
import com.gayadi.android.ui.screens.FriendAddViewModel
import com.gayadi.android.ui.screens.LoginScreen
import com.gayadi.android.ui.screens.MyPageScreen
import com.gayadi.android.ui.screens.ProfileViewModel
import com.gayadi.android.ui.screens.MyTripScreen
import com.gayadi.android.ui.screens.TripCreateScreen
import com.gayadi.android.ui.screens.TripViewModel
import com.gayadi.android.ui.screens.PlaceDetailScreen
import com.gayadi.android.ui.screens.PlaceSearchScreen
import com.gayadi.android.ui.screens.PlaceViewModel
import com.gayadi.android.ui.screens.RealtimeHomeScreen
import com.gayadi.android.ui.screens.RealtimeHomeViewModel
import com.gayadi.android.ui.screens.SettingsScreen

@Composable
fun GayadiNavHost(appContainer: AppContainer) {
    val navController = rememberNavController()
    val tripViewModel: TripViewModel = viewModel()
    val placeViewModel: PlaceViewModel = viewModel(factory = PlaceViewModel.factory())
    val trips by tripViewModel.trips.collectAsStateWithLifecycle()
    val selectedTripId by tripViewModel.selectedTripId.collectAsStateWithLifecycle()

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
                    appContainer.saveSurveyResultToProfileUseCase,
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
            val friendViewModel: FriendAddViewModel = viewModel(factory = FriendAddViewModel.factory())
            val friendUiState by friendViewModel.uiState.collectAsStateWithLifecycle()
            FriendAddScreen(
                uiState = friendUiState,
                onBack = { navController.popBackStack() },
                onQueryChange = friendViewModel::updateQuery,
                onAddFriend = friendViewModel::addFriend,
                onRetry = friendViewModel::retry,
            )
        }
        composable(Routes.PLACE_SEARCH) {
            val placeUiState by placeViewModel.uiState.collectAsStateWithLifecycle()
            PlaceSearchScreen(
                uiState = placeUiState,
                onBack = { navController.popBackStack() },
                onQueryChange = placeViewModel::updateQuery,
                onCategorySelected = placeViewModel::selectCategory,
                onPlaceClick = { id -> navController.navigate(Routes.placeDetail(id)) },
                onRetry = placeViewModel::retry,
            )
        }
        composable(
            route = Routes.PLACE_DETAIL,
            arguments = listOf(navArgument("placeId") { type = NavType.StringType }),
        ) { backStackEntry ->
            val placeId = requireNotNull(backStackEntry.arguments?.getString("placeId"))
            val placeUiState by placeViewModel.uiState.collectAsStateWithLifecycle()
            PlaceDetailScreen(
                place = placeViewModel.findPlace(placeId),
                isScheduled = placeId in placeUiState.scheduledPlaceIds,
                onBack = { navController.popBackStack() },
                onAddToSchedule = { placeViewModel.addPlaceToSchedule(placeId) },
            )
        }
        composable(Routes.MY_TRIP) {
            MyTripScreen(
                trips = trips,
                onAddTrip = { navController.navigate(Routes.TRIP_CREATE) },
                onDeleteTrip = tripViewModel::deleteTrip,
                onNavigateHome = { tripId ->
                    tripViewModel.selectTrip(tripId)
                    navController.navigate(Routes.realtimeHome(tripId))
                },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
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
        composable(
            route = Routes.REALTIME_HOME,
            arguments = listOf(navArgument("tripId") { type = NavType.StringType }),
        ) { backStackEntry ->
            val tripId = requireNotNull(backStackEntry.arguments?.getString("tripId"))
            val trip = tripViewModel.tripById(tripId)
            val homeViewModel: RealtimeHomeViewModel = viewModel(
                factory = RealtimeHomeViewModel.factory(appContainer.getUserProfileUseCase),
            )
            val homeUiState by homeViewModel.uiState.collectAsStateWithLifecycle()
            val placeUiState by placeViewModel.uiState.collectAsStateWithLifecycle()
            val nextScheduleName = placeUiState.scheduledPlaceIds.firstNotNullOfOrNull(placeViewModel::findPlace)?.name
            RealtimeHomeScreen(
                uiState = homeUiState,
                tripTitle = trip?.name ?: "선택한 여행",
                tripSubtitle = trip?.let { "${it.startDate} - ${it.endDate} · ${it.cities.joinToString(" · ")}" }.orEmpty(),
                nextScheduleName = nextScheduleName,
                onNavigateMyTrip = { navController.navigate(Routes.MY_TRIP) },
                onNavigateMyPage = { navController.navigate(Routes.MY_PAGE) },
                onNavigatePlaceSearch = { navController.navigate(Routes.PLACE_SEARCH) },
                onNavigateFriendAdd = { navController.navigate(Routes.FRIEND_ADD) },
                onOpenReschedule = homeViewModel::openRescheduleSuggestion,
                onDismissReschedule = homeViewModel::dismissRescheduleSuggestion,
                onAcceptReschedule = homeViewModel::acceptRescheduleSuggestion,
                onRejectReschedule = homeViewModel::rejectRescheduleSuggestion,
            )
        }
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
            val profileViewModel: ProfileViewModel = viewModel(
                factory = ProfileViewModel.factory(appContainer.getUserProfileUseCase),
            )
            val profileUiState by profileViewModel.uiState.collectAsStateWithLifecycle()
            val returnToLogin = {
                navController.navigate(Routes.LOGIN) {
                    popUpTo(navController.graph.id) { inclusive = true }
                }
            }
            SettingsScreen(
                uiState = profileUiState,
                onBack = { navController.popBackStack() },
                onEditProfile = { navController.navigate(Routes.BASIC_INFO) },
                onLogout = returnToLogin,
                onDeleteAccount = returnToLogin,
            )
        }
    }
}

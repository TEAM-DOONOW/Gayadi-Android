package com.gayadi.android.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
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
import com.gayadi.android.ui.screens.TripDetailScreen
import com.gayadi.android.ui.screens.ParticipantsScreen
import com.gayadi.android.ui.screens.InvitationScreen
import com.gayadi.android.ui.screens.ScheduleScreen
import com.gayadi.android.ui.screens.RouteHubScreen
import com.gayadi.android.ui.screens.RouteRecommendationScreen
import com.gayadi.android.ui.screens.RouteRecommendationType
import com.gayadi.android.ui.screens.NearbyPlacesScreen
import com.gayadi.android.ui.screens.FavoritePlacesScreen
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun GayadiNavHost(appContainer: AppContainer) {
    val navController = rememberNavController()
    val appScope = rememberCoroutineScope()
    val tripViewModel: TripViewModel = viewModel(
        factory = TripViewModel.factory(appContainer.getTravelStateUseCase, appContainer.saveTravelStateUseCase),
    )
    val placeViewModel: PlaceViewModel = viewModel(factory = PlaceViewModel.factory())
    val trips by tripViewModel.trips.collectAsStateWithLifecycle()
    val selectedTripId by tripViewModel.selectedTripId.collectAsStateWithLifecycle()
    val travelUiState by tripViewModel.uiState.collectAsStateWithLifecycle()
    val sharedProfileViewModel: ProfileViewModel = viewModel(
        factory = ProfileViewModel.factory(appContainer.getUserProfileUseCase),
    )
    val sharedProfileUiState by sharedProfileViewModel.uiState.collectAsStateWithLifecycle()

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
                    sharedProfileViewModel.reload()
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
        composable(
            route = Routes.PLACE_SEARCH,
            arguments = listOf(navArgument("tripId") { type = NavType.StringType }),
        ) { backStackEntry ->
            val tripId = requireNotNull(backStackEntry.arguments?.getString("tripId"))
            val placeUiState by placeViewModel.uiState.collectAsStateWithLifecycle()
            PlaceSearchScreen(
                uiState = placeUiState,
                onBack = { navController.popBackStack() },
                onQueryChange = placeViewModel::updateQuery,
                onCategorySelected = placeViewModel::selectCategory,
                onPlaceClick = { id -> navController.navigate(Routes.placeDetail(tripId, id)) },
                onRetry = placeViewModel::retry,
                favoritePlaceIds = travelUiState.travelState.favoritePlaceIds,
                onToggleFavorite = tripViewModel::toggleFavorite,
                onNearby = { navController.navigate(Routes.nearbyPlaces(tripId)) },
                onFavorites = { navController.navigate(Routes.favoritePlaces(tripId)) },
            )
        }
        composable(
            route = Routes.PLACE_DETAIL,
            arguments = listOf(
                navArgument("tripId") { type = NavType.StringType },
                navArgument("placeId") { type = NavType.StringType },
            ),
        ) { backStackEntry ->
            val tripId = requireNotNull(backStackEntry.arguments?.getString("tripId"))
            val placeId = requireNotNull(backStackEntry.arguments?.getString("placeId"))
            PlaceDetailScreen(
                place = placeViewModel.findPlace(placeId),
                isScheduled = tripViewModel.schedulesForTrip(tripId).any { it.placeId == placeId },
                onBack = { navController.popBackStack() },
                onAddToSchedule = {
                    placeViewModel.findPlace(placeId)?.let { place ->
                        tripViewModel.addPlaceSchedule(tripId, placeId, place.name)
                    }
                },
                isFavorite = tripViewModel.isFavorite(placeId),
                onToggleFavorite = { tripViewModel.toggleFavorite(placeId) },
                onNearby = { navController.navigate(Routes.nearbyPlaces(tripId, placeId)) },
            )
        }
        composable(Routes.MY_TRIP) {
            MyTripScreen(
                trips = trips,
                onAddTrip = { navController.navigate(Routes.TRIP_CREATE) },
                onDeleteTrip = tripViewModel::deleteTrip,
                onNavigateHome = { tripId ->
                    tripViewModel.selectTrip(tripId)
                    navController.navigate(Routes.tripDetail(tripId))
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
            route = Routes.TRIP_DETAIL,
            arguments = listOf(navArgument("tripId") { type = NavType.StringType }),
        ) { backStackEntry ->
            val tripId = requireNotNull(backStackEntry.arguments?.getString("tripId"))
            TripDetailScreen(
                trip = tripViewModel.domainTripById(tripId),
                participants = tripViewModel.participantsForTrip(tripId),
                profile = sharedProfileUiState.profile,
                onBack = { navController.popBackStack() },
                onEdit = { navController.navigate(Routes.tripEdit(tripId)) },
                onDelete = {
                    tripViewModel.deleteTrip(tripId)
                    navController.popBackStack()
                },
                onStart = { tripViewModel.startTrip(tripId) },
                onFinish = { tripViewModel.finishTrip(tripId) },
                onDepartureModeChange = { tripViewModel.setDepartureMode(tripId, it) },
                onParticipants = { navController.navigate(Routes.tripParticipants(tripId)) },
                onInvitation = { navController.navigate(Routes.tripInvitation(tripId)) },
                onSchedule = { navController.navigate(Routes.tripSchedule(tripId)) },
                onRoutes = { navController.navigate(Routes.routeHub(tripId)) },
                onHome = {
                    tripViewModel.selectTrip(tripId)
                    navController.navigate(Routes.realtimeHome(tripId))
                },
            )
        }
        composable(
            route = Routes.TRIP_EDIT,
            arguments = listOf(navArgument("tripId") { type = NavType.StringType }),
        ) { backStackEntry ->
            val tripId = requireNotNull(backStackEntry.arguments?.getString("tripId"))
            TripCreateScreen(
                initialTrip = tripViewModel.tripById(tripId),
                onBack = { navController.popBackStack() },
                onCreate = { trip ->
                    tripViewModel.updateTrip(trip)
                    navController.popBackStack()
                },
            )
        }
        composable(
            route = Routes.TRIP_PARTICIPANTS,
            arguments = listOf(navArgument("tripId") { type = NavType.StringType }),
        ) { backStackEntry ->
            val tripId = requireNotNull(backStackEntry.arguments?.getString("tripId"))
            ParticipantsScreen(
                tripName = tripViewModel.tripById(tripId)?.name.orEmpty(),
                profile = sharedProfileUiState.profile,
                participants = tripViewModel.participantsForTrip(tripId),
                candidates = tripViewModel.availableParticipants,
                onBack = { navController.popBackStack() },
                onAdd = { tripViewModel.addParticipant(tripId, it) },
                onRemove = { tripViewModel.removeParticipant(tripId, it) },
            )
        }
        composable(
            route = Routes.TRIP_INVITATION,
            arguments = listOf(navArgument("tripId") { type = NavType.StringType }),
        ) { backStackEntry ->
            val tripId = requireNotNull(backStackEntry.arguments?.getString("tripId"))
            val clipboard = LocalClipboardManager.current
            InvitationScreen(
                tripName = tripViewModel.tripById(tripId)?.name.orEmpty(),
                invitation = tripViewModel.invitationForTrip(tripId),
                candidates = tripViewModel.availableParticipants,
                message = travelUiState.message,
                onBack = { navController.popBackStack() },
                onCreate = { tripViewModel.createInvitation(tripId, it) },
                onCopyCode = { clipboard.setText(AnnotatedString(it)) },
                onJoinCode = tripViewModel::joinByCode,
                onAccept = tripViewModel::acceptInvitation,
                onDecline = tripViewModel::declineInvitation,
                onCancel = tripViewModel::cancelInvitation,
            )
        }
        composable(
            route = Routes.TRIP_SCHEDULE,
            arguments = listOf(navArgument("tripId") { type = NavType.StringType }),
        ) { backStackEntry ->
            val tripId = requireNotNull(backStackEntry.arguments?.getString("tripId"))
            val trip = tripViewModel.domainTripById(tripId)
            ScheduleScreen(
                tripId = tripId,
                tripName = trip?.name.orEmpty(),
                defaultDate = trip?.startDate.orEmpty(),
                schedules = tripViewModel.schedulesForTrip(tripId),
                onBack = { navController.popBackStack() },
                onSave = tripViewModel::upsertSchedule,
                onDelete = tripViewModel::deleteSchedule,
                onMove = tripViewModel::moveSchedule,
                onToggleVisited = tripViewModel::toggleVisited,
                onRecommendRoute = {
                    navController.navigate(Routes.routeRecommendation(tripId, RouteRecommendationType.ITINERARY.name))
                },
            )
        }
        composable(
            route = Routes.ROUTE_HUB,
            arguments = listOf(navArgument("tripId") { type = NavType.StringType }),
        ) { backStackEntry ->
            val tripId = requireNotNull(backStackEntry.arguments?.getString("tripId"))
            RouteHubScreen(
                tripName = tripViewModel.tripById(tripId)?.name.orEmpty(),
                onBack = { navController.popBackStack() },
                onSelect = { type -> navController.navigate(Routes.routeRecommendation(tripId, type.name)) },
            )
        }
        composable(
            route = Routes.ROUTE_RECOMMENDATION,
            arguments = listOf(
                navArgument("tripId") { type = NavType.StringType },
                navArgument("routeType") { type = NavType.StringType },
            ),
        ) { backStackEntry ->
            val tripId = requireNotNull(backStackEntry.arguments?.getString("tripId"))
            val type = backStackEntry.arguments?.getString("routeType")
                ?.let { runCatching { RouteRecommendationType.valueOf(it) }.getOrNull() }
                ?: RouteRecommendationType.ITINERARY
            RouteRecommendationScreen(
                type = type,
                trip = tripViewModel.domainTripById(tripId),
                schedules = tripViewModel.schedulesForTrip(tripId),
                profile = sharedProfileUiState.profile,
                onBack = { navController.popBackStack() },
            )
        }
        composable(
            route = Routes.NEARBY_PLACES,
            arguments = listOf(
                navArgument("tripId") { type = NavType.StringType },
                navArgument("placeId") { type = NavType.StringType },
            ),
        ) { backStackEntry ->
            val tripId = requireNotNull(backStackEntry.arguments?.getString("tripId"))
            val placeId = backStackEntry.arguments?.getString("placeId").takeUnless { it == "origin" }
            NearbyPlacesScreen(
                places = placeViewModel.nearbyPlaces(placeId),
                favoriteIds = travelUiState.travelState.favoritePlaceIds,
                onBack = { navController.popBackStack() },
                onPlaceClick = { navController.navigate(Routes.placeDetail(tripId, it)) },
                onToggleFavorite = tripViewModel::toggleFavorite,
            )
        }
        composable(
            route = Routes.FAVORITE_PLACES,
            arguments = listOf(navArgument("tripId") { type = NavType.StringType }),
        ) { backStackEntry ->
            val tripId = requireNotNull(backStackEntry.arguments?.getString("tripId"))
            FavoritePlacesScreen(
                places = travelUiState.travelState.favoritePlaceIds.mapNotNull(placeViewModel::findPlace),
                onBack = { navController.popBackStack() },
                onPlaceClick = { navController.navigate(Routes.placeDetail(tripId, it)) },
                onToggleFavorite = tripViewModel::toggleFavorite,
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
            val nextScheduleName = tripViewModel.schedulesForTrip(tripId)
                .firstOrNull { !it.isVisited }?.title
            RealtimeHomeScreen(
                uiState = homeUiState,
                tripTitle = trip?.name ?: "선택한 여행",
                tripSubtitle = trip?.let { "${it.startDate} - ${it.endDate} · ${it.cities.joinToString(" · ")}" }.orEmpty(),
                nextScheduleName = nextScheduleName,
                onNavigateMyTrip = { navController.navigate(Routes.MY_TRIP) },
                onNavigateMyPage = { navController.navigate(Routes.MY_PAGE) },
                onNavigatePlaceSearch = { navController.navigate(Routes.placeSearch(tripId)) },
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
                onDeleteAccount = {
                    appScope.launch(Dispatchers.IO) {
                        appContainer.clearUserProfileUseCase().onSuccess {
                            withContext(Dispatchers.Main) { returnToLogin() }
                        }
                    }
                },
            )
        }
    }
}

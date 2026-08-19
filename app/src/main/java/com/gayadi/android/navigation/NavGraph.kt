package com.gayadi.android.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.gayadi.android.notification.ExpenseReminderScheduler
import com.gayadi.android.notification.syncExpenseRemindersWithRetry
import com.gayadi.android.di.AppContainer
import com.gayadi.android.domain.model.ExpenseSettlementSummary
import com.gayadi.android.domain.model.TravelParticipant
import com.gayadi.android.domain.model.TravelState
import com.gayadi.android.domain.model.UserProfile
import com.gayadi.android.domain.model.LegalDocumentType
import com.gayadi.android.feature.basicinfo.presentation.BasicInfoRoute
import com.gayadi.android.feature.basicinfo.presentation.BasicInfoViewModel
import com.gayadi.android.feature.survey.presentation.SurveyRoute
import com.gayadi.android.feature.survey.presentation.SurveyViewModel
import com.gayadi.android.feature.surveyresult.presentation.SurveyResultRoute
import com.gayadi.android.feature.surveyresult.presentation.SurveyResultViewModel
import com.gayadi.android.ui.components.GayadiLoadingScreen
import com.gayadi.android.ui.screens.FriendAddScreen
import com.gayadi.android.ui.screens.FriendAddViewModel
import com.gayadi.android.ui.screens.LoginScreen
import com.gayadi.android.ui.screens.MyPageScreen
import com.gayadi.android.ui.screens.MyTravelProfileScreen
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
import com.gayadi.android.ui.screens.LegalDocumentRoute
import com.gayadi.android.ui.screens.LegalDocumentViewModel
import com.gayadi.android.ui.screens.TravelProfileResultViewModel
import com.gayadi.android.ui.screens.ParticipantsScreen
import com.gayadi.android.ui.screens.GroupDateCoordinationScreen
import com.gayadi.android.ui.screens.RouteHubScreen
import com.gayadi.android.ui.screens.RouteRecommendationScreen
import com.gayadi.android.ui.screens.RouteRecommendationType
import com.gayadi.android.ui.screens.NearbyPlacesScreen
import com.gayadi.android.ui.screens.FavoritePlacesScreen
import com.gayadi.android.ui.screens.ExpenseEditorScreen
import com.gayadi.android.ui.screens.TravelLedgerScreen
import com.gayadi.android.ui.screens.SettlementDetailsScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@Composable
fun GayadiNavHost(appContainer: AppContainer) {
    val navController = rememberNavController()
    val appScope = rememberCoroutineScope()
    val context = LocalContext.current
    val reminderScheduler = remember(context) { ExpenseReminderScheduler(context) }
    val tripViewModel: TripViewModel = viewModel(
        factory = TripViewModel.factory(
            appContainer.getTravelStateUseCase,
            appContainer.saveTravelStateUseCase,
            appContainer.updateTravelStateUseCase,
            appContainer.publishTripInviteUseCase,
        ),
    )
    val placeViewModel: PlaceViewModel = viewModel(
        factory = PlaceViewModel.factory(appContainer.getTourPlacesUseCase),
    )
    val trips by tripViewModel.trips.collectAsStateWithLifecycle()
    val selectedTripId by tripViewModel.selectedTripId.collectAsStateWithLifecycle()
    val travelUiState by tripViewModel.uiState.collectAsStateWithLifecycle()
    val sharedProfileViewModel: ProfileViewModel = viewModel(
        factory = ProfileViewModel.factory(appContainer.getUserProfileUseCase),
    )
    val sharedProfileUiState by sharedProfileViewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(
        travelUiState.hasLoadedTravelState,
        sharedProfileUiState.isLoading,
        sharedProfileUiState.profile?.nickname,
        sharedProfileUiState.profile?.characterKey,
    ) {
        if (travelUiState.hasLoadedTravelState && !sharedProfileUiState.isLoading) {
            tripViewModel.syncCurrentUser(
                nickname = sharedProfileUiState.profile?.nickname,
                characterKey = sharedProfileUiState.profile?.characterKey,
            )
        }
    }
    LaunchedEffect(travelUiState.hasLoadedTravelState, travelUiState.travelState.schedules) {
        if (travelUiState.hasLoadedTravelState) {
            syncExpenseRemindersWithRetry(
                sync = { reminderScheduler.sync(travelUiState.travelState.schedules) },
            )
        }
    }

    NavHost(navController = navController, startDestination = Routes.STARTUP) {
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
        composable(
            route = Routes.FRIEND_ADD_WITH_CODE,
            arguments = listOf(
                navArgument("inviteCode") {
                    type = NavType.StringType
                    defaultValue = ""
                },
            ),
            deepLinks = listOf(navDeepLink { uriPattern = "gayadi://invite/{inviteCode}" }),
        ) { backStackEntry ->
            val deepLinkedInviteCode = backStackEntry.arguments?.getString("inviteCode").orEmpty()
            val friendViewModel: FriendAddViewModel = viewModel(
                factory = FriendAddViewModel.factory(
                    joinTripByInviteCode = appContainer.joinTripByInviteCodeUseCase,
                    localParticipant = TravelParticipant(
                        id = "local-user",
                        nickname = sharedProfileUiState.profile?.nickname ?: "나",
                        characterKey = sharedProfileUiState.profile?.characterKey,
                    ),
                ),
            )
            val friendUiState by friendViewModel.uiState.collectAsStateWithLifecycle()
            LaunchedEffect(deepLinkedInviteCode) {
                if (deepLinkedInviteCode.isNotBlank()) friendViewModel.updateFriendCode(deepLinkedInviteCode)
            }
            FriendAddScreen(
                uiState = friendUiState,
                onBack = { navController.popBackStack() },
                onQueryChange = friendViewModel::updateQuery,
                onFriendCodeChange = friendViewModel::updateFriendCode,
                onAddByCode = friendViewModel::addFriendByCode,
                onAddFriend = friendViewModel::addFriend,
                onRetry = friendViewModel::retry,
            )
        }
        composable(
            route = Routes.PLACE_SEARCH,
            arguments = listOf(navArgument("tripId") { type = NavType.StringType }),
        ) { backStackEntry ->
            val tripId = requireNotNull(backStackEntry.arguments?.getString("tripId"))
            val city = travelUiState.travelState.trip(tripId)?.cities?.firstOrNull().orEmpty()
            val placeUiState by placeViewModel.uiState.collectAsStateWithLifecycle()
            LaunchedEffect(tripId, city) { placeViewModel.setRegion(city) }
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
            val travelState = travelUiState.travelState
            val trip = travelState.trip(tripId)
            PlaceDetailScreen(
                place = placeViewModel.findPlace(placeId),
                tripName = trip?.name.orEmpty(),
                tripDate = trip?.startDate.orEmpty(),
                isScheduled = travelState.schedulesForTrip(tripId).any { it.placeId == placeId },
                onBack = { navController.popBackStack() },
                onAddToSchedule = { time, memo ->
                    placeViewModel.findPlace(placeId)?.let { place ->
                        tripViewModel.addPlaceSchedule(tripId, placeId, place.name, time, memo)
                    }
                },
                isFavorite = placeId in travelState.favoritePlaceIds,
                onToggleFavorite = { tripViewModel.toggleFavorite(placeId) },
                onNearby = { navController.navigate(Routes.nearbyPlaces(tripId, placeId)) },
            )
        }
        composable(Routes.MY_TRIP) {
            MyTripScreen(
                trips = trips,
                onAddTrip = { navController.navigate(Routes.TRIP_CREATE) },
                onDeleteTrip = tripViewModel::deleteTrip,
                onOpenTripDetail = { tripId ->
                    val trip = travelUiState.travelState.trip(tripId)
                    if (trip?.isGroupTrip == true && trip.startDate.isBlank()) {
                        navController.navigate(Routes.groupDateCoordination(tripId))
                    } else {
                        tripViewModel.selectTrip(tripId)
                        navController.navigate(Routes.realtimeHome(tripId))
                    }
                },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
            )
        }
        composable(Routes.TRIP_CREATE) {
            TripCreateScreen(
                onBack = { navController.popBackStack() },
                onCreate = tripViewModel::addTrip,
                onPublishInvite = tripViewModel::publishInvite,
                onStartTrip = { trip ->
                    tripViewModel.selectTrip(trip.id)
                    navController.navigate(Routes.realtimeHome(trip.id)) {
                        popUpTo(Routes.TRIP_CREATE) { inclusive = true }
                    }
                },
                onCoordinateDates = { trip ->
                    navController.navigate(Routes.groupDateCoordination(trip.id))
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
                    Result.success(trip)
                },
            )
        }
        composable(
            route = Routes.TRIP_INVITE_CODE,
            arguments = listOf(navArgument("tripId") { type = NavType.StringType }),
        ) { backStackEntry ->
            val tripId = requireNotNull(backStackEntry.arguments?.getString("tripId"))
            val travelState = travelUiState.travelState
            ParticipantsScreen(
                tripName = travelState.trip(tripId)?.name.orEmpty(),
                inviteCode = travelState.trip(tripId)?.inviteCode.orEmpty(),
                cities = travelState.trip(tripId)?.cities.orEmpty(),
                profile = sharedProfileUiState.profile,
                participants = travelState.participantsForTrip(tripId, tripViewModel.availableParticipants),
                candidates = tripViewModel.availableParticipants,
                onBack = { navController.popBackStack() },
                onAdd = { tripViewModel.addParticipant(tripId, it) },
                onRemove = { tripViewModel.removeParticipant(tripId, it) },
                onPublishInvite = { tripViewModel.publishInvite(tripId) },
            )
        }
        composable(
            route = Routes.GROUP_DATE_COORDINATION,
            arguments = listOf(navArgument("tripId") { type = NavType.StringType }),
        ) { backStackEntry ->
            val tripId = requireNotNull(backStackEntry.arguments?.getString("tripId"))
            val travelState = travelUiState.travelState
            GroupDateCoordinationScreen(
                trip = travelState.trip(tripId),
                currentUserId = travelState.currentUserId,
                participants = travelState.participantsForTrip(tripId, tripViewModel.availableParticipants),
                onBack = { navController.popBackStack() },
                onSubmit = { participantId, dates ->
                    tripViewModel.submitDateAvailability(tripId, participantId, dates)
                },
                onFinalize = { startDate, endDate ->
                    tripViewModel.finalizeGroupTripDates(tripId, startDate, endDate)
                    tripViewModel.selectTrip(tripId)
                    navController.navigate(Routes.realtimeHome(tripId)) {
                        popUpTo(Routes.MY_TRIP)
                    }
                },
            )
        }
        composable(
            route = Routes.TRIP_LEDGER,
            arguments = listOf(navArgument("tripId") { type = NavType.StringType }),
        ) { backStackEntry ->
            val tripId = requireNotNull(backStackEntry.arguments?.getString("tripId"))
            val travelState = travelUiState.travelState
            val settlementResult = tripViewModel.settlementForTrip(tripId)
            val settlementErrorMessage = settlementResult.exceptionOrNull()?.let { error ->
                error.message ?: "비용 정산 정보를 계산하지 못했어요"
            }
            TravelLedgerScreen(
                tripName = travelState.trip(tripId)?.name.orEmpty(),
                expenses = tripViewModel.expensesForTrip(tripId),
                schedules = travelState.schedulesForTrip(tripId),
                participants = travelState.participantsForTrip(tripId, tripViewModel.availableParticipants),
                settlementSummary = settlementResult.getOrElse {
                    ExpenseSettlementSummary(0L, emptyList(), emptyList())
                },
                settlementErrorMessage = settlementErrorMessage,
                sharedFundBalance = tripViewModel.sharedFundBalanceForTrip(tripId),
                onBack = { navController.popBackStack() },
                onAddExpense = { scheduleId ->
                    navController.navigate(Routes.tripExpense(tripId, scheduleId))
                },
                onAddSharedFund = { amount -> tripViewModel.addSharedFund(tripId, amount) },
                onOpenSettlementDetails = { participantId, detailType ->
                    navController.navigate(Routes.settlementDetails(tripId, participantId, detailType))
                },
                onEditExpense = { expenseId, scheduleId ->
                    navController.navigate(Routes.tripExpense(tripId, scheduleId, expenseId))
                },
                onDeleteExpense = tripViewModel::deleteExpense,
            )
        }
        composable(
            route = Routes.SETTLEMENT_DETAILS,
            arguments = listOf(
                navArgument("tripId") { type = NavType.StringType },
                navArgument("participantId") { type = NavType.StringType },
                navArgument("detailType") { type = NavType.StringType },
            ),
        ) { backStackEntry ->
            val tripId = requireNotNull(backStackEntry.arguments?.getString("tripId"))
            val participantId = requireNotNull(backStackEntry.arguments?.getString("participantId"))
            val detailType = requireNotNull(backStackEntry.arguments?.getString("detailType"))
            SettlementDetailsScreen(
                participantId = participantId,
                detailType = detailType,
                expenses = tripViewModel.expensesForTrip(tripId),
                onBack = { navController.popBackStack() },
            )
        }
        composable(
            route = Routes.TRIP_EXPENSE,
            arguments = listOf(
                navArgument("tripId") { type = NavType.StringType },
                navArgument("scheduleId") { type = NavType.StringType },
                navArgument("expenseId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
            ),
            deepLinks = listOf(
                navDeepLink { uriPattern = "gayadi://expense/{tripId}/{scheduleId}" },
            ),
        ) { backStackEntry ->
            val tripId = requireNotNull(backStackEntry.arguments?.getString("tripId"))
            val scheduleId = requireNotNull(backStackEntry.arguments?.getString("scheduleId"))
            val resolvedScheduleId = scheduleId.takeUnless { it == Routes.UNLINKED_SCHEDULE_ID }.orEmpty()
            val expenseId = backStackEntry.arguments?.getString("expenseId")
            val travelState = travelUiState.travelState
            val expense = expenseId?.let { id ->
                travelState.expenses.find {
                    it.id == id && it.tripId == tripId && it.scheduleId == resolvedScheduleId
                }
            }
            LaunchedEffect(backStackEntry) { tripViewModel.clearExpenseError() }
            LaunchedEffect(travelUiState.savedExpenseId, expenseId) {
                travelUiState.savedExpenseId?.let { savedId ->
                    tripViewModel.consumeSavedExpense()
                    if (expenseId == null || savedId == expenseId) {
                        navController.popBackStack()
                    }
                }
            }
            ExpenseEditorScreen(
                tripId = tripId,
                expense = expense,
                isEditMode = expenseId != null,
                schedule = travelState.schedules.find { it.id == resolvedScheduleId && it.tripId == tripId },
                participants = travelState.participantsForTrip(tripId, tripViewModel.availableParticipants),
                initialPayerId = travelState.currentUserId,
                onBack = { navController.popBackStack() },
                onSave = tripViewModel::saveExpense,
                isSaving = travelUiState.isSavingExpense,
                errorMessage = travelUiState.expenseErrorMessage,
                hasLoadedTravelState = travelUiState.hasLoadedTravelState,
                isLoadingTravelState = travelUiState.isLoading,
            )
        }
        composable(
            route = Routes.ROUTE_HUB,
            arguments = listOf(navArgument("tripId") { type = NavType.StringType }),
        ) { backStackEntry ->
            val tripId = requireNotNull(backStackEntry.arguments?.getString("tripId"))
            RouteHubScreen(
                tripName = travelUiState.travelState.trip(tripId)?.name.orEmpty(),
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
            val travelState = travelUiState.travelState
            RouteRecommendationScreen(
                type = type,
                trip = travelState.trip(tripId),
                schedules = travelState.schedulesForTrip(tripId),
                profile = sharedProfileUiState.profile,
                appliedOptionId = travelState.appliedRouteIds["$tripId:${type.name}"],
                onBack = { navController.popBackStack() },
                onApply = { tripViewModel.applyRoute(tripId, type.name, it) },
            )
        }
        composable(
            route = Routes.NEARBY_PLACES,
            arguments = listOf(
                navArgument("tripId") { type = NavType.StringType },
                navArgument("placeId") { type = NavType.StringType; nullable = true; defaultValue = null },
            ),
        ) { backStackEntry ->
            val tripId = requireNotNull(backStackEntry.arguments?.getString("tripId"))
            val placeId = backStackEntry.arguments?.getString("placeId")
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
            val travelState = travelUiState.travelState
            val trip = travelState.trip(tripId)
            val tripSummary = trips.firstOrNull { it.id == tripId }
            val homeViewModel: RealtimeHomeViewModel = viewModel(
                factory = RealtimeHomeViewModel.factory(appContainer.getUserProfileUseCase),
            )
            val homeUiState by homeViewModel.uiState.collectAsStateWithLifecycle()
            val tripSchedules = travelState.schedulesForTrip(tripId)
            val tripParticipants = travelState.participantsForTrip(tripId, tripViewModel.availableParticipants)
            RealtimeHomeScreen(
                uiState = homeUiState,
                tripTitle = trip?.name ?: "선택한 여행",
                travelPlans = tripSchedules.map { schedule ->
                    com.gayadi.android.ui.screens.HomeTravelPlan(
                        id = schedule.id,
                        title = schedule.title,
                        date = schedule.date,
                        time = schedule.time,
                        memo = schedule.memo,
                        isVisited = schedule.isVisited,
                    )
                },
                tripDays = trip?.let { selectedTrip ->
                    runCatching {
                        val formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd")
                        val start = LocalDate.parse(selectedTrip.startDate, formatter)
                        val end = LocalDate.parse(selectedTrip.endDate, formatter)
                        generateSequence(start) { current ->
                            current.plusDays(1).takeIf { !it.isAfter(end) }
                        }.mapIndexed { index, date ->
                            val weekday = listOf("월", "화", "수", "목", "금", "토", "일")[date.dayOfWeek.value - 1]
                            com.gayadi.android.ui.screens.HomeTripDay(
                                dayNumber = index + 1,
                                date = date.format(formatter),
                                dateLabel = "${date.monthValue}.${date.dayOfMonth}/$weekday",
                            )
                        }.toList()
                    }.getOrDefault(emptyList())
                }.orEmpty(),
                participantCount = tripParticipants.size,
                tripStartDate = trip?.startDate.orEmpty(),
                tripEndDate = trip?.endDate.orEmpty(),
                tripCoverImageResList = tripSummary?.coverImageResList.orEmpty(),
                kakaoMapJavaScriptKey = com.gayadi.android.BuildConfig.KAKAO_MAP_JAVASCRIPT_SDK,
                kakaoMapBaseUrl = com.gayadi.android.BuildConfig.API_BASE_URL,
                friendCharacterKeys = tripParticipants.map { it.characterKey },
                tripCountdownText = trip?.startDate?.let { startDate ->
                    runCatching {
                        val date = LocalDate.parse(startDate, DateTimeFormatter.ofPattern("yyyy.MM.dd"))
                        val days = ChronoUnit.DAYS.between(LocalDate.now(), date)
                        when {
                            days > 0 -> "${days}일 남았어요!"
                            days == 0L -> "오늘 출발해요!"
                            else -> "여행 중이에요!"
                        }
                    }.getOrDefault("여행을 준비하고 있어요!")
                } ?: "여행을 준비하고 있어요!",
                onNavigateMyTrip = { navController.navigate(Routes.MY_TRIP) },
                onNavigateMyPage = { navController.navigate(Routes.MY_PAGE) },
                onNavigateLedger = { navController.navigate(Routes.tripLedger(tripId)) },
                onNavigatePlaceSearch = { navController.navigate(Routes.placeSearch(tripId)) },
                onNavigateParticipants = { navController.navigate(Routes.tripInviteCode(tripId)) },
                onUpdateSchedule = { scheduleId, time, memo ->
                    tripSchedules.firstOrNull { it.id == scheduleId }?.let { schedule ->
                        tripViewModel.upsertSchedule(schedule.copy(time = time, memo = memo))
                    }
                },
                onAddScheduleExpense = { scheduleId, time, memo ->
                    tripSchedules.firstOrNull { it.id == scheduleId }?.let { schedule ->
                        tripViewModel.upsertSchedule(schedule.copy(time = time, memo = memo))
                        navController.navigate(Routes.tripExpense(tripId, scheduleId))
                    }
                },
                onScheduleDirections = { scheduleId, time, memo ->
                    tripSchedules.firstOrNull { it.id == scheduleId }?.let { schedule ->
                        tripViewModel.upsertSchedule(schedule.copy(time = time, memo = memo))
                        navController.navigate(Routes.routeHub(tripId))
                    }
                },
                onNavigateRoutes = { navController.navigate(Routes.routeHub(tripId)) },
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
            val returnToLogin = {
                navController.navigate(Routes.LOGIN) {
                    popUpTo(navController.graph.id) { inclusive = true }
                }
            }
            SettingsScreen(
                uiState = sharedProfileUiState,
                onBack = { navController.popBackStack() },
                onOpenTravelProfile = { navController.navigate(Routes.MY_TRAVEL_PROFILE) },
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
}

internal fun resolveStartupDestination(profile: UserProfile?): String = when {
    profile?.nickname.isNullOrBlank() -> Routes.LOGIN
    profile?.characterKey.isNullOrBlank() -> Routes.SURVEY
    else -> Routes.MY_TRIP
}

private fun TravelState.trip(tripId: String) = trips.find { it.id == tripId }

private fun TravelState.schedulesForTrip(tripId: String) =
    schedules.filter { it.tripId == tripId }.sortedBy { it.order }

private fun TravelState.participantsForTrip(
    tripId: String,
    candidates: List<TravelParticipant>,
): List<TravelParticipant> {
    val participantIds = trip(tripId)?.participantIds.orEmpty()
    return (participants + candidates)
        .distinctBy(TravelParticipant::id)
        .filter { it.id in participantIds }
}

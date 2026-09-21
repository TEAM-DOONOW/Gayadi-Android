package com.gayadi.android.navigation

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.gayadi.android.domain.model.ExpenseSettlementSummary
import com.gayadi.android.domain.error.isCoroutineCancellation
import com.gayadi.android.domain.error.userFacingMessage
import com.gayadi.android.domain.model.TravelParticipant
import com.gayadi.android.domain.model.TravelSchedule
import com.gayadi.android.ui.screens.ExpenseEditorScreen
import com.gayadi.android.ui.screens.FavoritePlacesScreen
import com.gayadi.android.ui.screens.FriendAddScreen
import com.gayadi.android.ui.screens.FriendAddViewModel
import com.gayadi.android.ui.screens.GroupDateCoordinationScreen
import com.gayadi.android.ui.screens.MyTripScreen
import com.gayadi.android.ui.screens.NearbyPlacesScreen
import com.gayadi.android.ui.screens.ParticipantsScreen
import com.gayadi.android.ui.screens.PlaceDetailScreen
import com.gayadi.android.ui.screens.PlaceSearchScreen
import com.gayadi.android.ui.screens.PlaceRecommendationViewModel
import com.gayadi.android.ui.screens.RealtimeHomeScreen
import com.gayadi.android.ui.screens.RealtimeHomeViewModel
import com.gayadi.android.ui.screens.RouteHubScreen
import com.gayadi.android.ui.screens.RouteRecommendationScreen
import com.gayadi.android.ui.screens.RouteRecommendationType
import com.gayadi.android.ui.screens.SettlementDetailsScreen
import com.gayadi.android.ui.screens.TravelLedgerScreen
import com.gayadi.android.ui.screens.TripCreateScreen
import com.gayadi.android.ui.screens.AgentScreen
import com.gayadi.android.ui.screens.AgentViewModel

internal fun NavGraphBuilder.tripGraph(context: AppNavigationContext) = with(context) {
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
        LaunchedEffect(friendUiState.joinedTripId) {
            if (friendUiState.joinedTripId != null) tripViewModel.retry()
        }
        val joinedTripId = friendUiState.joinedTripId
        val joinedTrip = joinedTripId?.let(travelUiState.travelState::trip)
        LaunchedEffect(joinedTripId, joinedTrip) {
            if (joinedTripId != null && joinedTrip != null) {
                if (joinedTrip.isGroupTrip && joinedTrip.startDate.isBlank()) {
                    navController.navigate(Routes.groupDateCoordination(joinedTripId)) {
                        popUpTo(backStackEntry.destination.id) { inclusive = true }
                    }
                } else {
                    tripViewModel.selectTrip(joinedTripId)
                    navController.navigate(Routes.realtimeHome(joinedTripId)) {
                        popUpTo(backStackEntry.destination.id) { inclusive = true }
                    }
                }
            }
        }
        val friendships: com.gayadi.android.ui.screens.FriendshipViewModel = viewModel(
            factory = com.gayadi.android.ui.screens.FriendshipViewModel.factory(appContainer.friendshipGateway),
        )
        val friendshipState by friendships.state.collectAsStateWithLifecycle()
        FriendAddScreen(
            uiState = friendUiState,
            onBack = { navController.popBackStack() },
            onQueryChange = friendViewModel::updateQuery,
            onFriendCodeChange = friendViewModel::updateFriendCode,
            onAddByCode = friendViewModel::addFriendByCode,
            onAddFriend = friendViewModel::addFriend,
            onRetry = friendViewModel::retry,
            friendshipContent = {
                com.gayadi.android.ui.screens.FriendshipPanel(friendshipState, friendships::search,
                    friendships::request, friendships::decide, friendships::delete, friendships::reload)
            },
        )
    }
    composable(
        route = Routes.PLACE_SEARCH,
        arguments = listOf(
            navArgument("tripId") { type = NavType.StringType },
            navArgument("date") {
                type = NavType.StringType
                defaultValue = ""
            },
        ),
    ) { backStackEntry ->
        val tripId = requireNotNull(backStackEntry.arguments?.getString("tripId"))
        val trip = travelUiState.travelState.trip(tripId)
        val selectedDate = backStackEntry.arguments?.getString("date")
            ?.takeIf(String::isNotBlank)
            ?: trip?.startDate.orEmpty()
        val scheduledPlaces = travelUiState.travelState.schedulesForTrip(tripId)
        val city = trip?.cities?.firstOrNull().orEmpty()
        val placeUiState by placeViewModel.uiState.collectAsStateWithLifecycle()
        val recommendationViewModel: PlaceRecommendationViewModel = viewModel(
            key = "place-recommendations-$tripId",
            factory = PlaceRecommendationViewModel.factory(appContainer.agentGateway),
        )
        val recommendationUiState by recommendationViewModel.uiState.collectAsStateWithLifecycle()
        val destination = city.ifBlank { "제주 성산" }
        val recommendationRegionReady =
            placeUiState.regionName == destination && !placeUiState.isLoading
        val recommendationOrigin = placeUiState.places
            .takeIf { recommendationRegionReady }
            ?.firstOrNull { it.latitude != null && it.longitude != null }
        val recommendationContextReady = recommendationRegionReady && recommendationOrigin != null
        val recommendationProfile = sharedProfileUiState.profile?.let { profile ->
            buildList {
                profile.travelStyleName?.takeIf(String::isNotBlank)?.let(::add)
                addAll(profile.strengths)
                profile.introduction.takeIf(String::isNotBlank)?.let(::add)
            }.joinToString(", ")
        }.orEmpty().ifBlank { "새로운 장소를 발견하고 여유롭게 여행하는 것을 좋아해요." }
        LaunchedEffect(tripId, city) { placeViewModel.setRegion(city) }
        LaunchedEffect(
            tripId,
            recommendationContextReady,
            recommendationOrigin?.latitude,
            recommendationOrigin?.longitude,
            recommendationProfile,
        ) {
            if (!recommendationContextReady) return@LaunchedEffect
            recommendationViewModel.recommend(
                destination = destination,
                profile = recommendationProfile,
                latitude = recommendationOrigin?.latitude,
                longitude = recommendationOrigin?.longitude,
                keywords = emptyList(),
                groupSize = trip?.participantIds?.size?.coerceAtLeast(1) ?: 1,
            )
        }
        LaunchedEffect(recommendationUiState.recommendations) {
            placeViewModel.applyAgentRecommendations(recommendationUiState.recommendations)
        }
        val androidContext = LocalContext.current
        PlaceSearchScreen(
            showUsageGuide = remember(androidContext) {
                !UsageGuidePreferences.hasCompleted(androidContext, UsageGuidePreferences.PlaceSearch)
            },
            onUsageGuideFinished = {
                UsageGuidePreferences.markCompleted(androidContext, UsageGuidePreferences.PlaceSearch)
            },
            uiState = placeUiState,
            recommendationUiState = recommendationUiState,
            onBack = { navController.popBackStack() },
            onQueryChange = placeViewModel::updateQuery,
            onCategorySelected = placeViewModel::selectCategory,
            onPlaceClick = { id -> navController.navigate(Routes.placeDetail(tripId, id, selectedDate)) },
            onRetry = placeViewModel::retry,
            favoritePlaceIds = travelUiState.travelState.favoritePlaceIds,
            onToggleFavorite = { id ->
                tripViewModel.toggleFavorite(id, placeViewModel.findPlace(id)?.name)
            },
            onNearby = { navController.navigate(Routes.nearbyPlaces(tripId)) },
            onFavorites = { navController.navigate(Routes.favoritePlaces(tripId)) },
            onRequestRecommendations = {
                recommendationViewModel.recommend(
                    destination = destination,
                    profile = recommendationProfile,
                    latitude = recommendationOrigin?.latitude,
                    longitude = recommendationOrigin?.longitude,
                    keywords = placeUiState.query.trim().takeIf(String::isNotBlank)?.let(::listOf).orEmpty(),
                    groupSize = trip?.participantIds?.size?.coerceAtLeast(1) ?: 1,
                    force = true,
                )
            },
            onRecommendationClick = { recommendation ->
                placeViewModel.applyAgentRecommendations(listOf(recommendation))
                recommendation.placeId.toLongOrNull()
                    ?.takeIf { it > 0 }
                    ?.let {
                        navController.navigate(Routes.placeDetail(tripId, recommendation.placeId, selectedDate))
                    }
            },
            tripName = trip?.name.orEmpty(),
            tripDate = selectedDate,
            scheduledPlaceIds = scheduledPlaces.mapNotNull(TravelSchedule::placeId).toSet(),
            scheduledPlaceNames = scheduledPlaces.map(TravelSchedule::title).toSet(),
            onAddToSchedule = { placeId, time, memo ->
                placeViewModel.findPlace(placeId)?.let { place ->
                    tripViewModel.addPlaceSchedule(tripId, placeId, place.name, selectedDate, time, memo)
                }
            },
        )
    }
    composable(
        route = Routes.PLACE_DETAIL,
        arguments = listOf(
            navArgument("tripId") { type = NavType.StringType },
            navArgument("placeId") { type = NavType.StringType },
            navArgument("date") {
                type = NavType.StringType
                defaultValue = ""
            },
        ),
    ) { backStackEntry ->
        val tripId = requireNotNull(backStackEntry.arguments?.getString("tripId"))
        val placeId = requireNotNull(backStackEntry.arguments?.getString("placeId"))
        val travelState = travelUiState.travelState
        val trip = travelState.trip(tripId)
        val selectedDate = backStackEntry.arguments?.getString("date")
            ?.takeIf(String::isNotBlank)
            ?: trip?.startDate.orEmpty()
        val androidContext = LocalContext.current
        val placeUiState by placeViewModel.uiState.collectAsStateWithLifecycle()
        val place = placeUiState.places.firstOrNull { it.id == placeId }
            ?: placeViewModel.findPlace(placeId)
        val hourlyUiState by placeViewModel.hourlyUiState.collectAsStateWithLifecycle()
        LaunchedEffect(placeId, place?.regionCode, place?.districtCode) {
            placeViewModel.loadCongestionHourly(placeId)
        }
        PlaceDetailScreen(
            showUsageGuide = remember(androidContext) {
                !UsageGuidePreferences.hasCompleted(androidContext, UsageGuidePreferences.PlaceDetail)
            },
            onUsageGuideFinished = {
                UsageGuidePreferences.markCompleted(androidContext, UsageGuidePreferences.PlaceDetail)
            },
            place = place,
            tripName = trip?.name.orEmpty(),
            tripDate = selectedDate,
            isScheduled = travelState.schedulesForTrip(tripId).any { it.placeId == placeId },
            onBack = { navController.popBackStack() },
            onAddToSchedule = { time, memo ->
                placeViewModel.findPlace(placeId)?.let { place ->
                    tripViewModel.addPlaceSchedule(tripId, placeId, place.name, selectedDate, time, memo)
                }
            },
            isFavorite = placeId in travelState.favoritePlaceIds,
            onToggleFavorite = {
                tripViewModel.toggleFavorite(placeId, placeViewModel.findPlace(placeId)?.name)
            },
            onNearby = { navController.navigate(Routes.nearbyPlaces(tripId, placeId)) },
            hourlyUiState = hourlyUiState,
            onHourlyRetry = { placeViewModel.loadCongestionHourly(placeId) },
        )
    }
    composable(Routes.MY_TRIP) {
        LaunchedEffect(Unit) { tripViewModel.retry() }
        val androidContext = LocalContext.current
        val showFirstGuide = remember(androidContext) {
            !UsageGuidePreferences.hasCompleted(androidContext, UsageGuidePreferences.MyTrip)
        }
        MyTripScreen(
            trips = trips,
            showUsageGuide = showFirstGuide,
            onUsageGuideFinished = {
                UsageGuidePreferences.markCompleted(androidContext, UsageGuidePreferences.MyTrip)
            },
            onAddTrip = { navController.navigate(Routes.TRIP_CREATE) },
            onJoinTrip = { navController.navigate(Routes.FRIEND_ADD) },
            onJoinTripWithCode = { inviteCode ->
                navController.navigate("friend_add?inviteCode=$inviteCode")
            },
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
            onOpenAgent = { navController.navigate(Routes.AGENT) },
        )
    }
    composable(Routes.AGENT) {
        val activeTrip = selectedTripId
            ?.let(travelUiState.travelState::trip)
            ?: travelUiState.travelState.trips.firstOrNull { it.status != com.gayadi.android.domain.model.TripStatus.COMPLETED }
        val agentViewModel: AgentViewModel = viewModel(
            key = "agent-${activeTrip?.id ?: "empty"}",
            factory = AgentViewModel.factory(activeTrip?.id, appContainer.agentGateway),
        )
        val agentUiState by agentViewModel.uiState.collectAsStateWithLifecycle()
        val placeUiState by placeViewModel.uiState.collectAsStateWithLifecycle()
        val city = activeTrip?.cities?.firstOrNull().orEmpty()
        LaunchedEffect(activeTrip?.id, city) {
            if (city.isNotBlank()) placeViewModel.setRegion(city)
        }
        AgentScreen(
            tripName = activeTrip?.name,
            uiState = agentUiState,
            onBack = { navController.popBackStack() },
            onAnalyze = {
                val origin = placeUiState.places.firstOrNull {
                    it.latitude != null && it.longitude != null
                }
                agentViewModel.analyze(
                    latitude = origin?.latitude,
                    longitude = origin?.longitude,
                    regionCode = origin?.regionCode.orEmpty(),
                    districtCode = origin?.districtCode.orEmpty(),
                )
            },
            onRetry = agentViewModel::refresh,
            onSelectOption = agentViewModel::selectOption,
            onApprove = { agentViewModel.decide(it, approve = true) },
            onReject = { agentViewModel.decide(it, approve = false) },
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
                tripViewModel.updateTrip(trip).onSuccess { navController.popBackStack() }
            },
        )
    }
    composable(
        route = Routes.TRIP_INVITE_CODE,
        arguments = listOf(navArgument("tripId") { type = NavType.StringType }),
    ) { backStackEntry ->
        val tripId = requireNotNull(backStackEntry.arguments?.getString("tripId"))
        LaunchedEffect(tripId) { tripViewModel.retry() }
        val travelState = travelUiState.travelState
        ParticipantsScreen(
            tripName = travelState.trip(tripId)?.name.orEmpty(),
            inviteCode = travelState.trip(tripId)?.inviteCode.orEmpty(),
            cities = travelState.trip(tripId)?.cities.orEmpty(),
            currentUserId = travelState.currentUserId,
            ownerId = travelState.trip(tripId)?.ownerId.orEmpty(),
            participants = travelState.participantsForTrip(tripId, tripViewModel.availableParticipants),
            onBack = { navController.popBackStack() },
            onRemove = { tripViewModel.removeParticipant(tripId, it) },
            onPublishInvite = { tripViewModel.publishInvite(tripId) },
            onCoordinateDates = { navController.navigate(Routes.groupDateCoordination(tripId)) },
            invitationContent = {
                val invites: com.gayadi.android.ui.screens.InvitationViewModel = viewModel(
                    factory = com.gayadi.android.ui.screens.InvitationViewModel.factory(appContainer.travelGateway, appContainer.friendshipGateway, tripId))
                val inviteState by invites.state.collectAsStateWithLifecycle()
                com.gayadi.android.ui.screens.InvitationPanel(inviteState, invites::search, invites::invite, invites::cancel, invites::reload)
            },
        )
    }
    composable(
        route = Routes.GROUP_DATE_COORDINATION,
        arguments = listOf(navArgument("tripId") { type = NavType.StringType }),
    ) { backStackEntry ->
        val tripId = requireNotNull(backStackEntry.arguments?.getString("tripId"))
        LaunchedEffect(tripId) {
            while (true) { tripViewModel.retry(); kotlinx.coroutines.delay(15000) }
        }
        val androidContext = LocalContext.current
        val travelState = travelUiState.travelState
        val coordinatedTrip = travelState.trip(tripId)
        val canFinalize = coordinatedTrip?.ownerId.isNullOrBlank() ||
            coordinatedTrip?.ownerId == travelState.currentUserId
        LaunchedEffect(coordinatedTrip?.startDate, coordinatedTrip?.endDate, canFinalize) {
            if (!canFinalize && !coordinatedTrip?.startDate.isNullOrBlank() && !coordinatedTrip?.endDate.isNullOrBlank()) {
                tripViewModel.selectTrip(tripId)
                navController.navigate(Routes.realtimeHome(tripId)) { popUpTo(Routes.MY_TRIP) }
            }
        }
        GroupDateCoordinationScreen(
            trip = coordinatedTrip,
            currentUserId = travelState.currentUserId,
            canFinalize = canFinalize,
            showUsageGuide = remember(androidContext) {
                !UsageGuidePreferences.hasCompleted(androidContext, UsageGuidePreferences.GroupDate)
            },
            onUsageGuideFinished = {
                UsageGuidePreferences.markCompleted(androidContext, UsageGuidePreferences.GroupDate)
            },
            participants = travelState.participantsForTrip(tripId, tripViewModel.availableParticipants),
            candidates = tripViewModel.availableParticipants,
            onBack = { navController.popBackStack() },
            onAddParticipant = { participantId ->
                tripViewModel.addParticipant(tripId, participantId)
            },
            onSubmit = { participantId, dates ->
                tripViewModel.submitDateAvailability(tripId, participantId, dates)
            },
            onFinalize = { startDate, endDate ->
                tripViewModel.finalizeGroupTripDates(tripId, startDate, endDate) {
                    tripViewModel.selectTrip(tripId)
                    navController.navigate(Routes.realtimeHome(tripId)) { popUpTo(Routes.MY_TRIP) }
                }
            },
        )
    }
    composable(
        route = Routes.TRIP_LEDGER,
        arguments = listOf(navArgument("tripId") { type = NavType.StringType }),
    ) { backStackEntry ->
        val tripId = requireNotNull(backStackEntry.arguments?.getString("tripId"))
        LaunchedEffect(tripId) { tripViewModel.retry() }
        val travelState = travelUiState.travelState
        LaunchedEffect(
            tripId,
            travelUiState.hasLoadedTravelState,
            travelState.expenses,
            travelState.sharedFundAmounts,
        ) {
            if (travelUiState.hasLoadedTravelState) {
                tripViewModel.refreshSettlement(tripId)
            }
        }
        val settlementResult = tripViewModel.settlementForTrip(tripId)
        val settlementErrorMessage = settlementResult.exceptionOrNull()
            ?.takeUnless { it.isCoroutineCancellation() }
            ?.let { error ->
            error.userFacingMessage("비용 정산 정보를 계산하지 못했어요")
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
            tripStartDate = travelState.trip(tripId)?.startDate,
            tripEndDate = travelState.trip(tripId)?.endDate,
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
        val nearbyUiState by placeViewModel.nearbyUiState.collectAsStateWithLifecycle()
        LaunchedEffect(placeId) { placeViewModel.loadNearbyPlaces(placeId) }
        NearbyPlacesScreen(
            places = nearbyUiState.places,
            favoriteIds = travelUiState.travelState.favoritePlaceIds,
            onBack = { navController.popBackStack() },
            onPlaceClick = { navController.navigate(Routes.placeDetail(tripId, it)) },
            onToggleFavorite = { id ->
                tripViewModel.toggleFavorite(id, placeViewModel.findPlace(id)?.name)
            },
            isLoading = nearbyUiState.isLoading,
            errorMessage = nearbyUiState.errorMessage,
            onRetry = { placeViewModel.loadNearbyPlaces(placeId) },
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
            onToggleFavorite = { id ->
                tripViewModel.toggleFavorite(id, placeViewModel.findPlace(id)?.name)
            },
        )
    }
    composable(
        route = Routes.REALTIME_HOME,
        arguments = listOf(navArgument("tripId") { type = NavType.StringType }),
    ) { backStackEntry ->
        val tripId = requireNotNull(backStackEntry.arguments?.getString("tripId"))
        LaunchedEffect(tripId) { tripViewModel.retry() }
        val androidContext = LocalContext.current
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
            travelPlans = tripSchedules.map { it.toHomeTravelPlan() },
            tripDays = trip?.let { buildHomeTripDays(it.startDate, it.endDate) }.orEmpty(),
            participantCount = tripParticipants.size,
            tripStartDate = trip?.startDate.orEmpty(),
            tripEndDate = trip?.endDate.orEmpty(),
            tripCoverImageResList = tripSummary?.coverImageResList.orEmpty(),
            kakaoMapJavaScriptKey = com.gayadi.android.BuildConfig.KAKAO_MAP_JAVASCRIPT_SDK,
            kakaoMapBaseUrl = com.gayadi.android.BuildConfig.KAKAO_MAP_BASE_URL,
            friendCharacterKeys = tripParticipants.map { it.characterKey },
            showUsageGuide = remember(androidContext) {
                !UsageGuidePreferences.hasCompleted(androidContext, UsageGuidePreferences.TripHome)
            },
            onUsageGuideFinished = {
                UsageGuidePreferences.markCompleted(androidContext, UsageGuidePreferences.TripHome)
            },
            showScheduleActionsGuide = remember(androidContext) {
                !UsageGuidePreferences.hasCompleted(androidContext, UsageGuidePreferences.ScheduleActions)
            },
            onScheduleActionsGuideFinished = {
                UsageGuidePreferences.markCompleted(androidContext, UsageGuidePreferences.ScheduleActions)
            },
            tripCountdownText = buildTripCountdownText(trip?.startDate),
            onNavigateMyTrip = { navController.navigate(Routes.MY_TRIP) },
            onNavigateMyPage = { navController.navigate(Routes.MY_PAGE) },
            onNavigateLedger = { navController.navigate(Routes.tripLedger(tripId)) },
            onNavigatePlaceSearch = { date -> navController.navigate(Routes.placeSearch(tripId, date)) },
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
}

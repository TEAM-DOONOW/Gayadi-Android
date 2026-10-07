package com.gayadi.android.navigation

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.gayadi.android.ui.screens.ParticipantsScreen
import com.gayadi.android.ui.screens.PlaceDetailScreen
import com.gayadi.android.ui.screens.PlaceSearchScreen
import com.gayadi.android.ui.screens.PlaceRecommendationViewModel
import com.gayadi.android.ui.screens.PlaceCandidateViewModel
import com.gayadi.android.ui.screens.candidateSearchContext
import com.gayadi.android.ui.screens.RealtimeHomeScreen
import com.gayadi.android.ui.screens.RealtimeHomeViewModel
import com.gayadi.android.ui.screens.ScheduleMapViewModel
import com.gayadi.android.ui.screens.SettlementDetailsScreen
import com.gayadi.android.ui.screens.TravelLedgerScreen
import com.gayadi.android.ui.screens.TripCreateScreen
import com.gayadi.android.ui.screens.AgentScreen
import com.gayadi.android.ui.screens.HomeRankingViewModel
import com.gayadi.android.ui.screens.ItineraryRouteScreen
import com.gayadi.android.ui.screens.ItineraryRouteViewModel
import com.gayadi.android.ui.screens.GeoPoint
import com.gayadi.android.ui.screens.isLinked
import com.gayadi.android.ui.screens.linkedOrigin
import com.gayadi.android.ui.screens.point
import com.gayadi.android.ui.screens.replacementNeighbors
import com.gayadi.android.ui.screens.sameDayPlaceVisits
import com.gayadi.android.ui.screens.AgentViewModel
import com.gayadi.android.notification.ExpenseNotificationsBottomSheet

internal fun NavGraphBuilder.tripGraph(context: AppNavigationContext) = with(context) {
    composable(
        route = Routes.ITINERARY_ROUTE,
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
        val date = backStackEntry.arguments?.getString("date")
            ?.takeIf(String::isNotBlank)
            ?: trip?.startDate.orEmpty()
        val routeViewModel: ItineraryRouteViewModel = viewModel(
            key = "itinerary-route-$tripId-$date",
            factory = ItineraryRouteViewModel.factory(appContainer.tripSupportGateway, tripId, date),
        )
        val routeUiState by routeViewModel.uiState.collectAsStateWithLifecycle()
        LaunchedEffect(Unit) { routeViewModel.recommend() }
        ItineraryRouteScreen(
            uiState = routeUiState,
            onBack = { navController.popBackStack() },
            onTransportModeSelected = routeViewModel::selectTransportMode,
            onTimeRangeSelected = routeViewModel::selectTimeRange,
            onNewRoute = { routeViewModel.recommend(forceNewRoute = true) },
            onRetry = { routeViewModel.recommend() },
            onApply = {
                routeViewModel.apply {
                    tripViewModel.retry()
                    navController.popBackStack()
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
        var insertionBeforeId by rememberSaveable(tripId, selectedDate) { mutableStateOf<String?>(null) }
        val sameDayVisits = scheduledPlaces.filter {
            it.date == selectedDate && it.type == com.gayadi.android.domain.model.ScheduleType.MAIN && it.placeId != null
        }
        val beforeId = insertionBeforeId?.takeIf { id -> sameDayVisits.any { it.id == id } }
        val candidateViewModel: PlaceCandidateViewModel = viewModel(
            key = "place-candidates-$tripId-$selectedDate",
            factory = PlaceCandidateViewModel.factory(
                appContainer.placeCandidateGateway, appContainer.tripSupportGateway::getPlace,
                placeViewModel.uiState.value.transportMode,
            ),
        )
        val linkContext = LocalContext.current
        var linkMode by rememberSaveable { mutableStateOf(PlaceSearchPreferences.linkMode(linkContext)) }
        // 사용자가 직접 고른 교체 대상. 없으면 이 화면에서 마지막으로 추가한 일정을 교체한다.
        var replaceTargetId by rememberSaveable(tripId, selectedDate) { mutableStateOf<String?>(null) }
        // 이 화면에서 마지막으로 일정에 넣거나 바꾼 장소. 다음 추천의 기준이자 기본 교체 대상이다.
        var chainAnchorPlaceId by rememberSaveable(tripId, selectedDate) { mutableStateOf<String?>(null) }
        val orderedVisits = sameDayPlaceVisits(scheduledPlaces, tripId, selectedDate)
        val anchorSchedule = chainAnchorPlaceId?.let { anchorId -> orderedVisits.lastOrNull { it.placeId == anchorId } }
        val replaceTarget = orderedVisits.firstOrNull { it.id == replaceTargetId } ?: anchorSchedule
        val neighbors = replacementNeighbors(orderedVisits, replaceTarget?.id)
        val explicitTarget = neighbors?.takeIf { replaceTargetId != null }
        val searchContext = if (linkMode && explicitTarget != null) {
            // 교체할 일정을 빼고 그 자리에 들어갈 후보를 찾으면 앞뒤 일정 기준 추가 이동시간순이 된다.
            candidateSearchContext(
                tripId, selectedDate, city, scheduledPlaces.filterNot { it.id == explicitTarget.target.id },
                explicitTarget.next?.id,
            )
        } else {
            candidateSearchContext(tripId, selectedDate, city, scheduledPlaces, beforeId)
        }
        LaunchedEffect(searchContext) { candidateViewModel.configure(searchContext) }
        val placeUiState by candidateViewModel.uiState.collectAsStateWithLifecycle()
        LaunchedEffect(placeUiState.places) { placeViewModel.rememberCandidates(placeUiState.places) }
        val recommendationViewModel: PlaceRecommendationViewModel = viewModel(
            key = "place-recommendations-$tripId-$selectedDate",
            factory = PlaceRecommendationViewModel.factory(
                appContainer.agentGateway, appContainer.tripSupportGateway::getPlace,
            ),
        )
        val recommendationUiState by recommendationViewModel.uiState.collectAsStateWithLifecycle()
        LaunchedEffect(recommendationUiState.places) {
            placeViewModel.rememberCandidates(recommendationUiState.places.values.toList())
        }
        val destination = city
        val lastVisit = orderedVisits.lastOrNull { it.point() != null }
        val firstCandidate = placeUiState.places.firstOrNull { it.latitude != null && it.longitude != null }
        val initialOrigin = firstCandidate?.let {
            RecommendationOrigin("initial", GeoPoint(it.latitude!!, it.longitude!!), "")
        }
        val recommendationOrigin: RecommendationOrigin? = if (!linkMode) {
            // 비연계: 동선과 무관하게 지역 전체에서 추천한다.
            initialOrigin
        } else if (explicitTarget != null) {
            explicitTarget.linkedOrigin()?.let { point ->
                val previous = explicitTarget.previous?.title
                val next = explicitTarget.next?.title
                RecommendationOrigin(
                    "target:${explicitTarget.target.id}:${explicitTarget.target.placeId}",
                    point,
                    when {
                        previous != null && next != null -> "${previous}와 ${next} 사이에 가기 좋은 곳"
                        next != null -> "${next} 전에 가기 좋은 곳"
                        else -> "${previous} 다음으로 가기 좋은 곳"
                    },
                )
            } ?: initialOrigin
        } else {
            val anchorPoint = anchorSchedule?.let { schedule ->
                schedule.point() ?: schedule.placeId?.let(placeViewModel::findPlace)?.let { place ->
                    if (place.latitude != null && place.longitude != null) GeoPoint(place.latitude!!, place.longitude!!) else null
                }
            }
            when {
                anchorSchedule != null && anchorPoint != null -> RecommendationOrigin(
                    "anchor:${anchorSchedule.id}:${anchorSchedule.placeId}", anchorPoint,
                    "${anchorSchedule.title} 다음으로 가기 좋은 곳",
                )
                lastVisit != null -> RecommendationOrigin(
                    "visit:${lastVisit.id}", lastVisit.point()!!, "${lastVisit.title} 다음으로 가기 좋은 곳",
                )
                else -> initialOrigin
            }
        }
        val recommendationProfile = sharedProfileUiState.profile?.let { profile ->
            buildList {
                profile.travelStyleName?.takeIf(String::isNotBlank)?.let(::add)
                addAll(profile.strengths)
                profile.introduction.takeIf(String::isNotBlank)?.let(::add)
            }.joinToString(", ")
        }.orEmpty().ifBlank { "새로운 장소를 발견하고 여유롭게 여행하는 것을 좋아해요." }
        val groupSize = trip?.participantIds?.size?.coerceAtLeast(1) ?: 1
        val requestRecommendations: () -> Unit = {
            recommendationViewModel.recommend(
                destination = destination,
                profile = recommendationProfile,
                latitude = recommendationOrigin?.point?.latitude,
                longitude = recommendationOrigin?.point?.longitude,
                keywords = placeUiState.query.trim().takeIf(String::isNotBlank)?.let(::listOf).orEmpty(),
                groupSize = groupSize,
                force = true,
                headline = recommendationOrigin?.headline.orEmpty(),
            )
        }
        // 기준 장소가 바뀔 때마다(처음 진입, 일정 추가, 장소 변경) 그 장소에서 이어갈 곳을 다시 추천한다.
        var recommendedOriginKey by rememberSaveable(tripId, selectedDate) { mutableStateOf<String?>(null) }
        LaunchedEffect(recommendationOrigin?.key) {
            val key = recommendationOrigin?.key ?: return@LaunchedEffect
            if (key != recommendedOriginKey) {
                recommendedOriginKey = key
                requestRecommendations()
            }
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
            onQueryChange = candidateViewModel::updateQuery,
            onCategorySelected = candidateViewModel::selectCategory,
            onTransportModeSelected = { mode ->
                placeViewModel.selectTransportMode(mode)
                candidateViewModel.selectTransportMode(mode)
            },
            insertionOptions = sameDayVisits.map { it.id to it.title },
            beforeScheduleId = beforeId,
            onBeforeSelected = { insertionBeforeId = it },
            onLoadMore = candidateViewModel::loadMore,
            onPlaceClick = { id ->
                placeViewModel.rememberCandidates(placeUiState.places)
                navController.navigate(Routes.placeDetail(tripId, id, selectedDate, beforeId))
            },
            onRetry = candidateViewModel::retry,
            favoritePlaceIds = travelUiState.travelState.favoritePlaceIds,
            onToggleFavorite = { id ->
                tripViewModel.toggleFavorite(id, placeViewModel.findPlace(id)?.name)
            },
            onFavorites = { navController.navigate(Routes.favoritePlaces(tripId)) },
            onRequestRecommendations = requestRecommendations,
            replaceTargetName = replaceTarget?.title,
            onReplaceWithPlace = { place ->
                replaceTarget?.let { schedule ->
                    tripViewModel.upsertSchedule(
                        schedule.copy(
                            title = place.name,
                            placeId = place.id,
                            latitude = place.latitude,
                            longitude = place.longitude,
                        ),
                    )
                    // 직접 고른 교체 대상은 일정 ID가 유지되므로 선택을 그대로 두고, 바뀐 장소 기준으로 다시 추천한다.
                    if (replaceTargetId == null) chainAnchorPlaceId = place.id
                }
            },
            linkMode = linkMode,
            onLinkModeChange = { enabled ->
                linkMode = enabled
                PlaceSearchPreferences.setLinkMode(linkContext, enabled)
            },
            replaceTargetOptions = orderedVisits.map { it.id to it.title },
            replaceTargetId = replaceTarget?.id,
            onReplaceTargetSelected = { replaceTargetId = it },
            isLinkedReplacement = { place ->
                val point = if (place.latitude != null && place.longitude != null) {
                    GeoPoint(place.latitude!!, place.longitude!!)
                } else null
                neighbors?.isLinked(point) ?: true
            },
            onRecommendationClick = { recommendation ->
                placeViewModel.applyAgentRecommendations(listOf(recommendation))
                recommendation.placeId.toLongOrNull()
                    ?.takeIf { it > 0 }
                    ?.let {
                        navController.navigate(Routes.placeDetail(tripId, recommendation.placeId, selectedDate, beforeId))
                    }
            },
            tripName = trip?.name.orEmpty(),
            tripDate = selectedDate,
            scheduledPlaceIds = scheduledPlaces.filter { it.date == selectedDate }.mapNotNull(TravelSchedule::placeId).toSet(),
            scheduledPlaceNames = scheduledPlaces.filter { it.date == selectedDate && it.placeId == null }.map(TravelSchedule::title).toSet(),
            onAddToSchedule = { placeId, time, memo ->
                (placeUiState.places.firstOrNull { it.id == placeId } ?: placeViewModel.findPlace(placeId))?.let { place ->
                    tripViewModel.addPlaceSchedule(tripId, placeId, place.name, selectedDate, time, memo,
                        latitude = place.latitude, longitude = place.longitude, beforeScheduleId = beforeId)
                    chainAnchorPlaceId = placeId
                    replaceTargetId = null
                }
            },
        )
    }
    composable(
        route = Routes.PLACE_DETAIL,
        arguments = listOf(
            navArgument("tripId") { type = NavType.StringType },
            navArgument("placeId") { type = NavType.StringType },
            navArgument("beforeScheduleId") { type = NavType.StringType; nullable = true; defaultValue = null },
            navArgument("date") {
                type = NavType.StringType
                defaultValue = ""
            },
        ),
    ) { backStackEntry ->
        val tripId = requireNotNull(backStackEntry.arguments?.getString("tripId"))
        val placeId = requireNotNull(backStackEntry.arguments?.getString("placeId"))
        val beforeScheduleId = backStackEntry.arguments?.getString("beforeScheduleId")
        val travelState = travelUiState.travelState
        val trip = travelState.trip(tripId)
        val selectedDate = backStackEntry.arguments?.getString("date")
            ?.takeIf(String::isNotBlank)
            ?: trip?.startDate.orEmpty()
        val androidContext = LocalContext.current
        val placeUiState by placeViewModel.uiState.collectAsStateWithLifecycle()
        val detailPlace by placeViewModel.detailPlace.collectAsStateWithLifecycle()
        val weatherUiState by placeViewModel.weatherUiState.collectAsStateWithLifecycle()
        val place = detailPlace?.takeIf { it.id == placeId } ?: placeViewModel.findPlace(placeId)
            ?: placeUiState.places.firstOrNull { it.id == placeId }
        val hourlyUiState by placeViewModel.hourlyUiState.collectAsStateWithLifecycle()
        val forecastUiState by placeViewModel.forecastUiState.collectAsStateWithLifecycle()
        LaunchedEffect(placeId, selectedDate) {
            placeViewModel.loadPlaceDetails(placeId, selectedDate)
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
            isScheduled = travelState.schedulesForTrip(tripId).any { it.placeId == placeId && it.date == selectedDate },
            onBack = { navController.popBackStack() },
            onAddToSchedule = { time, memo ->
                placeViewModel.findPlace(placeId)?.let { place ->
                    tripViewModel.addPlaceSchedule(tripId, placeId, place.name, selectedDate, time, memo,
                        latitude = place.latitude, longitude = place.longitude, beforeScheduleId = beforeScheduleId)
                }
            },
            isFavorite = placeId in travelState.favoritePlaceIds,
            onToggleFavorite = {
                tripViewModel.toggleFavorite(placeId, placeViewModel.findPlace(placeId)?.name)
            },
            hourlyUiState = hourlyUiState,
            forecastUiState = forecastUiState,
            onForecastRetry = { placeViewModel.loadPlaceDetails(placeId, selectedDate) },
            onHourlyRetry = { placeViewModel.loadPlaceDetails(placeId, selectedDate) },
            weatherUiState = weatherUiState,
            onWeatherRetry = { placeViewModel.loadPlaceDetails(placeId, selectedDate) },
        )
    }
    composable(Routes.MY_TRIP) {
        LaunchedEffect(Unit) { tripViewModel.retry() }
        val androidContext = LocalContext.current
        val showFirstGuide = remember(androidContext) {
            !UsageGuidePreferences.hasCompleted(androidContext, UsageGuidePreferences.MyTrip)
        }
        val rankingViewModel: HomeRankingViewModel = viewModel(
            key = "home-rankings",
            factory = HomeRankingViewModel.factory(appContainer.rankingGateway),
        )
        val rankingUiState by rankingViewModel.uiState.collectAsStateWithLifecycle()
        MyTripScreen(
            trips = trips,
            rankingUiState = rankingUiState,
            onRankingCategorySelected = rankingViewModel::select,
            onRankingRetry = rankingViewModel::retry,
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
        val mapViewModel: ScheduleMapViewModel = viewModel(
            key = "schedule-map-$tripId",
            factory = ScheduleMapViewModel.factory(appContainer.tripSupportGateway::getPlace),
        )
        val mapUiState by mapViewModel.uiState.collectAsStateWithLifecycle()
        LaunchedEffect(tripSchedules) { mapViewModel.load(tripSchedules.mapNotNull { it.placeId }) }
        val tripParticipants = travelState.participantsForTrip(tripId, tripViewModel.availableParticipants)
        var showNotifications by rememberSaveable(tripId) { mutableStateOf(false) }
        if (showNotifications) {
            ExpenseNotificationsBottomSheet(
                tripId = tripId,
                scheduleIds = tripSchedules.map { it.id },
                onDismiss = { showNotifications = false },
                onOpenExpense = { scheduleId ->
                    showNotifications = false
                    navController.navigate(Routes.tripExpense(tripId, scheduleId))
                },
            )
        }
        RealtimeHomeScreen(
            uiState = homeUiState,
            tripTitle = trip?.name ?: "선택한 여행",
            travelPlans = tripSchedules.map { schedule ->
                val coordinates = mapUiState.coordinates[schedule.placeId]
                schedule.toHomeTravelPlan().copy(
                    latitude = schedule.latitude ?: coordinates?.latitude,
                    longitude = schedule.longitude ?: coordinates?.longitude,
                    placeId = mapUiState.places[schedule.placeId]?.contentId,
                    imageUrl = mapUiState.places[schedule.placeId]?.imageUrl.orEmpty(),
                )
            },
            tripDays = trip?.let { buildHomeTripDays(it.startDate, it.endDate) }.orEmpty(),
            participantCount = tripParticipants.size,
            tripStartDate = trip?.startDate.orEmpty(),
            tripEndDate = trip?.endDate.orEmpty(),
            tripCoverImageResList = tripSummary?.coverImageResList.orEmpty(),
            kakaoMapJavaScriptKey = com.gayadi.android.BuildConfig.KAKAO_MAP_JAVASCRIPT_SDK,
            kakaoMapBaseUrl = com.gayadi.android.BuildConfig.KAKAO_MAP_BASE_URL,
            isMapLoading = mapUiState.isLoading,
            mapErrorMessage = mapUiState.errorMessage,
            onRetryMap = { mapViewModel.load(tripSchedules.mapNotNull { it.placeId }, retry = true) },
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
            onNavigateNotifications = { showNotifications = true },
            onNavigatePlaceDetail = { placeId, date ->
                mapUiState.places[placeId]?.let(placeViewModel::rememberPlace)
                navController.navigate(Routes.placeDetail(tripId, placeId, date))
            },
            onNavigatePlaceSearch = { date -> navController.navigate(Routes.placeSearch(tripId, date)) },
            onNavigateItineraryRoute = { date -> navController.navigate(Routes.itineraryRoute(tripId, date)) },
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
        )
    }
}

/** 추천 기준점. [key]가 바뀌면 추천을 다시 요청한다. */
private data class RecommendationOrigin(
    val key: String,
    val point: GeoPoint,
    val headline: String,
)

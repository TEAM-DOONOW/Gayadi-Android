package com.gayadi.android.navigation

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.gayadi.android.di.AppContainer
import com.gayadi.android.domain.error.isCoroutineCancellationMessage
import com.gayadi.android.domain.model.UserProfile
import com.gayadi.android.notification.ExpenseReminderScheduler
import com.gayadi.android.notification.syncExpenseRemindersWithRetry
import com.gayadi.android.ui.screens.PlaceViewModel
import com.gayadi.android.ui.screens.ProfileViewModel
import com.gayadi.android.ui.screens.TripViewModel

@Composable
fun GayadiNavHost(appContainer: AppContainer) {
    val navController = rememberNavController()
    val appScope = rememberCoroutineScope()
    val context = LocalContext.current
    val reminderScheduler = remember(context) { ExpenseReminderScheduler(context) }
    val googleLoginViewModel: GoogleLoginViewModel = viewModel(
        factory = GoogleLoginViewModel.factory(
            appContainer.signInWithGoogleUseCase,
            appContainer.getUserProfileUseCase,
        ),
    )
    val googleLoginUiState by googleLoginViewModel.uiState.collectAsStateWithLifecycle()
    val authSession by appContainer.authRepository.observeSession().collectAsStateWithLifecycle(
        initialValue = appContainer.authRepository.currentSession(),
    )
    val tripViewModel: TripViewModel = viewModel(
        factory = TripViewModel.factory(
            appContainer.getTravelStateUseCase,
            appContainer.saveTravelStateUseCase,
            appContainer.updateTravelStateUseCase,
            appContainer.publishTripInviteUseCase,
            appContainer.observeSharedTripInviteUseCase,
            appContainer.removeSharedTripParticipantUseCase,
            appContainer.submitSharedTripAvailabilityUseCase,
            appContainer.finalizeSharedTripDatesUseCase,
            travelGateway = appContainer.travelGateway,
            authRepository = appContainer.authRepository,
        ),
    )
    val placeViewModel: PlaceViewModel = viewModel(
        factory = PlaceViewModel.factory(
            appContainer.getTourPlacesUseCase,
            appContainer.getNearbyTourPlacesUseCase,
            appContainer.searchTourPlacesUseCase,
            appContainer.getCongestionHourlyUseCase,
        ),
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
    LaunchedEffect(travelUiState.hasLoadedTravelState) {
        if (travelUiState.hasLoadedTravelState) {
            placeViewModel.retry()
        }
    }
    LaunchedEffect(googleLoginUiState.loginCompleted) {
        if (!googleLoginUiState.loginCompleted) return@LaunchedEffect
        sharedProfileViewModel.reload()
        tripViewModel.retry()
        placeViewModel.retry()
        if (navController.currentDestination?.route == Routes.LOGIN) {
            navController.navigate(
                resolveAuthenticatedDestination(googleLoginUiState.completedProfile),
            ) {
                popUpTo(Routes.LOGIN) { inclusive = true }
            }
        }
        googleLoginViewModel.consumeCompletion()
    }
    LaunchedEffect(authSession) {
        if (authSession != null) return@LaunchedEffect
        val route = navController.currentDestination?.route
        if (route != null && route !in unauthenticatedRoutes) {
            navController.navigate(Routes.LOGIN) {
                popUpTo(navController.graph.id) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    val navigationContext = AppNavigationContext(
        navController = navController,
        appContainer = appContainer,
        appScope = appScope,
        tripViewModel = tripViewModel,
        placeViewModel = placeViewModel,
        trips = trips,
        selectedTripId = selectedTripId,
        travelUiState = travelUiState,
        sharedProfileViewModel = sharedProfileViewModel,
        sharedProfileUiState = sharedProfileUiState,
        googleLoginViewModel = googleLoginViewModel,
        googleLoginUiState = googleLoginUiState,
    )

    travelUiState.errorMessage
        ?.takeIf { authSession != null }
        ?.takeUnless { it.isCoroutineCancellationMessage() }
        ?.let { message ->
            AlertDialog(
                onDismissRequest = tripViewModel::dismissError,
                title = { Text("여행 정보를 확인해 주세요") },
                text = { Text(message) },
                confirmButton = { TextButton(onClick = tripViewModel::retry) { Text("다시 시도") } },
                dismissButton = { TextButton(onClick = tripViewModel::dismissError) { Text("닫기") } },
            )
        }
    NavHost(navController = navController, startDestination = Routes.STARTUP) {
        onboardingGraph(navigationContext)
        tripGraph(navigationContext)
        myPageGraph(navigationContext)
    }
}

private val unauthenticatedRoutes = setOf(
    Routes.STARTUP,
    Routes.LOGIN,
    Routes.LEGAL_DOCUMENT,
)

internal fun resolveStartupDestination(profile: UserProfile?): String = when {
    profile?.nickname.isNullOrBlank() -> Routes.LOGIN
    profile?.characterKey.isNullOrBlank() -> Routes.SURVEY
    else -> Routes.MY_TRIP
}

internal fun resolveAuthenticatedDestination(profile: UserProfile?): String = when {
    profile?.nickname.isNullOrBlank() -> Routes.BASIC_INFO
    profile?.characterKey.isNullOrBlank() -> Routes.SURVEY
    else -> Routes.MY_TRIP
}

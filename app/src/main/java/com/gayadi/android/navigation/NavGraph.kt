package com.gayadi.android.navigation

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
        ),
    )
    val placeViewModel: PlaceViewModel = viewModel(
        factory = PlaceViewModel.factory(
            appContainer.getTourPlacesUseCase,
            appContainer.getNearbyTourPlacesUseCase,
            appContainer.searchTourPlacesUseCase,
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
    )

    NavHost(navController = navController, startDestination = Routes.STARTUP) {
        onboardingGraph(navigationContext)
        tripGraph(navigationContext)
        myPageGraph(navigationContext)
    }
}

internal fun resolveStartupDestination(profile: UserProfile?): String = when {
    profile?.nickname.isNullOrBlank() -> Routes.LOGIN
    profile?.characterKey.isNullOrBlank() -> Routes.SURVEY
    else -> Routes.MY_TRIP
}

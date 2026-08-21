package com.gayadi.android.navigation

import androidx.navigation.NavHostController
import com.gayadi.android.di.AppContainer
import com.gayadi.android.ui.screens.PlaceViewModel
import com.gayadi.android.ui.screens.ProfileUiState
import com.gayadi.android.ui.screens.ProfileViewModel
import com.gayadi.android.ui.screens.TravelUiState
import com.gayadi.android.ui.screens.TripSummary
import com.gayadi.android.ui.screens.TripViewModel
import kotlinx.coroutines.CoroutineScope

internal data class AppNavigationContext(
    val navController: NavHostController,
    val appContainer: AppContainer,
    val appScope: CoroutineScope,
    val tripViewModel: TripViewModel,
    val placeViewModel: PlaceViewModel,
    val trips: List<TripSummary>,
    val selectedTripId: String?,
    val travelUiState: TravelUiState,
    val sharedProfileViewModel: ProfileViewModel,
    val sharedProfileUiState: ProfileUiState,
)

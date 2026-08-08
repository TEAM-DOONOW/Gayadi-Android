package com.gayadi.android.domain.repository

import com.gayadi.android.domain.model.TravelState

/** Persists the complete Android-local travel aggregate until server APIs are connected. */
interface TravelRepository {
    suspend fun getTravelState(): Result<TravelState>
    suspend fun saveTravelState(state: TravelState): Result<Unit>
}

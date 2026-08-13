package com.gayadi.android.domain.repository

import com.gayadi.android.domain.model.TravelState

/** Persists the complete Android-local travel aggregate until server APIs are connected. */
interface TravelRepository {
    suspend fun getTravelState(): Result<TravelState>
    suspend fun saveTravelState(state: TravelState): Result<Unit>

    /**
     * Applies one read-modify-write operation to the complete aggregate.
     *
     * Persistent implementations must override this method when multiple callers can mutate the
     * same storage concurrently. The default keeps simple in-memory test repositories source
     * compatible.
     */
    suspend fun updateTravelState(transform: (TravelState) -> TravelState): Result<TravelState> {
        val current = getTravelState().getOrElse { return Result.failure(it) }
        val updated = runCatching { transform(current) }.getOrElse { return Result.failure(it) }
        return saveTravelState(updated).map { updated }
    }
}

package com.gayadi.android.domain.usecase

import com.gayadi.android.domain.model.SharedTripInvite
import com.gayadi.android.domain.repository.TripInviteRepository
import kotlinx.coroutines.flow.Flow

class ObserveSharedTripInviteUseCase(private val repository: TripInviteRepository) {
    operator fun invoke(inviteCode: String): Flow<Result<SharedTripInvite>> = repository.observe(inviteCode)
}

class SubmitSharedTripAvailabilityUseCase(private val repository: TripInviteRepository) {
    suspend operator fun invoke(inviteCode: String, dates: List<String>): Result<Unit> =
        repository.submitAvailability(inviteCode, dates)
}

class FinalizeSharedTripDatesUseCase(private val repository: TripInviteRepository) {
    suspend operator fun invoke(inviteCode: String, startDate: String, endDate: String): Result<Unit> =
        repository.finalizeDates(inviteCode, startDate, endDate)
}

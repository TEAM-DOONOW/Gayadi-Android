package com.gayadi.android.domain

import com.gayadi.android.domain.model.SharedTripInvite
import com.gayadi.android.domain.model.TravelParticipant
import com.gayadi.android.domain.model.TravelState
import com.gayadi.android.domain.model.TravelTrip
import com.gayadi.android.domain.repository.TripInviteRepository
import com.gayadi.android.domain.repository.TravelRepository
import com.gayadi.android.domain.usecase.JoinTripByInviteCodeUseCase
import com.gayadi.android.domain.usecase.SubmitSharedTripAvailabilityUseCase
import com.gayadi.android.domain.usecase.FinalizeSharedTripDatesUseCase
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Test

class TripInviteUseCaseTest {

    @Test
    fun sharedAvailabilityAndFinalDatesAreForwardedToRemoteInvite() = runTest {
        val remote = FakeTripInviteRepository(sampleInvite())

        SubmitSharedTripAvailabilityUseCase(remote)("AB12CD", listOf("2026.08.21", "2026.08.22")).getOrThrow()
        FinalizeSharedTripDatesUseCase(remote)("AB12CD", "2026.08.21", "2026.08.22").getOrThrow()

        assertEquals(listOf("2026.08.21", "2026.08.22"), remote.submittedDates)
        assertEquals("2026.08.21" to "2026.08.22", remote.finalizedRange)
    }

    @Test
    fun remoteInviteAddsTripAndEveryParticipantToLocalState() = runTest {
        val local = MemoryTravelRepository()
        val owner = TravelParticipant("owner-installation", "미르")
        val joiningUser = TravelParticipant("local-user", "친구")
        val remote = FakeTripInviteRepository(
            SharedTripInvite(
                trip = TravelTrip(
                    id = "shared-trip",
                    name = "서울 여행",
                    startDate = "2026.08.19",
                    endDate = "2026.08.20",
                    cities = listOf("서울"),
                    inviteCode = "AB12CD",
                ),
                participants = listOf(owner, joiningUser),
            ),
        )

        val joined = JoinTripByInviteCodeUseCase(local, remote)("ab12cd", joiningUser).getOrThrow()

        assertEquals("shared-trip", joined.id)
        assertEquals("shared-trip", local.state.selectedTripId)
        assertEquals(listOf("owner-installation", "local-user"), local.state.trips.single().participantIds)
        assertEquals(listOf(owner, joiningUser), local.state.participants)
    }
}

private fun sampleInvite() = SharedTripInvite(
    trip = TravelTrip("shared-trip", "서울 여행", "", "", listOf("서울"), inviteCode = "AB12CD"),
    participants = listOf(TravelParticipant("local-user", "친구")),
)

private class FakeTripInviteRepository(
    private val invite: SharedTripInvite,
) : TripInviteRepository {
    var submittedDates: List<String> = emptyList()
    var finalizedRange: Pair<String, String>? = null
    override suspend fun publish(trip: TravelTrip, owner: TravelParticipant) = Result.success(Unit)
    override suspend fun join(inviteCode: String, participant: TravelParticipant) = Result.success(invite)
    override fun observe(inviteCode: String) = flowOf(Result.success(invite))
    override suspend fun submitAvailability(inviteCode: String, dates: List<String>) =
        Result.success(Unit).also { submittedDates = dates }
    override suspend fun finalizeDates(inviteCode: String, startDate: String, endDate: String) =
        Result.success(Unit).also { finalizedRange = startDate to endDate }
}

private class MemoryTravelRepository : TravelRepository {
    var state = TravelState()

    override suspend fun getTravelState() = Result.success(state)
    override suspend fun saveTravelState(state: TravelState) = Result.success(Unit).also { this.state = state }
    override suspend fun updateTravelState(transform: (TravelState) -> TravelState): Result<TravelState> =
        runCatching { transform(state) }.onSuccess { state = it }
}

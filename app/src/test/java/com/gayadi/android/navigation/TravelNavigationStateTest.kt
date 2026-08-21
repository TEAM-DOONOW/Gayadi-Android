package com.gayadi.android.navigation

import com.gayadi.android.domain.model.TravelParticipant
import com.gayadi.android.domain.model.TravelSchedule
import com.gayadi.android.domain.model.TravelState
import com.gayadi.android.domain.model.TravelTrip
import org.junit.Assert.assertEquals
import org.junit.Test

class TravelNavigationStateTest {
    @Test
    fun schedulesForTripFiltersAndSortsByOrder() {
        val state = TravelState(
            schedules = listOf(
                schedule("second", "trip-1", order = 2),
                schedule("other", "trip-2", order = 0),
                schedule("first", "trip-1", order = 1),
            ),
        )

        assertEquals(listOf("first", "second"), state.schedulesForTrip("trip-1").map { it.id })
    }

    @Test
    fun participantsForTripMergesCandidatesWithoutDuplicates() {
        val owner = TravelParticipant("owner", "방장")
        val guest = TravelParticipant("guest", "참여자")
        val state = TravelState(
            trips = listOf(
                TravelTrip(
                    id = "trip-1",
                    name = "제주 여행",
                    startDate = "2026.08.21",
                    endDate = "2026.08.23",
                    cities = listOf("제주"),
                    participantIds = listOf(owner.id, guest.id),
                ),
            ),
            participants = listOf(owner),
        )

        val participants = state.participantsForTrip(
            tripId = "trip-1",
            candidates = listOf(owner.copy(nickname = "중복"), guest, TravelParticipant("outsider", "외부")),
        )

        assertEquals(listOf(owner, guest), participants)
    }

    private fun schedule(id: String, tripId: String, order: Int) = TravelSchedule(
        id = id,
        tripId = tripId,
        title = id,
        date = "2026.08.21",
        time = "10:00",
        order = order,
    )
}

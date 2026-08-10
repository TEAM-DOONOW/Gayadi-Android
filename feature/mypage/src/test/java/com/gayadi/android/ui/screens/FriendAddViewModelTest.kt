package com.gayadi.android.ui.screens

import com.gayadi.android.domain.model.TravelParticipant
import com.gayadi.android.domain.model.TravelState
import com.gayadi.android.domain.model.TravelTrip
import com.gayadi.android.domain.repository.TravelRepository
import com.gayadi.android.domain.usecase.JoinTripByInviteCodeUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FriendAddViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `invite code joins only the matching persisted trip`() = runTest(dispatcher) {
        val repository = MemoryTravelRepository(
            TravelState(trips = listOf(
                trip("trip-a", "가야 여행", "GAYADI"),
                trip("trip-b", "다온 여행", "DAON12"),
            )),
        )
        val participant = TravelParticipant("local-user", "가야")
        val viewModel = FriendAddViewModel(
            joinTripByInviteCode = JoinTripByInviteCodeUseCase(repository),
            localParticipant = participant,
            ioDispatcher = dispatcher,
        )

        viewModel.updateFriendCode("ga한y-adi!")
        assertEquals("GAYADI", viewModel.uiState.value.friendCode)

        viewModel.addFriendByCode()
        advanceUntilIdle()

        assertEquals("", viewModel.uiState.value.friendCode)
        assertTrue(viewModel.uiState.value.codeMessage?.contains("가야 여행") == true)
        assertEquals(listOf("local-user"), repository.state.trips.single { it.id == "trip-a" }.participantIds)
        assertTrue(repository.state.trips.single { it.id == "trip-b" }.participantIds.isEmpty())
        assertEquals(participant, repository.state.participants.single())
    }

    @Test
    fun `unknown six character code does not change persisted trips`() = runTest(dispatcher) {
        val initialState = TravelState(trips = listOf(trip("trip-a", "가야 여행", "GAYADI")))
        val repository = MemoryTravelRepository(initialState)
        val viewModel = FriendAddViewModel(
            joinTripByInviteCode = JoinTripByInviteCodeUseCase(repository),
            ioDispatcher = dispatcher,
        )

        viewModel.updateFriendCode("ABC123")
        viewModel.addFriendByCode()
        advanceUntilIdle()

        assertEquals("ABC123", viewModel.uiState.value.friendCode)
        assertEquals("유효하지 않은 초대 코드예요", viewModel.uiState.value.codeMessage)
        assertEquals(initialState, repository.state)
    }

    @Test
    fun `non ascii input does not change friend list`() {
        val viewModel = FriendAddViewModel(FakeFriendRepository())
        val initialFriends = viewModel.uiState.value.friends

        viewModel.updateFriendCode("가나다라마바")
        viewModel.addFriendByCode()

        assertEquals("", viewModel.uiState.value.friendCode)
        assertEquals(initialFriends, viewModel.uiState.value.friends)
    }

    @Test
    fun queryFiltersAndAddUpdatesRecommendedFriend() {
        val viewModel = FriendAddViewModel()

        viewModel.updateQuery("시연")
        assertEquals(listOf("시연"), viewModel.uiState.value.visibleFriends.map(FriendItem::name))

        viewModel.addFriend("friend-4")
        assertEquals(FriendStatus.ADDED, viewModel.uiState.value.visibleFriends.single().status)
        viewModel.retry()
        assertEquals(FriendStatus.ADDED, viewModel.uiState.value.visibleFriends.single().status)
    }

    private fun trip(id: String, name: String, inviteCode: String) = TravelTrip(
        id = id,
        name = name,
        startDate = "2026.08.11",
        endDate = "2026.08.12",
        cities = listOf("서울"),
        inviteCode = inviteCode,
    )
}

private class MemoryTravelRepository(initialState: TravelState) : TravelRepository {
    var state = initialState

    override suspend fun getTravelState(): Result<TravelState> = Result.success(state)

    override suspend fun saveTravelState(state: TravelState): Result<Unit> {
        this.state = state
        return Result.success(Unit)
    }
}

package com.gayadi.android.ui.screens

import com.gayadi.android.domain.repository.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class FriendshipViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun teardown() { Dispatchers.resetMain() }
    @Test fun failedMutationPreservesRelationshipsAndPreventsDuplicateRequests() = runTest(dispatcher) {
        val friend = Friendship("1", FriendshipUser("2", "친구"), "PENDING", false, true, 3)
        var requests = 0
        val gate = CompletableDeferred<Unit>()
        val gateway = object : FriendshipGateway {
            override suspend fun list() = listOf(friend)
            override suspend fun search(query: String) = emptyList<FriendshipUser>()
            override suspend fun request(userId: String) {}
            override suspend fun decide(friendship: Friendship, accept: Boolean) {
                requests++; gate.await(); error("다시 조회해 주세요")
            }
            override suspend fun delete(friendshipId: String) {}
        }
        val vm = FriendshipViewModel(gateway)
        advanceUntilIdle()
        vm.decide(friend, true); vm.decide(friend, true)
        runCurrent()
        assertEquals(1, requests)
        gate.complete(Unit); advanceUntilIdle()
        assertEquals(listOf(friend), vm.state.value.friends)
        assertNotNull(vm.state.value.error)
        assertFalse(vm.state.value.busy)
    }
}

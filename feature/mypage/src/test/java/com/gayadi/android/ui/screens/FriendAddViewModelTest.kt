package com.gayadi.android.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FriendAddViewModelTest {
    @Test
    fun `six character code adds a recommended friend`() {
        val viewModel = FriendAddViewModel(FakeFriendRepository())

        viewModel.updateFriendCode("ga한y-adi!")
        assertEquals("GAYADI", viewModel.uiState.value.friendCode)

        viewModel.addFriendByCode()

        assertEquals("", viewModel.uiState.value.friendCode)
        assertTrue(viewModel.uiState.value.codeMessage?.contains("추가했어요") == true)
        assertTrue(viewModel.uiState.value.friends.any { it.status == FriendStatus.ADDED })
    }

    @Test
    fun `unknown six character code does not add a friend`() {
        val viewModel = FriendAddViewModel(FakeFriendRepository())

        viewModel.updateFriendCode("ABC123")
        viewModel.addFriendByCode()

        assertEquals("ABC123", viewModel.uiState.value.friendCode)
        assertEquals("유효하지 않은 초대 코드예요", viewModel.uiState.value.codeMessage)
        assertTrue(viewModel.uiState.value.friends.none { it.status == FriendStatus.ADDED })
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
}

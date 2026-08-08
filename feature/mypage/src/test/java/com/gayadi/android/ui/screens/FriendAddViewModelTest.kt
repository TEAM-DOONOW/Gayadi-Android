package com.gayadi.android.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Test

class FriendAddViewModelTest {
    @Test
    fun queryFiltersAndAddUpdatesRecommendedFriend() {
        val viewModel = FriendAddViewModel()

        viewModel.updateQuery("시연")
        assertEquals(listOf("시연"), viewModel.uiState.value.visibleFriends.map(FriendItem::name))

        viewModel.addFriend("friend-4")
        assertEquals(FriendStatus.ADDED, viewModel.uiState.value.visibleFriends.single().status)
    }
}

package com.gayadi.android.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class FriendStatus { TRAVEL_MATE, INVITED, RECOMMENDED, ADDED }

data class FriendItem(
    val id: String,
    val name: String,
    val handle: String,
    val emoji: String,
    val status: FriendStatus,
)

data class FriendAddUiState(
    val query: String = "",
    val friends: List<FriendItem> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
) {
    val visibleFriends: List<FriendItem>
        get() = friends.filter {
            query.isBlank() || it.name.contains(query, ignoreCase = true) ||
                it.handle.contains(query, ignoreCase = true)
        }
}

interface FriendRepository {
    fun getFriends(): Result<List<FriendItem>>
    fun addFriend(friendId: String): Result<Unit>
}

class FakeFriendRepository : FriendRepository {
    private var friends = listOf(
            FriendItem("friend-1", "석혁", "@sunghyeok", "🐱", FriendStatus.TRAVEL_MATE),
            FriendItem("friend-2", "민수", "@mintsu", "🐶", FriendStatus.TRAVEL_MATE),
            FriendItem("friend-3", "지은", "@jieun", "🐱", FriendStatus.INVITED),
            FriendItem("friend-4", "시연", "@siyeon", "🐶", FriendStatus.RECOMMENDED),
        )

    override fun getFriends(): Result<List<FriendItem>> = Result.success(friends)

    override fun addFriend(friendId: String): Result<Unit> {
        if (friends.none { it.id == friendId }) return Result.failure(IllegalArgumentException("친구를 찾을 수 없습니다."))
        friends = friends.map { friend ->
            if (friend.id == friendId && friend.status == FriendStatus.RECOMMENDED) {
                friend.copy(status = FriendStatus.ADDED)
            } else friend
        }
        return Result.success(Unit)
    }
}

class FriendAddViewModel(
    private val repository: FriendRepository = FakeFriendRepository(),
) : ViewModel() {
    private val _uiState = MutableStateFlow(FriendAddUiState())
    val uiState: StateFlow<FriendAddUiState> = _uiState.asStateFlow()

    init {
        loadFriends()
    }

    fun updateQuery(query: String) {
        _uiState.update { it.copy(query = query) }
    }

    fun addFriend(friendId: String) {
        repository.addFriend(friendId).fold(
            onSuccess = { loadFriends() },
            onFailure = { error -> _uiState.update { it.copy(errorMessage = error.message) } },
        )
    }

    fun retry() = loadFriends()

    private fun loadFriends() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        repository.getFriends().fold(
            onSuccess = { friends ->
                _uiState.update { it.copy(friends = friends, isLoading = false, errorMessage = null) }
            },
            onFailure = { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "친구 목록을 불러오지 못했습니다.",
                    )
                }
            },
        )
    }

    companion object {
        fun factory(repository: FriendRepository = FakeFriendRepository()) = viewModelFactory {
            initializer { FriendAddViewModel(repository) }
        }
    }
}

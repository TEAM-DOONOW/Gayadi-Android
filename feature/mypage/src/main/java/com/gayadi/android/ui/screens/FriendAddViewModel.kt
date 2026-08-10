package com.gayadi.android.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import java.util.Locale
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
    val friendCode: String = "",
    val codeMessage: String? = null,
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
    fun addFriendByCode(code: String): Result<FriendItem>
}

class FakeFriendRepository : FriendRepository {
    private val friendIdsByCode = mapOf("GAYADI" to "friend-4")
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

    override fun addFriendByCode(code: String): Result<FriendItem> {
        val friendId = friendIdsByCode[code]
            ?: return Result.failure(IllegalArgumentException("유효하지 않은 초대 코드예요"))
        val friend = friends.firstOrNull { it.id == friendId }
            ?: return Result.failure(IllegalArgumentException("친구를 찾을 수 없습니다."))
        if (friend.status != FriendStatus.RECOMMENDED) {
            return Result.failure(IllegalStateException("이미 추가된 여행메이트예요"))
        }
        return addFriend(friendId).map { friend.copy(status = FriendStatus.ADDED) }
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

    fun updateFriendCode(code: String) {
        val normalizedCode = code.uppercase(Locale.ROOT)
            .filter { it in 'A'..'Z' || it in '0'..'9' }
            .take(6)
        _uiState.update { it.copy(friendCode = normalizedCode, codeMessage = null) }
    }

    fun addFriendByCode() {
        val code = _uiState.value.friendCode
        if (code.length != 6) return
        repository.addFriendByCode(code).fold(
            onSuccess = { addedFriend ->
                loadFriends()
                _uiState.update { it.copy(friendCode = "", codeMessage = "${addedFriend.name} 님을 여행메이트로 추가했어요") }
            },
            onFailure = { error -> _uiState.update { it.copy(codeMessage = error.message ?: "친구를 추가하지 못했어요") } },
        )
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

package com.gayadi.android.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.gayadi.android.domain.repository.*
import com.gayadi.android.domain.error.rethrowCancellation
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FriendshipUiState(val busy: Boolean = false, val error: String? = null,
    val friends: List<Friendship> = emptyList(), val results: List<FriendshipUser> = emptyList(), val searched: Boolean = false)
class FriendshipViewModel(private val gateway: FriendshipGateway) : ViewModel() {
    private val mutable = MutableStateFlow(FriendshipUiState())
    val state = mutable.asStateFlow()
    init { reload() }
    fun reload() = execute {
        val friends = gateway.list()
        mutable.update { it.copy(friends = friends) }
    }
    fun search(query: String) = execute {
        val results = gateway.search(query)
        mutable.update { it.copy(results = results, searched = true) }
    }
    fun request(userId: String) = mutate { gateway.request(userId) }
    fun decide(friendship: Friendship, accept: Boolean) = mutate { gateway.decide(friendship, accept) }
    fun delete(friendship: Friendship) = mutate { gateway.delete(friendship.id) }
    private fun mutate(action: suspend () -> Unit) = execute {
        action()
        val friends = gateway.list()
        mutable.update { it.copy(friends = friends, results = emptyList(), searched = false) }
    }
    private fun execute(action: suspend () -> Unit) {
        if (mutable.value.busy) return
        mutable.update { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            try { action() }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                e.rethrowCancellation()
                mutable.update { it.copy(error = e.message ?: "요청을 처리하지 못했어요") }
            }
            finally { mutable.update { it.copy(busy = false) } }
        }
    }
    companion object {
        fun factory(gateway: FriendshipGateway) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = FriendshipViewModel(gateway) as T
        }
    }
}

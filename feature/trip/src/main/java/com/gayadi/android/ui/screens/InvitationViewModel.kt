package com.gayadi.android.ui.screens

import androidx.lifecycle.*
import com.gayadi.android.domain.repository.*
import com.gayadi.android.domain.model.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class InvitationUiState(val busy: Boolean = false, val error: String? = null,
    val users: List<FriendshipUser> = emptyList(), val invitations: List<TravelInvitation> = emptyList())
class InvitationViewModel(private val travel: TravelGateway, private val friends: FriendshipGateway,
    private val tripId: String) : ViewModel() {
    private val mutable = MutableStateFlow(InvitationUiState())
    val state = mutable.asStateFlow()
    init { reload() }
    private suspend fun list(): List<TravelInvitation> = buildList {
        var offset = 0
        do {
            val page = travel.listInvitations(tripId, 100, offset)
            addAll(page); offset += page.size
        } while(page.size == 100)
    }
    fun reload() = execute { val rows = list(); mutable.update { it.copy(invitations = rows) } }
    fun search(query: String) = execute { val users = friends.search(query); mutable.update { it.copy(users = users) } }
    fun invite(user: FriendshipUser) = execute {
        travel.createInvitation(tripId, user.id)
        val rows = list(); mutable.update { it.copy(invitations = rows, users = emptyList()) }
    }
    fun cancel(invitation: TravelInvitation) = execute {
        travel.updateInvitationStatus(tripId, invitation.id, InvitationDecision.CANCELLED)
        val rows = list(); mutable.update { it.copy(invitations = rows) }
    }
    private fun execute(action: suspend () -> Unit) {
        if(mutable.value.busy) return
        mutable.update { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            try { action() }
            catch(e: CancellationException) { throw e }
            catch(e: Exception) { mutable.update { it.copy(error = e.message ?: "초대를 처리하지 못했어요") } }
            finally { mutable.update { it.copy(busy = false) } }
        }
    }
    companion object {
        fun factory(travel: TravelGateway, friends: FriendshipGateway, tripId: String) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = InvitationViewModel(travel, friends, tripId) as T
        }
    }
}

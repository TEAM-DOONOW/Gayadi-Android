package com.gayadi.android.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.gayadi.android.domain.error.rethrowCancellation
import com.gayadi.android.domain.error.userFacingMessage
import com.gayadi.android.domain.model.AgentChangeProposal
import com.gayadi.android.domain.model.AgentSituationResponse
import com.gayadi.android.domain.repository.AgentGateway
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AgentUiState(
    val proposals: List<AgentChangeProposal> = emptyList(),
    val latestResponse: AgentSituationResponse? = null,
    val selectedOptionKeys: Map<String, String> = emptyMap(),
    val isLoading: Boolean = false,
    val isAnalyzing: Boolean = false,
    val decidingProposalId: String? = null,
    val errorMessage: String? = null,
)

class AgentViewModel(
    private val tripId: String?,
    private val gateway: AgentGateway,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {
    private val _uiState = MutableStateFlow(AgentUiState())
    val uiState: StateFlow<AgentUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        val id = tripId ?: return
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch(ioDispatcher) {
            runCatching { gateway.listChangeProposals(id) }.fold(
                onSuccess = { proposals ->
                    _uiState.update {
                        it.copy(
                            proposals = proposals,
                            selectedOptionKeys = proposals.associate { proposal ->
                                proposal.id to (
                                    it.selectedOptionKeys[proposal.id]
                                        ?: proposal.selectedOptionKey
                                        ?: proposal.options.firstOrNull()?.key.orEmpty()
                                    )
                            },
                            isLoading = false,
                        )
                    }
                },
                onFailure = { error ->
                    error.rethrowCancellation()
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.userFacingMessage("에이전트 알림을 불러오지 못했어요."),
                        )
                    }
                },
            )
        }
    }

    fun analyze(latitude: Double?, longitude: Double?) {
        val id = tripId
        if (id == null) {
            _uiState.update { it.copy(errorMessage = "먼저 여행을 만들어 주세요.") }
            return
        }
        if (latitude == null || longitude == null) {
            _uiState.update { it.copy(errorMessage = "여행지 위치를 불러온 뒤 다시 시도해 주세요.") }
            return
        }
        _uiState.update { it.copy(isAnalyzing = true, errorMessage = null) }
        viewModelScope.launch(ioDispatcher) {
            runCatching { gateway.analyzeSituation(id, latitude, longitude) }.fold(
                onSuccess = { response ->
                    _uiState.update { state ->
                        val proposal = response.changeProposal
                        state.copy(
                            latestResponse = response,
                            proposals = if (proposal == null) state.proposals else {
                                listOf(proposal) + state.proposals.filterNot { it.id == proposal.id }
                            },
                            selectedOptionKeys = if (proposal == null) state.selectedOptionKeys else {
                                state.selectedOptionKeys + (
                                    proposal.id to proposal.options.firstOrNull()?.key.orEmpty()
                                    )
                            },
                            isAnalyzing = false,
                        )
                    }
                },
                onFailure = { error ->
                    error.rethrowCancellation()
                    _uiState.update {
                        it.copy(
                            isAnalyzing = false,
                            errorMessage = error.userFacingMessage("현재 상황을 분석하지 못했어요."),
                        )
                    }
                },
            )
        }
    }

    fun selectOption(proposalId: String, optionKey: String) {
        _uiState.update { it.copy(selectedOptionKeys = it.selectedOptionKeys + (proposalId to optionKey)) }
    }

    fun decide(proposal: AgentChangeProposal, approve: Boolean) {
        val id = tripId ?: return
        val revision = proposal.baseRevisionNo
        val selected = _uiState.value.selectedOptionKeys[proposal.id]
        if (revision == null || approve && selected.isNullOrBlank()) {
            _uiState.update { it.copy(errorMessage = "변경안을 새로고침한 뒤 다시 선택해 주세요.") }
            return
        }
        _uiState.update { it.copy(decidingProposalId = proposal.id, errorMessage = null) }
        viewModelScope.launch(ioDispatcher) {
            runCatching {
                gateway.decideChangeProposal(id, proposal.id, approve, selected, revision)
            }.fold(
                onSuccess = { decided ->
                    _uiState.update {
                        it.copy(
                            proposals = it.proposals.map { proposal ->
                                if (proposal.id == decided.id) decided else proposal
                            },
                            decidingProposalId = null,
                        )
                    }
                },
                onFailure = { error ->
                    error.rethrowCancellation()
                    _uiState.update {
                        it.copy(
                            decidingProposalId = null,
                            errorMessage = error.userFacingMessage("변경안을 처리하지 못했어요."),
                        )
                    }
                },
            )
        }
    }

    fun dismissError() = _uiState.update { it.copy(errorMessage = null) }

    companion object {
        fun factory(tripId: String?, gateway: AgentGateway): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    AgentViewModel(tripId, gateway) as T
            }
    }
}

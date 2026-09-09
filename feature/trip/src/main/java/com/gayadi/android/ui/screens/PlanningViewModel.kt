package com.gayadi.android.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.gayadi.android.domain.repository.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PlanningUiState(
    val busy: Boolean = false,
    val error: String? = null,
    val routes: List<RecommendedRoute> = emptyList(),
    val selected: RecommendedRoute? = null,
    val plan: GeneratedPlan? = null,
)

class PlanningViewModel(private val gateway: PlanningGateway, private val tripId: String,
    private val type: PlanningRouteType) : ViewModel() {
    private val mutable = MutableStateFlow(PlanningUiState())
    val state = mutable.asStateFlow()
    init { reload() }
    fun reload() = execute {
        val selected = gateway.selectedRoutes(tripId).firstOrNull { it.type == type }
        val plan = gateway.getPlan(tripId)
        mutable.update { it.copy(selected=selected, plan=plan, routes=listOfNotNull(selected)) }
    }
    fun recommend() = execute {
        val routes = gateway.recommend(tripId, type)
        val selected = gateway.selectedRoutes(tripId).firstOrNull { it.type == type }
        mutable.update { it.copy(routes=routes, selected=selected) }
    }
    fun select(route: RecommendedRoute) = execute {
        val saved = gateway.select(tripId, route)
        mutable.update { it.copy(selected=saved) }
    }
    fun clearSelection() = execute {
        gateway.clearSelection(tripId, type)
        mutable.update { it.copy(selected=null) }
    }
    fun generatePlan() = execute {
        val generated = gateway.generatePlan(tripId)
        // A new plan can invalidate route revisions; re-read selections from the server.
        mutable.update { it.copy(plan=generated, routes=emptyList(), selected=null) }
        val selected = gateway.selectedRoutes(tripId).firstOrNull { it.type == type }
        mutable.update { it.copy(selected=selected, routes=listOfNotNull(selected)) }
    }
    private fun execute(block: suspend () -> Unit) {
        if (mutable.value.busy) return
        mutable.update { it.copy(busy=true, error=null) }
        viewModelScope.launch {
            try { block() }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) { mutable.update { it.copy(error=error.message ?: "요청을 처리하지 못했어요. 다시 시도해 주세요.") } }
            finally { mutable.update { it.copy(busy=false) } }
        }
    }
    companion object {
        fun factory(gateway: PlanningGateway, tripId: String, type: PlanningRouteType) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = PlanningViewModel(gateway,tripId,type) as T
        }
    }
}

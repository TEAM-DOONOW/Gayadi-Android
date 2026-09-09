package com.gayadi.android.ui.screens

import com.gayadi.android.domain.repository.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class PlanningViewModelTest {
    private val dispatcher=StandardTestDispatcher()
    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun teardown() { Dispatchers.resetMain() }
    private val route=RecommendedRoute("3","balanced","균형",PlanningRouteType.ITINERARY,20,1000,0,"추천",listOf("A","B"),true)
    @Test fun failedSelectionKeepsServerSelectionAndRetryDoesNotGenerate() = runTest(dispatcher) {
        var recommendations=0
        val gateway=fake { name -> when(name) {
            "selectedRoutes" -> listOf(route)
            "getPlan" -> null
            "select" -> throw IllegalStateException("저장 실패")
            "recommend" -> {recommendations++;listOf(route)}
            else -> error(name)
        } }
        val vm=PlanningViewModel(gateway,"1",PlanningRouteType.ITINERARY)
        advanceUntilIdle()
        vm.select(route.copy(id="4"));advanceUntilIdle()
        assertEquals("3",vm.state.value.selected?.id)
        assertEquals("저장 실패",vm.state.value.error)
        vm.reload();advanceUntilIdle()
        assertEquals(0,recommendations)
    }
    @Test fun duplicateGenerationIsIgnoredAndFailedGenerationKeepsPlan() = runTest(dispatcher) {
        var calls=0
        val existing=GeneratedPlan(listOf(GeneratedPlanDay("2026-10-10","기존",emptyList())))
        val gateway=fake { name -> when(name) {
            "selectedRoutes" -> emptyList<RecommendedRoute>()
            "getPlan" -> existing
            "generatePlan" -> {calls++;throw IllegalStateException("생성 실패")}
            else -> error(name)
        } }
        val vm=PlanningViewModel(gateway,"1",PlanningRouteType.ITINERARY);advanceUntilIdle()
        vm.generatePlan();vm.generatePlan();advanceUntilIdle()
        assertEquals(1,calls);assertEquals(existing,vm.state.value.plan);assertFalse(vm.state.value.busy)
    }
    private fun fake(block:(String)->Any?):PlanningGateway = java.lang.reflect.Proxy.newProxyInstance(
        PlanningGateway::class.java.classLoader,arrayOf(PlanningGateway::class.java),
    ) { _,method,_->block(method.name) } as PlanningGateway
}

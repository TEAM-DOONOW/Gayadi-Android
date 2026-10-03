package com.gayadi.android.ui.screens

import com.gayadi.android.domain.model.RankingItem
import com.gayadi.android.domain.model.RankingList
import com.gayadi.android.domain.model.RankingType
import com.gayadi.android.domain.repository.RankingGateway
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeRankingViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    @Before fun setUp() { Dispatchers.setMain(dispatcher) }
    @After fun tearDown() { Dispatchers.resetMain() }

    @Test fun `loads seoul attractions first and caches each category`() = runTest(dispatcher) {
        val gateway = FakeRankingGateway()
        val viewModel = HomeRankingViewModel(gateway, dispatcher)
        advanceUntilIdle()
        assertEquals(RankingType.ATTRACTION to "서울", gateway.requests.single())
        assertEquals("ATTRACTION 1", viewModel.uiState.value.rankings?.items?.single()?.title)

        val festivals = homeRankingCategories.first { it.type == RankingType.FESTIVAL }
        viewModel.select(festivals)
        advanceUntilIdle()
        viewModel.select(homeRankingCategories.first())
        advanceUntilIdle()
        viewModel.select(festivals)
        advanceUntilIdle()

        assertEquals(listOf(RankingType.ATTRACTION to "서울", RankingType.FESTIVAL to ""), gateway.requests)
        assertEquals("FESTIVAL 1", viewModel.uiState.value.rankings?.items?.single()?.title)
    }

    @Test fun `failure shows retryable error and retry reloads`() = runTest(dispatcher) {
        val gateway = FakeRankingGateway(failuresLeft = 1)
        val viewModel = HomeRankingViewModel(gateway, dispatcher)
        advanceUntilIdle()
        assertNotNull(viewModel.uiState.value.errorMessage)

        viewModel.retry()
        advanceUntilIdle()
        assertNull(viewModel.uiState.value.errorMessage)
        assertEquals(1, viewModel.uiState.value.rankings?.items?.size)
    }

    private class FakeRankingGateway(var failuresLeft: Int = 0) : RankingGateway {
        val requests = mutableListOf<Pair<RankingType, String>>()
        override suspend fun getRankings(type: RankingType, region: String, limit: Int): RankingList {
            requests += type to region
            if (failuresLeft-- > 0) throw IOException("offline")
            return RankingList(type, region, "2026-07", true, listOf(RankingItem(1, "${type.name} 1")))
        }
    }
}

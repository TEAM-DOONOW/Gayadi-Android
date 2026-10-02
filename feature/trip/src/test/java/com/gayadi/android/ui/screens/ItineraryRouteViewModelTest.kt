package com.gayadi.android.ui.screens

import com.gayadi.android.domain.model.RouteTransportMode
import com.gayadi.android.domain.repository.RecommendedItinerary
import com.gayadi.android.domain.repository.RecommendedItineraryStop
import com.gayadi.android.domain.repository.TripSupportGateway
import java.io.IOException
import java.lang.reflect.Proxy
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
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ItineraryRouteViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `failed condition change cannot leave a stale route applicable`() = runTest(dispatcher) {
        var recommendationCalls = 0
        val viewModel = ItineraryRouteViewModel(
            gateway = gateway(
                recommend = {
                    recommendationCalls++
                    if (recommendationCalls == 1) route() else throw IOException("연결 실패")
                },
            ),
            tripId = "42",
            date = "2026.10.03",
            ioDispatcher = dispatcher,
        )

        viewModel.recommend()
        advanceUntilIdle()
        assertNotNull(viewModel.uiState.value.recommendation)

        viewModel.selectTransportMode(RouteTransportMode.CAR)
        advanceUntilIdle()

        assertEquals(RouteTransportMode.CAR, viewModel.uiState.value.transportMode)
        assertNull(viewModel.uiState.value.recommendation)
        assertNotNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `apply failure keeps the reviewed route visible`() = runTest(dispatcher) {
        val reviewed = route()
        val viewModel = ItineraryRouteViewModel(
            gateway = gateway(
                recommend = { reviewed },
                apply = { throw IOException("적용 실패") },
            ),
            tripId = "42",
            date = "2026.10.03",
            ioDispatcher = dispatcher,
        )
        viewModel.recommend()
        advanceUntilIdle()

        viewModel.apply {}
        advanceUntilIdle()

        assertSame(reviewed, viewModel.uiState.value.recommendation)
        assertNull(viewModel.uiState.value.errorMessage)
        assertNotNull(viewModel.uiState.value.applyErrorMessage)
    }

    @Test
    fun `custom time range refreshes the route and invalid range is not requested`() = runTest(dispatcher) {
        val requestedRanges = mutableListOf<Pair<String, String>>()
        val viewModel = ItineraryRouteViewModel(
            gateway = gateway(
                recommend = { route() },
                onRecommend = { start, end -> requestedRanges += start to end },
            ),
            tripId = "42",
            date = "2026.10.03",
            ioDispatcher = dispatcher,
        )

        viewModel.selectTimeRange("09:30", "17:45")
        advanceUntilIdle()

        assertEquals(listOf("09:30" to "17:45"), requestedRanges)
        assertEquals("09:30", viewModel.uiState.value.startTime)
        assertEquals("17:45", viewModel.uiState.value.endTime)
        assertNotNull(viewModel.uiState.value.recommendation)

        viewModel.selectTimeRange("17:00", "19:00")
        advanceUntilIdle()

        assertEquals(1, requestedRanges.size)
        assertNull(viewModel.uiState.value.recommendation)
        assertEquals(
            "종료 시간은 시작 시간보다 3~12시간 뒤로 골라 주세요.",
            viewModel.uiState.value.errorMessage,
        )
    }

    private fun gateway(
        recommend: () -> RecommendedItinerary,
        apply: () -> RecommendedItinerary = recommend,
        onRecommend: (String, String) -> Unit = { _, _ -> },
    ): TripSupportGateway = Proxy.newProxyInstance(
        TripSupportGateway::class.java.classLoader,
        arrayOf(TripSupportGateway::class.java),
    ) { _, method, args ->
        when (method.name) {
            "recommendItinerary" -> {
                val callArgs = requireNotNull(args)
                onRecommend(callArgs[2] as String, callArgs[3] as String)
                recommend()
            }
            "applyItinerary" -> apply()
            else -> error("Unexpected gateway call: ${method.name}")
        }
    } as TripSupportGateway

    private fun route() = RecommendedItinerary(
        date = "2026.10.03",
        startTime = "10:00",
        endTime = "18:00",
        transportMode = RouteTransportMode.PUBLIC_TRANSIT,
        variation = 0,
        estimated = true,
        totalTravelMinutes = 18,
        totalStayMinutes = 140,
        summary = "동선을 맞췄어요.",
        stops = listOf(
            RecommendedItineraryStop(
                order = 1,
                placeId = "1",
                name = "경복궁",
                category = "관광명소",
                imageUrl = "",
                latitude = 37.5796,
                longitude = 126.9770,
                arrivalTime = "10:00",
                departureTime = "11:20",
                stayMinutes = 80,
                travelMinutesFromPrevious = 0,
                distanceMetersFromPrevious = 0,
            ),
            RecommendedItineraryStop(
                order = 2,
                placeId = "2",
                name = "광화문",
                category = "관광명소",
                imageUrl = "",
                latitude = 37.5716,
                longitude = 126.9769,
                arrivalTime = "11:38",
                departureTime = "12:38",
                stayMinutes = 60,
                travelMinutesFromPrevious = 18,
                distanceMetersFromPrevious = 1_800,
            ),
        ),
    )
}

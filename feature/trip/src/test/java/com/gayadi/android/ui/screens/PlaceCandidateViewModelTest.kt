package com.gayadi.android.ui.screens

import com.gayadi.android.domain.model.*
import com.gayadi.android.domain.repository.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class PlaceCandidateViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    @Before fun setUp() { Dispatchers.setMain(dispatcher) }
    @After fun tearDown() { Dispatchers.resetMain() }

    @Test fun `append and middle insertion use same day main visits only`() {
        val visits = listOf(schedule("a",0),schedule("b",1),schedule("other",2).copy(date="2026.09.23"),schedule("alt",3).copy(type=ScheduleType.ALTERNATIVE))
        val append = candidateSearchContext("1","2026.09.22","1",visits,null)
        assertEquals("b",append.previous?.id)
        assertNull(append.next)
        val middle = candidateSearchContext("1","2026.09.22","1",visits,"b")
        assertEquals("a",middle.previous?.id)
        assertEquals("b",middle.next?.id)
        val first = candidateSearchContext("1","2026.09.22","1",visits,"a")
        assertNull(first.previous)
    }

    @Test fun `mode and filters sent to server and returned order kept without local filtering`() = runTest(dispatcher) {
        val calls = mutableListOf<PlaceCandidateQuery>()
        val vm = PlaceCandidateViewModel(PlaceCandidateGateway { calls += it; page(listOf("9","2")) }, ::place)
        vm.configure(CandidateSearchContext("1","date","1",schedule("a",0),schedule("b",1)))
        runCurrent()
        vm.selectTransportMode(RouteTransportMode.CAR)
        runCurrent()
        vm.selectCategory("카페")
        vm.updateQuery("검색어")
        advanceUntilIdle()
        assertEquals(PlaceSort.TRAVEL_TIME,calls.last().sort)
        assertEquals(RouteTransportMode.CAR,calls.last().transportMode)
        assertEquals("CAFE",calls.last().category)
        assertNotNull(calls.last().origin)
        assertNotNull(calls.last().next)
        assertEquals(listOf("9","2"),vm.uiState.value.filteredPlaces.map { it.id })
        assertFalse(vm.uiState.value.hasNext)
    }

    @Test fun `clearing transport restores recent search without coordinates`() = runTest(dispatcher) {
        val calls = mutableListOf<PlaceCandidateQuery>()
        val vm = PlaceCandidateViewModel(PlaceCandidateGateway { calls += it; page().copy(sort=it.sort) }, ::place)
        vm.configure(CandidateSearchContext("1","date","1",schedule("a",0),null))
        runCurrent()
        assertEquals(PlaceSort.RECENT,calls.last().sort)
        vm.selectTransportMode(RouteTransportMode.CAR)
        runCurrent()
        assertEquals(PlaceSort.TRAVEL_TIME,calls.last().sort)
        vm.selectTransportMode(null)
        runCurrent()
        assertEquals(PlaceSort.RECENT,calls.last().sort)
        assertNull(calls.last().origin)
        assertNull(vm.uiState.value.transportMode)
    }

    @Test fun `first insertion omits next coordinate and displays fallback ranking`() = runTest(dispatcher) {
        var request: PlaceCandidateQuery? = null
        val vm = PlaceCandidateViewModel(PlaceCandidateGateway { request=it; page().copy(sort=PlaceSort.RECENT) }, ::place)
        vm.configure(CandidateSearchContext("1","date","1",null,schedule("b",1)))
        vm.selectTransportMode(RouteTransportMode.PUBLIC_TRANSIT)
        advanceUntilIdle()
        assertNull(request!!.origin)
        assertNull(request!!.next)
        assertEquals(PlaceSort.RECENT,vm.uiState.value.rankingSort)
    }

    @Test fun `server recent response with valid origin must not claim missing coordinates`() = runTest(dispatcher) {
        var request: PlaceCandidateQuery? = null
        val vm = PlaceCandidateViewModel(PlaceCandidateGateway { request=it; page().copy(sort=PlaceSort.RECENT) }, ::place)
        vm.configure(CandidateSearchContext("1","date","1",schedule("a",0),null))
        vm.selectTransportMode(RouteTransportMode.CAR)
        advanceUntilIdle()
        assertNotNull(request!!.origin)
        assertEquals(true,vm.uiState.value.originAvailable)
        assertFalse(vm.uiState.value.recentRankingNotice().contains("위치가 없어"))
    }

    @Test fun `late search cannot replace changed query and errors clear ranked results`() = runTest(dispatcher) {
        val first = CompletableDeferred<PlaceCandidatePage>()
        var fail = false
        val vm = PlaceCandidateViewModel(PlaceCandidateGateway {
            if (it.query.isEmpty()) withContext(NonCancellable) { first.await() }
            else if (fail) throw java.io.IOException("offline") else page(listOf("new"))
        }, ::place)
        vm.configure(CandidateSearchContext("1","date","1",null,null))
        runCurrent()
        vm.updateQuery("new")
        advanceTimeBy(301); runCurrent()
        first.complete(page(listOf("old")))
        runCurrent()
        assertEquals(listOf("new"),vm.uiState.value.places.map { it.id })
        fail=true
        vm.retry(); runCurrent()
        assertTrue(vm.uiState.value.places.isEmpty())
        assertNotNull(vm.uiState.value.errorMessage)
    }

    @Test fun `insertion keeps chosen location and order between neighbours`() {
        val inserted = schedule("candidate",8).copy(latitude=36.0,longitude=128.0)
        val result = insertPlaceSchedule(listOf(schedule("a",0),schedule("b",1)),inserted,"b")
        assertEquals(listOf("a","candidate","b"),result.map { it.id })
        assertEquals(listOf(0,1,2),result.map { it.order })
        assertEquals(36.0,result[1].latitude!!,0.0)
    }

    @Test fun `estimate and negative extra minutes remain distinct from provider durations`() {
        val time = PlaceTravelTime(RouteTransportMode.CAR,10,7,-5,"KAKAO_DIRECTIONS",false)
        assertEquals("자동차 약 10분",time.displayLabel())
        assertEquals("자동차 추정 약 10분",time.copy(fallback=true).displayLabel())
        assertEquals("자동차 추정 약 10분",time.copy(configuredProvider="LOCAL_ESTIMATE").displayLabel())
        assertEquals("도보 추정 약 10분",time.copy(transportMode=RouteTransportMode.WALK).displayLabel())
        assertEquals("자전거 추정 약 10분",time.copy(transportMode=RouteTransportMode.BICYCLE).displayLabel())
        assertEquals(-5,time.additionalDurationMinutes)
    }

    private fun schedule(id:String,order:Int) = TravelSchedule(id,"1",id,id,"2026.09.22","10:00",order=order,latitude=37.0+order/10.0,longitude=127.0)
    private fun place(id:String) = TourPlace(id,id,"","","",127.0,37.0)
    private fun page(ids:List<String> = emptyList()) = PlaceCandidatePage(ids.map { PlaceCandidate(place(it),"CAFE",null) },PlaceSort.TRAVEL_TIME,20,true,null,false)
}

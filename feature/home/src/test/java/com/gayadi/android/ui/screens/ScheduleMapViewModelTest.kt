package com.gayadi.android.ui.screens

import com.gayadi.android.domain.model.TourPlace
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ScheduleMapViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    @Before fun setUp() { Dispatchers.setMain(dispatcher) }
    @After fun tearDown() { Dispatchers.resetMain() }

    @Test fun `loads exact place ids only once and omits deleted schedules`() = runTest(dispatcher) {
        val calls = mutableListOf<String>()
        val vm = ScheduleMapViewModel { id -> calls += id; place(id) }
        vm.load(listOf("1", "2", "1"))
        runCurrent()
        assertEquals(listOf("1", "2"), calls)
        assertEquals(setOf("1", "2"), vm.uiState.value.coordinates.keys)
        vm.load(listOf("2"))
        runCurrent()
        assertEquals(setOf("2"), vm.uiState.value.coordinates.keys)
        assertEquals(2, calls.size)
        assertEquals(setOf("2"),vm.uiState.value.places.keys)
        assertEquals("image-2",vm.uiState.value.places["2"]?.imageUrl)
    }

    @Test fun `partial failure preserves valid markers and retry resolves missing coordinates`() = runTest(dispatcher) {
        var fail = true
        val vm = ScheduleMapViewModel { id ->
            if (id == "2" && fail) throw java.io.IOException("offline")
            place(id)
        }
        vm.load(listOf("1", "2"))
        runCurrent()
        assertEquals(setOf("1"), vm.uiState.value.coordinates.keys)
        assertNotNull(vm.uiState.value.errorMessage)
        fail = false
        vm.load(listOf("1", "2"), retry = true)
        runCurrent()
        assertEquals(setOf("1", "2"), vm.uiState.value.coordinates.keys)
        assertNull(vm.uiState.value.errorMessage)
    }

    @Test fun `mismatched id and missing coordinates never produce a marker`() = runTest(dispatcher) {
        val vm = ScheduleMapViewModel { id -> if (id == "1") place("different") else place(id).copy(latitude = null) }
        vm.load(listOf("1", "2"))
        runCurrent()
        assertTrue(vm.uiState.value.coordinates.isEmpty())
        assertFalse(vm.uiState.value.places.containsKey("1"))
        assertEquals("image-2",vm.uiState.value.places["2"]?.imageUrl)
        assertNotNull(vm.uiState.value.errorMessage)
    }

    private fun place(id: String) = TourPlace(id, "같은 이름", "", "", "image-$id", 127.0, 37.0)
}

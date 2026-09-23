package com.gayadi.android.ui.screens

import android.graphics.Bitmap
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.gayadi.android.domain.model.RouteTransportMode
import com.gayadi.android.domain.repository.PlaceSort
import com.gayadi.android.domain.repository.PlaceTravelTime
import com.gayadi.android.ui.theme.GayadiTheme
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class TravelTimePlaceSearchTest {
    @get:Rule val compose = createComposeRule()

    @Test fun transportChoiceReachesCallback() {
        var mode: RouteTransportMode? = null
        var before: String? = null
        compose.setContent {
            var state by remember { mutableStateOf(PlaceUiState(isLoading = false)) }
            GayadiTheme {
                Box(Modifier.padding(top=100.dp)) {
                PlaceSearchControls(state,
                    onTransportModeSelected = { mode=it; state=state.copy(transportMode=it) })
                }
            }
        }
        compose.onNodeWithText("이동시간순").assertDoesNotExist()
        compose.onNodeWithText("자동차").performClick()
        assertEquals(RouteTransportMode.CAR,mode)
        compose.onNodeWithText("자동차").performClick()
        assertEquals(null, mode)
        compose.onNodeWithText("도보").performClick()
        assertEquals(RouteTransportMode.WALK,mode)
        compose.onNodeWithText("자전거").performClick()
        assertEquals(RouteTransportMode.BICYCLE,mode)
        compose.onNodeWithText("자전거").performClick()
        assertEquals(null,mode)
        compose.onNodeWithText("추가 위치: 같은 날짜 일정 끝").assertDoesNotExist()
    }

    @Test fun displaysProviderAndEstimatedMinutesWithoutChangingServerOrder() {
        render { searchState() }
        compose.onNodeWithContentDescription("자동차 약 10분").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("10분").assertIsDisplayed()
        compose.onNodeWithText("추가 이동시간 -5분").assertIsDisplayed()
        compose.onNodeWithContentDescription("대중교통 추정 약 15분").assertIsDisplayed()
        compose.onNodeWithText("15분").performClick()
        compose.onNodeWithText("실제 교통 조회 결과가 아닌 추정 시간이에요.").assertIsDisplayed()
        compose.onNodeWithText("확인").performClick()
        val first = compose.onNodeWithText("첫 번째 후보").fetchSemanticsNode().boundsInRoot
        val second = compose.onNodeWithText("두 번째 후보").fetchSemanticsNode().boundsInRoot
        org.junit.Assert.assertTrue(first.left < second.left)
        compose.onNodeWithText("더 보기").assertDoesNotExist()
    }

    @Test fun recentFallbackAndFailureAreExplicit() {
        val state = mutableStateOf(searchState().copy(rankingSort=PlaceSort.RECENT,limited=false,originAvailable=false))
        render { state.value }
        compose.onNodeWithText("이전 장소 위치가 없어 지역 내 최신순으로 표시해요.").assertIsDisplayed()
        compose.runOnIdle { state.value=state.value.copy(places=emptyList(),errorMessage="장소를 불러오지 못했어요.") }
        compose.onNodeWithText("다시 시도").performScrollTo().assertIsDisplayed()
        compose.onNodeWithContentDescription("자동차 약 10분").assertDoesNotExist()
    }

    @Test fun filtersOnlyAppearAfterOpeningIcon() {
        render { searchState() }
        compose.onNodeWithText("자동차").assertDoesNotExist()
        compose.onNodeWithContentDescription("장소 필터").performClick()
        compose.onNodeWithText("자동차").assertIsDisplayed()
        compose.onNodeWithText("버스").assertIsDisplayed()
        compose.onNodeWithText("장소 보기").performScrollTo().performClick()
        compose.onNodeWithText("자동차").assertDoesNotExist()
    }

    @Test fun captureTransportButtonsAtSmallAndRegularWidths() {
        val width = mutableStateOf(320)
        compose.setContent {
            GayadiTheme {
                Box(Modifier.padding(top=100.dp)) {
                    Box(Modifier.width(width.value.dp).padding(20.dp).testTag("transport-buttons")) {
                        com.gayadi.android.ui.components.TransportModeSelector(RouteTransportMode.WALK) {}
                    }
                }
            }
        }
        listOf(320,400).forEach { value ->
            compose.runOnIdle { width.value=value }
            listOf("자동차","버스","도보","자전거").forEach { compose.onNodeWithText(it).assertIsDisplayed() }
            val bitmap=compose.onNodeWithTag("transport-buttons").captureToImage().asAndroidBitmap()
            val directory=InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null)
            File(directory,"transport-buttons-$value.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG,100,it) }
        }
    }

    @Test fun captureSmallAndRegularLayouts() {
        val width = mutableStateOf(320)
        render(width) { searchState() }
        listOf(320,400).forEach { value ->
            compose.runOnIdle { width.value=value }
            compose.waitForIdle()
            val bitmap=compose.onNodeWithTag("place-screen").captureToImage().asAndroidBitmap()
            val directory=InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null)
            File(directory,"travel-time-places-$value.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG,100,it) }
        }
    }

    private fun render(width: State<Int> = mutableStateOf(400), state: () -> PlaceUiState) {
        compose.setContent {
            GayadiTheme {
                Box(Modifier.padding(top=100.dp)) {
                    Box(Modifier.width(width.value.dp).height(680.dp).testTag("place-screen")) {
                        PlaceSearchScreen(state(), onBack={}, onQueryChange={}, onCategorySelected={}, onPlaceClick={}, onRetry={},
                            insertionOptions=listOf("a" to "관광지 A"))
                    }
                }
            }
        }
    }

    private fun searchState() = PlaceUiState(
        regionName="서울",isLoading=false,serverOrdered=true,sort=PlaceSort.TRAVEL_TIME,rankingSort=PlaceSort.TRAVEL_TIME,
        transportMode=RouteTransportMode.CAR,limited=true,
        places=listOf(
            item("9","첫 번째 후보",PlaceTravelTime(RouteTransportMode.CAR,10,7,-5,"KAKAO_DIRECTIONS",false)),
            item("2","두 번째 후보",PlaceTravelTime(RouteTransportMode.PUBLIC_TRANSIT,15,null,null,"LOCAL_ESTIMATE",true)),
        ),
    )
    private fun item(id:String,name:String,time:PlaceTravelTime) = PlaceItem(
        id,name,"카페",0.0,0,CrowdLevel.NORMAL,"☕","서울의 장소",hasRealtimeDetails=false,travelTime=time,
    )
}

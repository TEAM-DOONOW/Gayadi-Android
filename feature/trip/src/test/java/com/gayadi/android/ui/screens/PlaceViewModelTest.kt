package com.gayadi.android.ui.screens

import com.gayadi.android.domain.model.CongestionHourlyForecast
import com.gayadi.android.domain.model.CongestionHourlyPoint
import com.gayadi.android.domain.repository.CongestionRepository
import com.gayadi.android.domain.usecase.GetCongestionHourlyUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PlaceViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun queryAndCategoryFilterPlaces() {
        val viewModel = PlaceViewModel()
        viewModel.selectCategory("카페")
        viewModel.updateQuery("글렌코")
        assertEquals(listOf("place-2"), viewModel.uiState.value.filteredPlaces.map(PlaceItem::id))
    }

    @Test
    fun detailNearbyWeatherAndCrowdComeFromRepository() {
        val viewModel = PlaceViewModel()
        val place = viewModel.findPlace("place-3")!!
        assertEquals("섭지코지", place.name)
        assertEquals("바람", place.weather)
        assertEquals(CrowdLevel.CROWDED, place.crowdLevel)
        val nearby = viewModel.nearbyPlaces("place-3")
        assertTrue(nearby.none { it.id == "place-3" })
        assertEquals(nearby.sortedBy(PlaceItem::distanceMeters), nearby)
    }

    @Test
    fun hourlyCongestionUsesPlaceRegionCodesAndPublishesForecast() {
        val placeRepository = object : PlaceRepository {
            override suspend fun getPlaces(regionName: String): Result<List<PlaceItem>> = Result.success(
                listOf(
                    PlaceItem(
                        id = "palace-1",
                        name = "경복궁",
                        category = "관광명소",
                        rating = 4.8,
                        reviews = 100,
                        crowdLevel = CrowdLevel.NORMAL,
                        emoji = "🏯",
                        description = "서울 종로구",
                        regionCode = "11",
                        districtCode = "11110",
                    ),
                ),
            )
        }
        var requestedCodes: Pair<String, String>? = null
        val congestionRepository = object : CongestionRepository {
            override suspend fun getHourlyForecast(
                areaCode: String,
                districtCode: String,
                areaName: String,
                placeName: String,
                targetAt: String,
                hours: List<Int>?,
            ): Result<CongestionHourlyForecast> {
                requestedCodes = areaCode to districtCode
                return Result.success(
                    CongestionHourlyForecast(
                        placeName = placeName,
                        points = listOf(CongestionHourlyPoint(hour = 13, concentrationScore = 72, level = "혼잡")),
                    ),
                )
            }
        }
        val viewModel = PlaceViewModel(
            repository = placeRepository,
            getCongestionHourly = GetCongestionHourlyUseCase(congestionRepository),
        )

        viewModel.loadCongestionHourly("palace-1")

        assertEquals("11" to "11110", requestedCodes)
        assertEquals("경복궁", viewModel.hourlyUiState.value.forecast?.placeName)
        assertEquals(72, viewModel.hourlyUiState.value.forecast?.points?.single()?.concentrationScore)
    }

    @Test
    fun changingRegionLoadsTripSpecificPlacesAndResetsFilters() {
        val viewModel = PlaceViewModel()
        viewModel.selectCategory("카페")
        viewModel.updateQuery("글렌코")

        viewModel.setRegion("서울")

        assertEquals("서울", viewModel.uiState.value.regionName)
        assertEquals("", viewModel.uiState.value.query)
        assertEquals("전체", viewModel.uiState.value.selectedCategory)
        assertEquals(
            listOf("광장시장", "서울숲 카페거리", "경복궁", "서울역 스테이"),
            viewModel.uiState.value.places.map(PlaceItem::name),
        )
        assertTrue(viewModel.uiState.value.places.none { it.description.contains("제주") })
    }

    @Test
    fun changingRegionKeepsPreviouslyLoadedFavoriteDetailsAddressable() {
        val viewModel = PlaceViewModel()
        viewModel.setRegion("서울")
        viewModel.setRegion("부산")

        assertEquals("광장시장", viewModel.findPlace("seoul-place-1")?.name)
        assertTrue(viewModel.uiState.value.places.all { it.description.contains("부산") })
    }

    @Test
    fun sameRegionReloadsWhenPreviousLoadWasEmpty() {
        val repository = object : PlaceRepository {
            var loads = 0
            override suspend fun getPlaces(regionName: String): Result<List<PlaceItem>> {
                loads += 1
                return if (loads == 1) {
                    Result.success(emptyList())
                } else {
                    Result.success(
                        listOf(
                            PlaceItem(
                                id = "place-reload",
                                name = "재조회 장소",
                                category = "관광명소",
                                rating = 4.0,
                                reviews = 1,
                                crowdLevel = CrowdLevel.RELAXED,
                                emoji = "🏞️",
                                description = "로그인 후 다시 불러온 장소",
                            ),
                        ),
                    )
                }
            }
        }
        val viewModel = PlaceViewModel(repository)
        assertTrue(viewModel.uiState.value.places.isEmpty())

        viewModel.setRegion("제주 성산")

        assertEquals(2, repository.loads)
        assertEquals(listOf("재조회 장소"), viewModel.uiState.value.places.map(PlaceItem::name))
    }
}

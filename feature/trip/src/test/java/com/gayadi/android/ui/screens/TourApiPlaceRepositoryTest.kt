package com.gayadi.android.ui.screens

import com.gayadi.android.domain.model.TourPlace
import com.gayadi.android.domain.repository.TourRepository
import com.gayadi.android.domain.usecase.GetTourPlacesUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class TourApiPlaceRepositoryTest {
    @Test
    fun requestsBoundedQueriesInOrderAndMapsAllCategories() = runTest {
        val source = RecordingTourRepository { query ->
            Result.success(
                when {
                    query.contentTypeId == 12 -> listOf(tourPlace("attraction", "성산일출봉", ""))
                    query.lclsSystm2 == "FD01" -> emptyList()
                    query.lclsSystm2 == "FD05" -> listOf(tourPlace("cafe", "성산다원", ""))
                    query.contentTypeId == 39 -> listOf(
                        tourPlace("restaurant", "성산식당", ""),
                    )
                    query.contentTypeId == 32 -> listOf(tourPlace("stay", "성산스테이", ""))
                    else -> emptyList()
                },
            )
        }
        val repository = TourApiPlaceRepository(GetTourPlacesUseCase(source))

        val places = repository.getPlaces().getOrThrow()

        assertEquals(
            listOf(
                RecordedTourQuery(contentTypeId = 12),
                RecordedTourQuery(contentTypeId = 39),
                RecordedTourQuery(contentTypeId = 39, lclsSystm1 = "FD", lclsSystm2 = "FD01"),
                RecordedTourQuery(contentTypeId = 39, lclsSystm1 = "FD", lclsSystm2 = "FD05"),
                RecordedTourQuery(contentTypeId = 32),
            ),
            source.requests,
        )
        assertEquals(
            mapOf(
                "attraction" to ("관광명소" to "🏞️"),
                "restaurant" to ("맛집" to "🍲"),
                "cafe" to ("카페" to "☕"),
                "stay" to ("숙소" to "🏨"),
            ),
            places.associate { it.id to (it.category to it.emoji) },
        )
    }

    @Test
    fun exactQueriesApplyCafeThenRestaurantPrecedenceToDuplicateContentIds() = runTest {
        val source = RecordingTourRepository { query ->
            Result.success(
                when {
                    query.lclsSystm2 == "FD05" -> listOf(
                        tourPlace("shared", "전용 조회 카페", contentTypeId = ""),
                    )
                    query.lclsSystm2 == "FD01" -> listOf(
                        tourPlace("shared", "전용 조회 맛집", contentTypeId = ""),
                        tourPlace("restaurant-priority", "전용 조회 맛집 2", contentTypeId = ""),
                    )
                    query.contentTypeId == 39 -> listOf(
                        tourPlace("shared", "일반 조회 식당", "39", lclsSystm2 = "FD01"),
                        tourPlace("restaurant-priority", "일반 조회 카페", "39", lclsSystm2 = "FD05"),
                    )
                    query.contentTypeId == 32 -> listOf(
                        tourPlace("shared", "동일 ID 숙소", "32"),
                    )
                    else -> emptyList()
                },
            )
        }
        val repository = TourApiPlaceRepository(GetTourPlacesUseCase(source))

        val places = repository.getPlaces().getOrThrow()
        val sharedPlaces = places.filter { it.id == "shared" }

        assertEquals(1, sharedPlaces.size)
        assertEquals("전용 조회 카페", sharedPlaces.single().name)
        assertEquals("카페", sharedPlaces.single().category)
        assertEquals("☕", sharedPlaces.single().emoji)

        val exactRestaurant = places.single { it.id == "restaurant-priority" }
        assertEquals("전용 조회 맛집 2", exactRestaurant.name)
        assertEquals("맛집", exactRestaurant.category)
        assertEquals("🍲", exactRestaurant.emoji)
    }

    @Test
    fun exactRestaurantQueryKeepsRestaurantsNonEmptyWhenGenericPageContainsOnlyCafes() = runTest {
        val source = RecordingTourRepository { query ->
            Result.success(
                when {
                    query.lclsSystm2 == "FD01" -> listOf(
                        tourPlace("exact-restaurant", "전용 조회 맛집", contentTypeId = ""),
                    )
                    query.lclsSystm2 == "FD05" -> emptyList()
                    query.contentTypeId == 39 -> listOf(
                        tourPlace("generic-cafe-1", "첫 카페", "", lclsSystm2 = "FD05"),
                        tourPlace("generic-cafe-2", "두 번째 카페", "", lclsSystm3 = "FD050100"),
                    )
                    else -> emptyList()
                },
            )
        }
        val repository = TourApiPlaceRepository(GetTourPlacesUseCase(source))

        val places = repository.getPlaces().getOrThrow()

        assertEquals(2, places.count { it.category == "카페" })
        assertEquals(listOf("exact-restaurant"), places.filter { it.category == "맛집" }.map(PlaceItem::id))
    }

    @Test
    fun usesTitleFallbackOnlyWhenStructuredFoodClassificationIsMissing() = runTest {
        val source = RecordingTourRepository { query ->
            Result.success(
                if (query.contentTypeId == 39 && query.lclsSystm1 == null) {
                    listOf(
                        tourPlace("fallback-cafe", "바다 로스터리", "39"),
                        tourPlace(
                            "structured-restaurant",
                            "카페라는 이름의 식당",
                            "39",
                            lclsSystm2 = "FD01",
                        ),
                    )
                } else {
                    emptyList()
                },
            )
        }
        val repository = TourApiPlaceRepository(GetTourPlacesUseCase(source))

        val categories = repository.getPlaces().getOrThrow().associate { it.id to it.category }

        assertEquals("카페", categories["fallback-cafe"])
        assertEquals("맛집", categories["structured-restaurant"])
    }

    @Test
    fun mapsAnUnsupportedContentTypeToTheTouristAttractionFallback() = runTest {
        val source = RecordingTourRepository { query ->
            Result.success(
                if (query.contentTypeId == 12) {
                    listOf(tourPlace("unknown", "지원 전 콘텐츠", "14"))
                } else {
                    emptyList()
                },
            )
        }
        val repository = TourApiPlaceRepository(GetTourPlacesUseCase(source))

        val place = repository.getPlaces().getOrThrow().single()

        assertEquals("관광명소", place.category)
        assertEquals("🏞️", place.emoji)
    }

    @Test
    fun stopsAtFirstFailureAndReturnsTheOriginalError() = runTest {
        val expectedError = IllegalStateException("음식점 조회 실패")
        val source = RecordingTourRepository { query ->
            if (query.contentTypeId == 39) Result.failure(expectedError) else Result.success(emptyList())
        }
        val repository = TourApiPlaceRepository(GetTourPlacesUseCase(source))

        val result = repository.getPlaces()

        assertSame(expectedError, result.exceptionOrNull())
        assertEquals(
            listOf(RecordedTourQuery(contentTypeId = 12), RecordedTourQuery(contentTypeId = 39)),
            source.requests,
        )
    }

    @Test
    fun rethrowsCancellationFailure() = runTest {
        val expected = CancellationException("장소 조회 취소")
        val source = RecordingTourRepository { query ->
            if (query.contentTypeId == 39) Result.failure(expected) else Result.success(emptyList())
        }
        val repository = TourApiPlaceRepository(GetTourPlacesUseCase(source))
        var caught: CancellationException? = null

        try {
            repository.getPlaces()
        } catch (cancellation: CancellationException) {
            caught = cancellation
        }

        assertSame(expected, caught)
        assertEquals(
            listOf(RecordedTourQuery(contentTypeId = 12), RecordedTourQuery(contentTypeId = 39)),
            source.requests,
        )
    }
}

private data class RecordedTourQuery(
    val contentTypeId: Int,
    val numOfRows: Int = 100,
    val lclsSystm1: String? = null,
    val lclsSystm2: String? = null,
    val lclsSystm3: String? = null,
    val maxPages: Int? = 1,
)

private class RecordingTourRepository(
    private val resultForQuery: (RecordedTourQuery) -> Result<List<TourPlace>>,
) : TourRepository {
    val requests = mutableListOf<RecordedTourQuery>()

    override suspend fun getPlaces(
        numOfRows: Int,
        contentTypeId: Int,
        lclsSystm1: String?,
        lclsSystm2: String?,
        lclsSystm3: String?,
        maxPages: Int?,
    ): Result<List<TourPlace>> {
        val query = RecordedTourQuery(
            contentTypeId = contentTypeId,
            numOfRows = numOfRows,
            lclsSystm1 = lclsSystm1,
            lclsSystm2 = lclsSystm2,
            lclsSystm3 = lclsSystm3,
            maxPages = maxPages,
        )
        requests += query
        return resultForQuery(query)
    }
}

private fun tourPlace(
    contentId: String,
    title: String,
    contentTypeId: String,
    lclsSystm2: String = "",
    lclsSystm3: String = "",
) = TourPlace(
    contentId = contentId,
    title = title,
    address = "제주",
    addressDetail = "성산",
    imageUrl = "",
    longitude = null,
    latitude = null,
    contentTypeId = contentTypeId,
    lclsSystm2 = lclsSystm2,
    lclsSystm3 = lclsSystm3,
)

package com.gayadi.android.data

import com.gayadi.android.data.datasource.HttpTourApiDataSource
import com.gayadi.android.data.datasource.TourApiDataSource
import com.gayadi.android.data.model.TourPlaceDto
import com.gayadi.android.data.repository.DefaultTourRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class TourApiTest {
    @Test
    fun parsesTourAreaResponse() {
        val places = HttpTourApiDataSource("http://example.com").parsePlaces(
            """
            {
              "items": [{
                "contentId": "2783012",
                "title": "영산포철도공원",
                "address": "전남광주통합특별시 나주시 삼영동",
                "addressDetail": "174-1",
                "firstImage": "https://example.com/place.jpg",
                "mapX": "126.7075",
                "mapY": "35.0052"
              }]
            }
            """.trimIndent(),
        )

        assertEquals(1, places.size)
        assertEquals("2783012", places.single().contentId)
        assertEquals("영산포철도공원", places.single().title)
    }

    @Test
    fun reusesCachedResponseForSameRequest() = runTest {
        val dataSource = CountingTourApiDataSource()
        val repository = DefaultTourRepository(dataSource)

        repository.getPlaces(numOfRows = 5, contentTypeId = 12).getOrThrow()
        repository.getPlaces(numOfRows = 5, contentTypeId = 12).getOrThrow()

        assertEquals(1, dataSource.requestCount)
    }
}

private class CountingTourApiDataSource : TourApiDataSource {
    var requestCount = 0

    override suspend fun getPlaces(numOfRows: Int, contentTypeId: Int): List<TourPlaceDto> {
        requestCount += 1
        return listOf(
            TourPlaceDto(
                contentId = "1",
                title = "테스트 관광지",
                address = "서울",
                addressDetail = "",
                firstImage = "",
                mapX = "126.0",
                mapY = "37.0",
            ),
        )
    }
}

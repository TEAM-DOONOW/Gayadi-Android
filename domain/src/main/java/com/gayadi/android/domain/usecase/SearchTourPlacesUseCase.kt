package com.gayadi.android.domain.usecase

import com.gayadi.android.domain.model.TourPlace
import com.gayadi.android.domain.repository.KeywordTourRepository

class SearchTourPlacesUseCase(
    private val repository: KeywordTourRepository,
) {
    suspend operator fun invoke(
        pageSize: Int = 10,
        keyword: String,
        arrange: String = "C",
        lDongRegnCd: String? = null,
        lDongSignguCd: String? = null,
        lclsSystm1: String? = null,
        lclsSystm2: String? = null,
        lclsSystm3: String? = null,
        maxPages: Int? = 1,
    ): Result<List<TourPlace>> = repository.searchPlaces(
        pageSize = pageSize,
        keyword = keyword,
        arrange = arrange,
        lDongRegnCd = lDongRegnCd,
        lDongSignguCd = lDongSignguCd,
        lclsSystm1 = lclsSystm1,
        lclsSystm2 = lclsSystm2,
        lclsSystm3 = lclsSystm3,
        maxPages = maxPages,
    )
}

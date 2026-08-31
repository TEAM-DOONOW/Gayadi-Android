package com.gayadi.android.domain.repository

import com.gayadi.android.domain.model.TourPlace

interface KeywordTourRepository {
    suspend fun searchPlaces(
        pageSize: Int,
        keyword: String,
        arrange: String = "C",
        lDongRegnCd: String? = null,
        lDongSignguCd: String? = null,
        lclsSystm1: String? = null,
        lclsSystm2: String? = null,
        lclsSystm3: String? = null,
        maxPages: Int? = null,
    ): Result<List<TourPlace>>
}

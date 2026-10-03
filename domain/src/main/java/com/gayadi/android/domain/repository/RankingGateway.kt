package com.gayadi.android.domain.repository

import com.gayadi.android.domain.model.RankingList
import com.gayadi.android.domain.model.RankingType

interface RankingGateway {
    /** [region]이 비어 있으면 전국 기준이다(관광지는 서버가 서울로 처리). */
    suspend fun getRankings(type: RankingType, region: String = "", limit: Int = 10): RankingList
}

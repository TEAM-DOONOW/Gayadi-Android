package com.gayadi.android.domain.model

/** 홈 카테고리별 TOP 순위 종류. 서버 `GET /api/v1/rankings`의 type 값과 같다. */
enum class RankingType {
    ATTRACTION,
    FESTIVAL,
    REGION,
    RESTAURANT,
}

data class RankingItem(
    val rank: Int,
    val title: String,
    val subtitle: String = "",
    val imageUrl: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val placeId: String? = null,
    val contentId: String? = null,
    val metricLabel: String = "",
)

data class RankingList(
    val type: RankingType,
    val region: String,
    val basePeriod: String,
    /** false면 제공기관 순위 대신 대체 목록이다. */
    val providerDataAvailable: Boolean,
    val items: List<RankingItem>,
)

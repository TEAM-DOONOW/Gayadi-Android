package com.gayadi.android.domain.model

data class TourPlace(
    val contentId: String,
    val title: String,
    val address: String,
    val addressDetail: String,
    val imageUrl: String,
    val longitude: Double?,
    val latitude: Double?,
)

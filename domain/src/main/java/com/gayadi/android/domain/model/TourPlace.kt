package com.gayadi.android.domain.model

data class TourPlace(
    val contentId: String,
    val title: String,
    val address: String,
    val addressDetail: String,
    val imageUrl: String,
    val longitude: Double?,
    val latitude: Double?,
    val contentTypeId: String = "",
    val lclsSystm1: String = "",
    val lclsSystm2: String = "",
    val lclsSystm3: String = "",
    val distanceMeters: Int? = null,
)

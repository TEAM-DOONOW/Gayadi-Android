package com.gayadi.android.data.model

data class TourPlaceDto(
    val contentId: String,
    val title: String,
    val address: String,
    val addressDetail: String,
    val firstImage: String,
    val mapX: String,
    val mapY: String,
    val contentTypeId: String = "",
    val lclsSystm1: String = "",
    val lclsSystm2: String = "",
    val lclsSystm3: String = "",
)

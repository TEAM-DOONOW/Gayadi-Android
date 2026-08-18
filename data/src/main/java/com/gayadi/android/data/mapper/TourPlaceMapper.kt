package com.gayadi.android.data.mapper

import com.gayadi.android.data.model.TourPlaceDto
import com.gayadi.android.domain.model.TourPlace

fun TourPlaceDto.toDomain() = TourPlace(
    contentId = contentId,
    title = title,
    address = address,
    addressDetail = addressDetail,
    imageUrl = firstImage,
    longitude = mapX.toDoubleOrNull(),
    latitude = mapY.toDoubleOrNull(),
)

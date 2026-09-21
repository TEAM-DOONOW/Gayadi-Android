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
    contentTypeId = contentTypeId,
    lclsSystm1 = lclsSystm1,
    lclsSystm2 = lclsSystm2,
    lclsSystm3 = lclsSystm3,
    regionCode = lDongRegnCd,
    districtCode = lDongSignguCd,
    distanceMeters = distanceMeters,
    crowdLevel = crowdLevel,
    concentrationScore = concentrationScore,
    crowdSource = crowdSource,
    crowdEstimated = crowdEstimated,
    crowdProviderDataAvailable = crowdProviderDataAvailable,
    crowdConfidence = crowdConfidence,
    crowdMessage = crowdMessage,
)

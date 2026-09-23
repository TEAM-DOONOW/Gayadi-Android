package com.gayadi.android.ui.screens

data class HomeTravelPlan(
    val id: String,
    val title: String,
    val date: String,
    val time: String,
    val memo: String,
    val isVisited: Boolean,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val placeId: String? = null,
    val imageUrl: String = "",
)

data class HomeTripDay(
    val dayNumber: Int,
    val date: String,
    val dateLabel: String,
)

package com.gayadi.android.ui.screens

data class HomeTravelPlan(
    val id: String,
    val title: String,
    val date: String,
    val time: String,
    val memo: String,
    val isVisited: Boolean,
)

data class HomeTripDay(
    val dayNumber: Int,
    val date: String,
    val dateLabel: String,
)

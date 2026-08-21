package com.gayadi.android.navigation

import com.gayadi.android.domain.model.TravelSchedule
import com.gayadi.android.ui.screens.HomeTravelPlan
import com.gayadi.android.ui.screens.HomeTripDay
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

private val navigationDateFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd")
private val koreanWeekdays = listOf("월", "화", "수", "목", "금", "토", "일")
private const val DEFAULT_COUNTDOWN_TEXT = "여행을 준비하고 있어요!"

internal fun TravelSchedule.toHomeTravelPlan() = HomeTravelPlan(
    id = id,
    title = title,
    date = date,
    time = time,
    memo = memo,
    isVisited = isVisited,
)

internal fun buildHomeTripDays(
    startDate: String,
    endDate: String,
): List<HomeTripDay> = runCatching {
    val start = LocalDate.parse(startDate, navigationDateFormatter)
    val end = LocalDate.parse(endDate, navigationDateFormatter)
    if (end.isBefore(start)) return@runCatching emptyList()

    generateSequence(start) { current ->
        current.plusDays(1).takeIf { !it.isAfter(end) }
    }.mapIndexed { index, date ->
        val weekday = koreanWeekdays[date.dayOfWeek.value - 1]
        HomeTripDay(
            dayNumber = index + 1,
            date = date.format(navigationDateFormatter),
            dateLabel = "${date.monthValue}.${date.dayOfMonth}/$weekday",
        )
    }.toList()
}.getOrDefault(emptyList())

internal fun buildTripCountdownText(
    startDate: String?,
    today: LocalDate = LocalDate.now(),
): String = startDate
    ?.takeIf(String::isNotBlank)
    ?.let { value ->
        runCatching {
            val tripStart = LocalDate.parse(value, navigationDateFormatter)
            when (val days = ChronoUnit.DAYS.between(today, tripStart)) {
                in 1..Long.MAX_VALUE -> "${days}일 남았어요!"
                0L -> "오늘 출발해요!"
                else -> "여행 중이에요!"
            }
        }.getOrDefault(DEFAULT_COUNTDOWN_TEXT)
    }
    ?: DEFAULT_COUNTDOWN_TEXT

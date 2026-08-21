package com.gayadi.android.navigation

import com.gayadi.android.domain.model.TravelSchedule
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeNavigationMapperTest {
    @Test
    fun scheduleMapsToHomePlanWithoutLosingEditableFields() {
        val schedule = TravelSchedule(
            id = "schedule-1",
            tripId = "trip-1",
            title = "성산일출봉",
            date = "2026.08.21",
            time = "10:00",
            order = 1,
            isVisited = true,
            memo = "정문에서 만나기",
        )

        val plan = schedule.toHomeTravelPlan()

        assertEquals(schedule.id, plan.id)
        assertEquals(schedule.title, plan.title)
        assertEquals(schedule.date, plan.date)
        assertEquals(schedule.time, plan.time)
        assertEquals(schedule.memo, plan.memo)
        assertTrue(plan.isVisited)
    }

    @Test
    fun tripDaysIncludeBothEndsAndKoreanWeekdayLabels() {
        val days = buildHomeTripDays("2026.08.21", "2026.08.23")

        assertEquals(listOf(1, 2, 3), days.map { it.dayNumber })
        assertEquals(
            listOf("2026.08.21", "2026.08.22", "2026.08.23"),
            days.map { it.date },
        )
        assertEquals(listOf("8.21/금", "8.22/토", "8.23/일"), days.map { it.dateLabel })
    }

    @Test
    fun invalidTripRangeReturnsNoDays() {
        assertTrue(buildHomeTripDays("2026.08.23", "2026.08.21").isEmpty())
        assertTrue(buildHomeTripDays("invalid", "2026.08.21").isEmpty())
    }

    @Test
    fun countdownCoversFutureTodayPastAndInvalidDates() {
        val today = LocalDate.of(2026, 8, 21)

        assertEquals("3일 남았어요!", buildTripCountdownText("2026.08.24", today))
        assertEquals("오늘 출발해요!", buildTripCountdownText("2026.08.21", today))
        assertEquals("여행 중이에요!", buildTripCountdownText("2026.08.20", today))
        assertEquals("여행을 준비하고 있어요!", buildTripCountdownText("invalid", today))
        assertEquals("여행을 준비하고 있어요!", buildTripCountdownText(null, today))
    }
}

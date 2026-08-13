package com.gayadi.android.notification

import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ExpenseReminderContractTest {
    private val seoul = ZoneId.of("Asia/Seoul")
    private val clock = Clock.fixed(Instant.parse("2026-08-13T01:00:00Z"), seoul)

    @Test
    fun futureEndTimeReturnsDelayFromCurrentInstant() {
        assertEquals(
            30 * 60 * 1_000L,
            expenseReminderDelayMillis("2026.08.13", "10:30", clock),
        )
    }

    @Test
    fun absentMalformedAndNonFutureEndTimesAreIgnored() {
        assertNull(expenseReminderDelayMillis("2026.08.13", null, clock))
        assertNull(expenseReminderDelayMillis("2026.08.13", "", clock))
        assertNull(expenseReminderDelayMillis("2026.02.30", "10:30", clock))
        assertNull(expenseReminderDelayMillis("2026.08.13", "09:59", clock))
        assertNull(expenseReminderDelayMillis("2026.08.13", "10:00", clock))
    }

    @Test
    fun workNameAndUriAreStableAndEscapePathSegments() {
        val workName = expenseReminderWorkName("trip/서울", "schedule 1")

        assertEquals("expense-reminder:trip%2F%EC%84%9C%EC%9A%B8:schedule%201", workName)
        assertEquals(workName, expenseReminderWorkName("trip/서울", "schedule 1"))
        assertEquals(
            "gayadi://expense/trip%2F%EC%84%9C%EC%9A%B8/schedule%201",
            expenseReminderUri("trip/서울", "schedule 1"),
        )
    }

    @Test
    fun notificationIdIsStableAndScheduleSpecific() {
        assertEquals(
            expenseReminderNotificationId("trip-1", "schedule-1"),
            expenseReminderNotificationId("trip-1", "schedule-1"),
        )
        assertNotEquals(
            expenseReminderNotificationId("trip-1", "schedule-1"),
            expenseReminderNotificationId("trip-1", "schedule-2"),
        )
    }
}

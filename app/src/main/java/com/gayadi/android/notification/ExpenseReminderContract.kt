package com.gayadi.android.notification

import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.Clock
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.format.ResolverStyle

internal const val EXPENSE_REMINDER_CHANNEL_ID = "schedule_expense_reminders"
internal const val EXPENSE_REMINDER_TAG = "expense-reminder"
internal const val KEY_TRIP_ID = "trip-id"
internal const val KEY_SCHEDULE_ID = "schedule-id"
internal const val KEY_SCHEDULE_TITLE = "schedule-title"

private val scheduleDateTimeFormatter = DateTimeFormatter.ofPattern("uuuu.MM.dd HH:mm")
    .withResolverStyle(ResolverStyle.STRICT)

internal fun expenseReminderWorkName(tripId: String, scheduleId: String): String =
    "expense-reminder:${encodePathSegment(tripId)}:${encodePathSegment(scheduleId)}"

internal fun expenseReminderUri(tripId: String, scheduleId: String): String =
    "gayadi://expense/${encodePathSegment(tripId)}/${encodePathSegment(scheduleId)}"

internal fun expenseReminderNotificationId(tripId: String, scheduleId: String): Int =
    expenseReminderWorkName(tripId, scheduleId).hashCode()

internal fun expenseReminderDelayMillis(
    date: String,
    endTime: String?,
    clock: Clock = Clock.systemDefaultZone(),
    zoneId: ZoneId = clock.zone,
): Long? = expenseReminderTriggerAtMillis(date, endTime, zoneId)
    ?.let { triggerAtMillis -> (triggerAtMillis - clock.millis()).takeIf { it > 0L } }

internal fun expenseReminderTriggerAtMillis(
    date: String,
    endTime: String?,
    zoneId: ZoneId,
): Long? {
    if (endTime.isNullOrBlank()) return null
    return try {
        LocalDateTime.parse("$date $endTime", scheduleDateTimeFormatter)
            .atZone(zoneId)
            .toInstant()
            .toEpochMilli()
    } catch (_: DateTimeParseException) {
        return null
    }
}

private fun encodePathSegment(value: String): String =
    URLEncoder.encode(value, StandardCharsets.UTF_8.name()).replace("+", "%20")

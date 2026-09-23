package com.gayadi.android.ui.screens

import com.gayadi.android.domain.model.ScheduleType
import com.gayadi.android.domain.model.TravelSchedule

internal fun insertPlaceSchedule(
    schedules: List<TravelSchedule>, newSchedule: TravelSchedule, beforeScheduleId: String?,
): List<TravelSchedule> {
    val ordered = schedules.sortedBy { it.order }.toMutableList()
    val index = if (beforeScheduleId != null) {
        ordered.indexOfFirst { it.id == beforeScheduleId && it.date == newSchedule.date }
            .also { require(it >= 0) { "추가할 위치의 일정이 변경됐어요." } }
    } else {
        val lastSameDay = ordered.indexOfLast { it.date == newSchedule.date && it.type == ScheduleType.MAIN }
        if (lastSameDay >= 0) lastSameDay + 1 else ordered.size
    }
    ordered.add(index, newSchedule)
    return ordered.mapIndexed { order, schedule -> schedule.copy(order = order) }
}

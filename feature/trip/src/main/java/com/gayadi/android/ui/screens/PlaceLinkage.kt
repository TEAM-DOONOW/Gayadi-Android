package com.gayadi.android.ui.screens

import com.gayadi.android.domain.model.ScheduleType
import com.gayadi.android.domain.model.TravelSchedule
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/** 이웃 일정과 이 거리 안이면 지역 규모와 관계없이 연계된 것으로 본다. */
private const val MinLinkedDistanceKm = 5.0
/** 기존 장소보다 이 배율 이상 멀어지면 연계가 끊긴 것으로 본다. */
private const val LinkedDistanceRatio = 1.5

data class GeoPoint(val latitude: Double, val longitude: Double)

/** 같은 날 방문 일정에서 바꿀 일정과 그 앞뒤 일정. */
data class ReplacementNeighbors(
    val target: TravelSchedule,
    val previous: TravelSchedule?,
    val next: TravelSchedule?,
)

/** 같은 날 장소 방문 일정을 방문 순서대로 돌려준다. */
fun sameDayPlaceVisits(schedules: List<TravelSchedule>, tripId: String, date: String): List<TravelSchedule> =
    schedules.filter { it.tripId == tripId && it.date == date && it.type == ScheduleType.MAIN && it.placeId != null }
        .sortedBy { it.order }

fun replacementNeighbors(visits: List<TravelSchedule>, targetId: String?): ReplacementNeighbors? {
    val index = visits.indexOfFirst { it.id == targetId }.takeIf { it >= 0 } ?: return null
    return ReplacementNeighbors(visits[index], visits.getOrNull(index - 1), visits.getOrNull(index + 1))
}

/**
 * 연계 모드에서 바꿀 일정의 추천 기준점. 앞뒤가 모두 있으면 중간 지점, 한쪽만 있으면 그 일정이다.
 * 첫 번째 일정을 바꾸면 두 번째 일정 근처가 기준이 되어 이후 동선과 이어진다.
 */
fun ReplacementNeighbors.linkedOrigin(): GeoPoint? {
    val points = listOfNotNull(previous?.point(), next?.point())
    if (points.isEmpty()) return null
    return GeoPoint(points.map(GeoPoint::latitude).average(), points.map(GeoPoint::longitude).average())
}

/**
 * [candidate]로 바꿔도 앞뒤 일정과 연계되는지 판단한다.
 * 좌표를 모르면 경고하지 않도록 연계된 것으로 본다. 기준 거리는 기존 장소와 이웃 사이 거리의 1.5배(최소 5km)다.
 */
fun ReplacementNeighbors.isLinked(candidate: GeoPoint?): Boolean {
    candidate ?: return true
    val original = target.point()
    return listOfNotNull(previous?.point(), next?.point()).all { neighbor ->
        val allowed = max(MinLinkedDistanceKm, (original?.let { distanceKm(it, neighbor) } ?: 0.0) * LinkedDistanceRatio)
        distanceKm(candidate, neighbor) <= allowed
    }
}

fun TravelSchedule.point(): GeoPoint? {
    val lat = latitude ?: return null
    val lon = longitude ?: return null
    return GeoPoint(lat, lon)
}

fun distanceKm(from: GeoPoint, to: GeoPoint): Double {
    val earthRadiusKm = 6371.0
    val dLat = Math.toRadians(to.latitude - from.latitude)
    val dLon = Math.toRadians(to.longitude - from.longitude)
    val a = sin(dLat / 2).pow(2) +
        cos(Math.toRadians(from.latitude)) * cos(Math.toRadians(to.latitude)) * sin(dLon / 2).pow(2)
    return 2 * earthRadiusKm * asin(sqrt(a))
}

package com.gayadi.android.data.repository

import com.gayadi.android.domain.model.DepartureMode
import com.gayadi.android.domain.model.InvitationStatus
import com.gayadi.android.domain.model.ScheduleType
import com.gayadi.android.domain.model.TravelInvitation
import com.gayadi.android.domain.model.TravelParticipant
import com.gayadi.android.domain.model.TravelSchedule
import com.gayadi.android.domain.model.TravelState
import com.gayadi.android.domain.model.TravelTrip
import com.gayadi.android.domain.model.TripStatus
import com.gayadi.android.domain.repository.TravelRepository
import java.io.File
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject

/** Atomic JSON file storage for Android-only travel state before API integration. */
class FileTravelRepository(
    private val file: File,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : TravelRepository {
    override suspend fun getTravelState(): Result<TravelState> = withContext(ioDispatcher) {
        runCatching {
            if (!file.exists() || file.readText().isBlank()) TravelState()
            else decode(JSONObject(file.readText()))
        }
    }

    override suspend fun saveTravelState(state: TravelState): Result<Unit> = withContext(ioDispatcher) {
        mutexFor(file).withLock {
            runCatching {
                file.parentFile?.mkdirs()
                val temporary = File(file.parentFile, "${file.name}.tmp")
                try {
                    temporary.writeText(encode(state).toString())
                    check(temporary.renameTo(file)) { "여행 정보를 저장하지 못했습니다." }
                } finally {
                    if (temporary.exists()) temporary.delete()
                }
            }
        }
    }

    private fun encode(state: TravelState) = JSONObject().apply {
        put("selectedTripId", state.selectedTripId ?: JSONObject.NULL)
        put("favoritePlaceIds", JSONArray(state.favoritePlaceIds.toList()))
        put("appliedRouteIds", JSONObject(state.appliedRouteIds))
        put("trips", JSONArray().apply {
            state.trips.forEach { trip ->
                put(JSONObject().apply {
                    put("id", trip.id)
                    put("name", trip.name)
                    put("startDate", trip.startDate)
                    put("endDate", trip.endDate)
                    put("cities", JSONArray(trip.cities))
                    put("coverImageResList", JSONArray(trip.coverImageResList))
                    put("departureMode", trip.departureMode.name)
                    put("status", trip.status.name)
                    put("participantIds", JSONArray(trip.participantIds))
                })
            }
        })
        put("participants", JSONArray().apply {
            state.participants.forEach { participant ->
                put(JSONObject().apply {
                    put("id", participant.id)
                    put("nickname", participant.nickname)
                    put("characterKey", participant.characterKey ?: JSONObject.NULL)
                })
            }
        })
        put("invitations", JSONArray().apply {
            state.invitations.forEach { invitation ->
                put(JSONObject().apply {
                    put("id", invitation.id)
                    put("tripId", invitation.tripId)
                    put("code", invitation.code)
                    put("inviteeId", invitation.inviteeId)
                    put("status", invitation.status.name)
                })
            }
        })
        put("schedules", JSONArray().apply {
            state.schedules.forEach { schedule ->
                put(JSONObject().apply {
                    put("id", schedule.id)
                    put("tripId", schedule.tripId)
                    put("title", schedule.title)
                    put("placeId", schedule.placeId ?: JSONObject.NULL)
                    put("date", schedule.date)
                    put("time", schedule.time)
                    put("type", schedule.type.name)
                    put("order", schedule.order)
                    put("isVisited", schedule.isVisited)
                })
            }
        })
    }

    private fun decode(root: JSONObject) = TravelState(
        trips = root.optJSONArray("trips").objects().map { trip ->
            TravelTrip(
                id = trip.getString("id"),
                name = trip.getString("name"),
                startDate = trip.getString("startDate"),
                endDate = trip.getString("endDate"),
                cities = trip.optJSONArray("cities").strings(),
                coverImageResList = trip.optJSONArray("coverImageResList").ints(),
                departureMode = trip.optString("departureMode", DepartureMode.SOLO.name).enumOr(DepartureMode.SOLO),
                status = trip.optString("status", TripStatus.PLANNING.name).enumOr(TripStatus.PLANNING),
                participantIds = trip.optJSONArray("participantIds").strings(),
            )
        },
        participants = root.optJSONArray("participants").objects().map { participant ->
            TravelParticipant(
                id = participant.getString("id"),
                nickname = participant.getString("nickname"),
                characterKey = participant.optNullableString("characterKey"),
            )
        },
        invitations = root.optJSONArray("invitations").objects().map { invitation ->
            TravelInvitation(
                id = invitation.getString("id"),
                tripId = invitation.getString("tripId"),
                code = invitation.getString("code"),
                inviteeId = invitation.getString("inviteeId"),
                status = invitation.optString("status", InvitationStatus.PENDING.name)
                    .enumOr(InvitationStatus.PENDING),
            )
        },
        schedules = root.optJSONArray("schedules").objects().map { schedule ->
            TravelSchedule(
                id = schedule.getString("id"),
                tripId = schedule.getString("tripId"),
                title = schedule.getString("title"),
                placeId = schedule.optNullableString("placeId"),
                date = schedule.optString("date"),
                time = schedule.optString("time"),
                type = schedule.optString("type", ScheduleType.MAIN.name).enumOr(ScheduleType.MAIN),
                order = schedule.optInt("order"),
                isVisited = schedule.optBoolean("isVisited"),
            )
        },
        favoritePlaceIds = root.optJSONArray("favoritePlaceIds").strings().toSet(),
        appliedRouteIds = root.optJSONObject("appliedRouteIds").stringMap(),
        selectedTripId = root.optNullableString("selectedTripId"),
    )

    private fun JSONArray?.objects(): List<JSONObject> =
        if (this == null) emptyList() else List(length(), ::getJSONObject)

    private fun JSONArray?.strings(): List<String> =
        if (this == null) emptyList() else List(length(), ::getString)

    private fun JSONArray?.ints(): List<Int> =
        if (this == null) emptyList() else List(length(), ::getInt)

    private fun JSONObject.optNullableString(key: String): String? =
        if (isNull(key)) null else optString(key).takeIf(String::isNotBlank)

    private fun JSONObject?.stringMap(): Map<String, String> =
        this?.keys()?.asSequence()?.associateWith { key -> getString(key) }.orEmpty()

    private inline fun <reified T : Enum<T>> String.enumOr(default: T): T =
        enumValues<T>().firstOrNull { it.name == this } ?: default

    companion object {
        private val fileMutexes = java.util.concurrent.ConcurrentHashMap<String, Mutex>()

        private fun mutexFor(file: File): Mutex =
            fileMutexes.computeIfAbsent(file.absoluteFile.normalize().path) { Mutex() }
    }
}

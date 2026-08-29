package com.gayadi.android.data.repository

import com.gayadi.android.domain.model.InvitationStatus
import com.gayadi.android.domain.model.LOCAL_CURRENT_USER_ID
import com.gayadi.android.domain.model.ExpenseCategory
import com.gayadi.android.domain.model.ExpensePaymentSource
import com.gayadi.android.domain.model.ScheduleType
import com.gayadi.android.domain.model.TravelExpense
import com.gayadi.android.domain.model.TravelInvitation
import com.gayadi.android.domain.model.TravelParticipant
import com.gayadi.android.domain.model.TravelSchedule
import com.gayadi.android.domain.model.TravelState
import com.gayadi.android.domain.model.TravelTrip
import com.gayadi.android.domain.model.TripStatus
import com.gayadi.android.domain.repository.TravelRepository
import java.io.File
import java.nio.file.StandardCopyOption
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
        mutexFor(file).withLock {
            runCatching(::readState)
        }
    }

    override suspend fun saveTravelState(state: TravelState): Result<Unit> = withContext(ioDispatcher) {
        mutexFor(file).withLock {
            runCatching { writeState(state) }
        }
    }

    override suspend fun updateTravelState(
        transform: (TravelState) -> TravelState,
    ): Result<TravelState> = withContext(ioDispatcher) {
        mutexFor(file).withLock {
            runCatching {
                val updated = transform(readState())
                writeState(updated)
                updated
            }
        }
    }

    private fun readState(): TravelState {
        if (!file.exists()) return TravelState()
        val content = file.readText()
        return if (content.isBlank()) TravelState() else decode(JSONObject(content))
    }

    private fun writeState(state: TravelState) {
        requireValidExpenseTotals(state)
        file.parentFile?.mkdirs()
        val temporary = File(file.parentFile, "${file.name}.tmp")
        try {
            temporary.writeText(encode(state).toString())
            java.nio.file.Files.move(
                temporary.toPath(),
                file.toPath(),
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE,
            )
        } finally {
            if (temporary.exists()) temporary.delete()
        }
    }

    private fun encode(state: TravelState) = JSONObject().apply {
        put("schemaVersion", CURRENT_SCHEMA_VERSION)
        put("currentUserId", state.currentUserId)
        put("selectedTripId", state.selectedTripId ?: JSONObject.NULL)
        put("favoritePlaceIds", JSONArray(state.favoritePlaceIds.toList()))
        put("appliedRouteIds", JSONObject(state.appliedRouteIds))
        put("sharedFundAmounts", JSONObject().apply {
            state.sharedFundAmounts.forEach { (tripId, amount) -> put(tripId, amount) }
        })
        put("trips", JSONArray().apply {
            state.trips.forEach { trip ->
                put(JSONObject().apply {
                    put("id", trip.id)
                    put("name", trip.name)
                    put("startDate", trip.startDate)
                    put("endDate", trip.endDate)
                    put("cities", JSONArray(trip.cities))
                    put("coverImageResList", JSONArray(trip.coverImageResList))
                    put("status", trip.status.name)
                    put("participantIds", JSONArray(trip.participantIds))
                    put("inviteCode", trip.inviteCode)
                    put("isGroupTrip", trip.isGroupTrip)
                    put("dateAvailability", JSONObject().apply {
                        trip.dateAvailability.forEach { (participantId, dates) ->
                            put(participantId, JSONArray(dates))
                        }
                    })
                    put("ownerId", trip.ownerId)
                    put("version", trip.version)
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
                    put("endTime", schedule.endTime ?: JSONObject.NULL)
                    put("memo", schedule.memo)
                })
            }
        })
        put("expenses", JSONArray().apply {
            state.expenses.forEach { expense ->
                put(JSONObject().apply {
                    put("id", expense.id)
                    put("tripId", expense.tripId)
                    put("scheduleId", expense.scheduleId)
                    put("title", expense.title)
                    put("memo", expense.memo)
                    put("amount", expense.amount)
                    put("payerId", expense.payerId)
                    put("participantIds", JSONArray(expense.participantIds))
                    put("date", expense.date)
                    put("time", expense.time)
                    put("category", expense.category.name)
                    put("paymentSource", expense.paymentSource.name)
                    put("receiptImageUri", expense.receiptImageUri ?: JSONObject.NULL)
                })
            }
        })
    }

    private fun decode(root: JSONObject): TravelState {
        val schemaVersion = readSchemaVersion(root)
        require(schemaVersion in LEGACY_SCHEMA_VERSION..CURRENT_SCHEMA_VERSION) {
            "지원하지 않는 여행 데이터 스키마 버전입니다: $schemaVersion"
        }
        return TravelState(
        trips = root.optJSONArray("trips").objects().map { trip ->
            TravelTrip(
                id = trip.getString("id"),
                name = trip.getString("name"),
                startDate = trip.getString("startDate"),
                endDate = trip.getString("endDate"),
                cities = trip.optJSONArray("cities").strings(),
                coverImageResList = trip.optJSONArray("coverImageResList").ints(),
                status = trip.optString("status", TripStatus.PLANNING.name).enumOr(TripStatus.PLANNING),
                participantIds = trip.optJSONArray("participantIds").strings(),
                inviteCode = trip.optString("inviteCode"),
                isGroupTrip = trip.optBoolean("isGroupTrip"),
                dateAvailability = trip.optJSONObject("dateAvailability")?.let { availability ->
                    availability.keys().asSequence().associateWith { participantId ->
                        availability.optJSONArray(participantId).strings()
                    }
                }.orEmpty(),
                ownerId = trip.optString("ownerId"),
                version = trip.optInt("version", 0),
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
                endTime = schedule.optNullableString("endTime"),
                memo = schedule.optString("memo"),
            )
        },
        favoritePlaceIds = root.optJSONArray("favoritePlaceIds").strings().toSet(),
        appliedRouteIds = root.optJSONObject("appliedRouteIds").stringMap(),
        selectedTripId = root.optNullableString("selectedTripId"),
        expenses = root.optJSONArray("expenses").objects().map { expense ->
            TravelExpense(
                id = expense.getString("id"),
                tripId = expense.getString("tripId"),
                scheduleId = expense.getString("scheduleId"),
                title = expense.getString("title"),
                memo = expense.optString("memo"),
                amount = expense.getLong("amount"),
                payerId = expense.getString("payerId"),
                participantIds = expense.optJSONArray("participantIds").strings(),
                date = expense.getString("date"),
                time = expense.getString("time"),
                category = expense.optString("category", ExpenseCategory.OTHER.name).enumOr(ExpenseCategory.OTHER),
                paymentSource = expense.optString("paymentSource", ExpensePaymentSource.PERSONAL.name)
                    .enumOr(ExpensePaymentSource.PERSONAL),
                receiptImageUri = expense.optNullableString("receiptImageUri"),
            )
        },
        sharedFundAmounts = root.optJSONObject("sharedFundAmounts")?.let { amounts ->
            amounts.keys().asSequence().associateWith(amounts::getLong)
        }.orEmpty(),
        currentUserId = root.optNullableString("currentUserId") ?: LOCAL_CURRENT_USER_ID,
    ).also(::requireValidExpenseTotals)
    }

    private fun readSchemaVersion(root: JSONObject): Int {
        if (!root.has("schemaVersion")) return LEGACY_SCHEMA_VERSION
        val rawVersion = root.get("schemaVersion")
        require(rawVersion is Int || rawVersion is Long) {
            "여행 데이터 스키마 버전은 정수여야 합니다."
        }
        val version = (rawVersion as Number).toLong()
        require(version in Int.MIN_VALUE.toLong()..Int.MAX_VALUE.toLong()) {
            "여행 데이터 스키마 버전 범위를 벗어났습니다: $version"
        }
        return version.toInt()
    }

    private fun requireValidExpenseTotals(state: TravelState) {
        state.expenses.groupBy(TravelExpense::tripId).forEach { (tripId, expenses) ->
            try {
                expenses.fold(0L) { total, expense -> Math.addExact(total, expense.amount) }
            } catch (error: ArithmeticException) {
                throw IllegalArgumentException("여행 $tripId 비용 총액이 지원 범위를 벗어났습니다.", error)
            }
        }
    }

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
        internal const val CURRENT_SCHEMA_VERSION = 2
        private const val LEGACY_SCHEMA_VERSION = 1
        private val fileMutexes = java.util.concurrent.ConcurrentHashMap<String, Mutex>()

        private fun mutexFor(file: File): Mutex =
            fileMutexes.computeIfAbsent(file.absoluteFile.normalize().path) { Mutex() }
    }
}

package com.gayadi.android.data.repository

import com.gayadi.android.domain.model.SharedTripInvite
import com.gayadi.android.domain.model.LOCAL_CURRENT_USER_ID
import com.gayadi.android.domain.model.TravelParticipant
import com.gayadi.android.domain.model.TravelTrip
import com.gayadi.android.domain.model.TripStatus
import com.gayadi.android.domain.repository.TripInviteRepository
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class FirestoreTripInviteRepository(
    private val firestore: FirebaseFirestore,
    private val installationId: String,
) : TripInviteRepository {
    override suspend fun publish(trip: TravelTrip, owner: TravelParticipant): Result<Unit> = runCatching {
        val code = trip.inviteCode.trim().uppercase()
        require(code.matches(INVITE_CODE_PATTERN)) { "6자리 초대 코드가 필요합니다." }
        val reference = firestore.collection(INVITES_COLLECTION).document(code)
        val remoteOwner = owner.toRemoteMap()

        firestore.runTransaction { transaction ->
            val existing = transaction.get(reference)
            val existingData = existing.data.orEmpty()
            val existingTripId = existingData[TRIP_ID_FIELD] as? String
            require(existingTripId == null || existingTripId == trip.id) { "이미 사용 중인 초대 코드입니다." }

            val participants = mergeParticipant(readParticipantMaps(existingData), remoteOwner)
            val availability = readAvailability(existingData).toMutableMap().apply {
                trip.dateAvailability[owner.id]?.let { put(installationId, it.distinct().sorted()) }
            }
            val isConfirmed = existingData[DATE_STATUS_FIELD] == DATE_STATUS_CONFIRMED
            transaction.set(
                reference,
                mapOf(
                    INVITE_CODE_FIELD to code,
                    TRIP_ID_FIELD to trip.id,
                    NAME_FIELD to trip.name,
                    START_DATE_FIELD to if (isConfirmed) existingData[START_DATE_FIELD].orEmptyString() else trip.startDate,
                    END_DATE_FIELD to if (isConfirmed) existingData[END_DATE_FIELD].orEmptyString() else trip.endDate,
                    CITIES_FIELD to trip.cities,
                    STATUS_FIELD to trip.status.name,
                    GROUP_TRIP_FIELD to trip.isGroupTrip,
                    OWNER_ID_FIELD to (existingData[OWNER_ID_FIELD] ?: installationId),
                    PARTICIPANTS_FIELD to participants,
                    DATE_AVAILABILITY_FIELD to availability,
                    DATE_STATUS_FIELD to (existingData[DATE_STATUS_FIELD] ?: DATE_STATUS_COORDINATING),
                    CREATED_AT_FIELD to (existingData[CREATED_AT_FIELD] ?: FieldValue.serverTimestamp()),
                    UPDATED_AT_FIELD to FieldValue.serverTimestamp(),
                ),
            )
        }.awaitResult()
    }

    override suspend fun join(
        inviteCode: String,
        participant: TravelParticipant,
    ): Result<SharedTripInvite> = runCatching {
        val code = inviteCode.trim().uppercase()
        require(code.matches(INVITE_CODE_PATTERN)) { "6자리 초대 코드를 입력해 주세요." }
        val reference = firestore.collection(INVITES_COLLECTION).document(code)
        val remoteParticipant = participant.toRemoteMap()

        val joinedData = firestore.runTransaction { transaction ->
            val snapshot = transaction.get(reference)
            require(snapshot.exists()) { "유효하지 않은 초대 코드예요" }
            val current = snapshot.data.orEmpty()
            require(current[DATE_STATUS_FIELD] != DATE_STATUS_CONFIRMED) {
                "이미 여행 날짜가 확정되어 다시 제출할 수 없어요"
            }
            val updated = current + mapOf(
                PARTICIPANTS_FIELD to mergeParticipant(readParticipantMaps(current), remoteParticipant),
                UPDATED_AT_FIELD to FieldValue.serverTimestamp(),
            )
            transaction.set(reference, updated)
            updated
        }.awaitResult()

        joinedData.toSharedInvite(participant.id)
    }

    override fun observe(inviteCode: String): Flow<Result<SharedTripInvite>> = callbackFlow {
        val code = inviteCode.trim().uppercase()
        if (!code.matches(INVITE_CODE_PATTERN)) {
            trySend(Result.failure(IllegalArgumentException("6자리 초대 코드를 입력해 주세요.")))
            close()
            return@callbackFlow
        }
        val registration = firestore.collection(INVITES_COLLECTION).document(code)
            .addSnapshotListener { snapshot, error ->
                when {
                    error != null -> trySend(Result.failure(error))
                    snapshot == null || !snapshot.exists() ->
                        trySend(Result.failure(IllegalStateException("초대 여행을 찾을 수 없어요")))
                    else -> trySend(runCatching { snapshot.data.orEmpty().toSharedInvite(LOCAL_CURRENT_USER_ID) })
                }
            }
        awaitClose { registration.remove() }
    }

    override suspend fun submitAvailability(inviteCode: String, dates: List<String>): Result<Unit> = runCatching {
        val code = inviteCode.trim().uppercase()
        require(code.matches(INVITE_CODE_PATTERN)) { "6자리 초대 코드가 필요합니다." }
        val reference = firestore.collection(INVITES_COLLECTION).document(code)
        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(reference)
            require(snapshot.exists()) { "초대 여행을 찾을 수 없어요" }
            val current = snapshot.data.orEmpty()
            require(readParticipantMaps(current).any { it[PARTICIPANT_ID_FIELD] == installationId }) {
                "이 여행의 참여자만 날짜를 제출할 수 있어요"
            }
            val availability = readAvailability(current).toMutableMap().apply {
                put(installationId, dates.distinct().sorted())
            }
            transaction.update(reference, mapOf(
                DATE_AVAILABILITY_FIELD to availability,
                DATE_STATUS_FIELD to DATE_STATUS_COORDINATING,
                UPDATED_AT_FIELD to FieldValue.serverTimestamp(),
            ))
        }.awaitResult()
    }

    override suspend fun finalizeDates(
        inviteCode: String,
        startDate: String,
        endDate: String,
    ): Result<Unit> = runCatching {
        require(startDate.isNotBlank() && endDate.isNotBlank()) { "확정할 여행 날짜가 필요합니다." }
        val code = inviteCode.trim().uppercase()
        val reference = firestore.collection(INVITES_COLLECTION).document(code)
        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(reference)
            require(snapshot.exists()) { "초대 여행을 찾을 수 없어요" }
            require(snapshot.getString(OWNER_ID_FIELD) == installationId) { "방장만 여행 날짜를 확정할 수 있어요" }
            transaction.update(reference, mapOf(
                START_DATE_FIELD to startDate,
                END_DATE_FIELD to endDate,
                DATE_STATUS_FIELD to DATE_STATUS_CONFIRMED,
                UPDATED_AT_FIELD to FieldValue.serverTimestamp(),
            ))
        }.awaitResult()
    }

    private fun Map<String, Any?>.toSharedInvite(localParticipantId: String): SharedTripInvite {
        val participants = readParticipantMaps(this).map { item ->
            TravelParticipant(
                id = (item[PARTICIPANT_ID_FIELD] as? String).orEmpty().let { remoteId ->
                    if (remoteId == installationId) localParticipantId else remoteId
                },
                nickname = (item[PARTICIPANT_NICKNAME_FIELD] as? String).orEmpty(),
                characterKey = (item[PARTICIPANT_CHARACTER_FIELD] as? String)?.takeIf(String::isNotBlank),
            )
        }
        val trip = TravelTrip(
            id = requiredString(TRIP_ID_FIELD),
            name = requiredString(NAME_FIELD),
            startDate = optionalString(START_DATE_FIELD),
            endDate = optionalString(END_DATE_FIELD),
            cities = (get(CITIES_FIELD) as? List<*>)?.filterIsInstance<String>().orEmpty(),
            status = requiredString(STATUS_FIELD).toTripStatus(),
            participantIds = participants.map(TravelParticipant::id),
            inviteCode = requiredString(INVITE_CODE_FIELD),
            isGroupTrip = get(GROUP_TRIP_FIELD) as? Boolean ?: false,
            dateAvailability = readAvailability(this).mapKeys { (remoteId, _) ->
                if (remoteId == installationId) localParticipantId else remoteId
            },
            ownerId = optionalString(OWNER_ID_FIELD).let { remoteId ->
                if (remoteId == installationId) localParticipantId else remoteId
            },
        )
        return SharedTripInvite(trip, participants)
    }

    private fun TravelParticipant.toRemoteMap(): Map<String, Any?> = mapOf(
        PARTICIPANT_ID_FIELD to installationId,
        PARTICIPANT_NICKNAME_FIELD to nickname,
        PARTICIPANT_CHARACTER_FIELD to characterKey,
    )

    private fun readParticipantMaps(data: Map<String, Any?>): List<Map<String, Any?>> =
        (data[PARTICIPANTS_FIELD] as? List<*>)
            ?.mapNotNull { raw ->
                (raw as? Map<*, *>)?.entries?.associate { (key, value) -> key.toString() to value }
            }
            .orEmpty()

    private fun readAvailability(data: Map<String, Any?>): Map<String, List<String>> =
        (data[DATE_AVAILABILITY_FIELD] as? Map<*, *>)?.entries?.associate { (key, value) ->
            key.toString() to (value as? List<*>)?.filterIsInstance<String>().orEmpty()
        }.orEmpty()

    private fun mergeParticipant(
        participants: List<Map<String, Any?>>,
        participant: Map<String, Any?>,
    ): List<Map<String, Any?>> = participants.filterNot {
        it[PARTICIPANT_ID_FIELD] == participant[PARTICIPANT_ID_FIELD]
    } + participant

    private fun Map<String, Any?>.requiredString(field: String): String =
        (get(field) as? String)?.takeIf(String::isNotBlank)
            ?: error("초대 여행의 $field 값이 없습니다.")

    private fun Map<String, Any?>.optionalString(field: String): String =
        get(field) as? String ?: ""

    private fun Any?.orEmptyString(): String = this as? String ?: ""

    private fun String.toTripStatus(): TripStatus =
        TripStatus.entries.firstOrNull { it.name == this } ?: TripStatus.PLANNING

    private suspend fun <T> Task<T>.awaitResult(): T = suspendCancellableCoroutine { continuation ->
        addOnSuccessListener { value -> if (continuation.isActive) continuation.resume(value) }
        addOnFailureListener { error -> if (continuation.isActive) continuation.resumeWithException(error) }
        addOnCanceledListener { continuation.cancel() }
    }

    private companion object {
        const val INVITES_COLLECTION = "tripInvites"
        const val INVITE_CODE_FIELD = "inviteCode"
        const val TRIP_ID_FIELD = "tripId"
        const val NAME_FIELD = "name"
        const val START_DATE_FIELD = "startDate"
        const val END_DATE_FIELD = "endDate"
        const val CITIES_FIELD = "cities"
        const val STATUS_FIELD = "status"
        const val GROUP_TRIP_FIELD = "isGroupTrip"
        const val OWNER_ID_FIELD = "ownerId"
        const val PARTICIPANTS_FIELD = "participants"
        const val DATE_AVAILABILITY_FIELD = "dateAvailability"
        const val DATE_STATUS_FIELD = "dateStatus"
        const val DATE_STATUS_COORDINATING = "COORDINATING"
        const val DATE_STATUS_CONFIRMED = "CONFIRMED"
        const val PARTICIPANT_ID_FIELD = "id"
        const val PARTICIPANT_NICKNAME_FIELD = "nickname"
        const val PARTICIPANT_CHARACTER_FIELD = "characterKey"
        const val CREATED_AT_FIELD = "createdAt"
        const val UPDATED_AT_FIELD = "updatedAt"
        val INVITE_CODE_PATTERN = Regex("[A-Z0-9]{6}")
    }
}

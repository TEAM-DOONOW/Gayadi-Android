package com.gayadi.android.data.repository

import com.gayadi.android.domain.model.SharedTripInvite
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
            transaction.set(
                reference,
                mapOf(
                    INVITE_CODE_FIELD to code,
                    TRIP_ID_FIELD to trip.id,
                    NAME_FIELD to trip.name,
                    START_DATE_FIELD to trip.startDate,
                    END_DATE_FIELD to trip.endDate,
                    CITIES_FIELD to trip.cities,
                    STATUS_FIELD to trip.status.name,
                    GROUP_TRIP_FIELD to trip.isGroupTrip,
                    OWNER_ID_FIELD to installationId,
                    PARTICIPANTS_FIELD to participants,
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
            val updated = current + mapOf(
                PARTICIPANTS_FIELD to mergeParticipant(readParticipantMaps(current), remoteParticipant),
                UPDATED_AT_FIELD to FieldValue.serverTimestamp(),
            )
            transaction.set(reference, updated)
            updated
        }.awaitResult()

        joinedData.toSharedInvite(participant.id)
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
        const val PARTICIPANT_ID_FIELD = "id"
        const val PARTICIPANT_NICKNAME_FIELD = "nickname"
        const val PARTICIPANT_CHARACTER_FIELD = "characterKey"
        const val CREATED_AT_FIELD = "createdAt"
        const val UPDATED_AT_FIELD = "updatedAt"
        val INVITE_CODE_PATTERN = Regex("[A-Z0-9]{6}")
    }
}

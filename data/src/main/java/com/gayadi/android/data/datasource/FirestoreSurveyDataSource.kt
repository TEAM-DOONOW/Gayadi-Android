package com.gayadi.android.data.datasource

import com.gayadi.android.data.model.CompatibleTravelTypeDto
import com.gayadi.android.data.model.SurveyDefinitionDto
import com.gayadi.android.data.model.SurveyOptionDto
import com.gayadi.android.data.model.SurveyQuestionDto
import com.gayadi.android.data.model.SurveyResultDto
import com.gayadi.android.data.model.TravelRoleDto
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot

/** Reads the active travel survey and its eight results from Cloud Firestore. */
class FirestoreSurveyDataSource(
    private val firestore: FirebaseFirestore,
) : SurveyDataSource {
    override fun loadSurvey(callback: (Result<SurveyDefinitionDto>) -> Unit) {
        val surveyReference = firestore.collection(SURVEYS_COLLECTION).document(SURVEY_ID)
        surveyReference.get()
            .addOnSuccessListener { survey ->
                if (!survey.exists()) {
                    callback(Result.failure(IllegalStateException("설문 기준 데이터를 찾을 수 없습니다.")))
                    return@addOnSuccessListener
                }
                surveyReference.collection(QUESTIONS_COLLECTION)
                    .orderBy(ORDER_FIELD)
                    .get()
                    .addOnSuccessListener { questions ->
                        surveyReference.collection(RESULTS_COLLECTION)
                            .get()
                            .addOnSuccessListener { results ->
                                callback(runCatching { mapDefinition(survey, questions, results) })
                            }
                            .addOnFailureListener { callback(Result.failure(it)) }
                    }
                    .addOnFailureListener { callback(Result.failure(it)) }
            }
            .addOnFailureListener { callback(Result.failure(it)) }
    }

    override fun loadResult(code: String, callback: (Result<SurveyResultDto>) -> Unit) {
        firestore.collection(SURVEYS_COLLECTION)
            .document(SURVEY_ID)
            .collection(RESULTS_COLLECTION)
            .document(code)
            .get()
            .addOnSuccessListener { document ->
                callback(
                    runCatching {
                        require(document.exists()) { "결과 유형을 찾을 수 없습니다: $code" }
                        mapResult(document)
                    },
                )
            }
            .addOnFailureListener { callback(Result.failure(it)) }
    }

    private fun mapDefinition(
        survey: DocumentSnapshot,
        questionSnapshot: QuerySnapshot,
        resultSnapshot: QuerySnapshot,
    ): SurveyDefinitionDto {
        val questions = questionSnapshot.documents.map(::mapQuestion)
        val results = resultSnapshot.documents.map(::mapResult)
        val resultCodeOrder = stringList(survey, RESULT_CODE_ORDER_FIELD)

        require(questions.size == EXPECTED_QUESTION_COUNT) {
            "설문 문항은 ${EXPECTED_QUESTION_COUNT}개여야 합니다."
        }
        require(results.size == EXPECTED_RESULT_COUNT) {
            "설문 결과는 ${EXPECTED_RESULT_COUNT}개여야 합니다."
        }
        require(resultCodeOrder.size == EXPECTED_DIMENSION_COUNT) {
            "결과 코드 차원은 ${EXPECTED_DIMENSION_COUNT}개여야 합니다."
        }
        require(questions.map { it.order }.sorted() == (1..EXPECTED_QUESTION_COUNT).toList()) {
            "문항 order는 1부터 $EXPECTED_QUESTION_COUNT 사이에서 중복 없이 존재해야 합니다."
        }
        require(questions.groupingBy { it.dimension }.eachCount() == resultCodeOrder.associateWith { 3 }) {
            "각 결과 차원에는 문항이 3개씩 있어야 합니다."
        }
        require(results.map { it.code }.toSet() == EXPECTED_RESULT_CODES) {
            "8개 결과 코드 구성이 올바르지 않습니다."
        }

        return SurveyDefinitionDto(
            id = survey.id,
            title = requiredString(survey, TITLE_FIELD),
            resultCodeOrder = resultCodeOrder,
            questions = questions,
            results = results,
        )
    }

    private fun mapQuestion(document: DocumentSnapshot): SurveyQuestionDto {
        val rawOptions = document.get(OPTIONS_FIELD) as? List<*>
            ?: error("문항 ${document.id}의 선택지가 없습니다.")
        val options = rawOptions.mapIndexed { index, rawOption ->
            val option = rawOption as? Map<*, *>
                ?: error("문항 ${document.id}의 ${index + 1}번째 선택지가 올바르지 않습니다.")
            SurveyOptionDto(
                id = option[ID_FIELD] as? String ?: error("선택지 ID가 없습니다."),
                text = option[TEXT_FIELD] as? String ?: error("선택지 문구가 없습니다."),
                code = option[CODE_FIELD] as? String ?: error("선택지 코드가 없습니다."),
            )
        }
        require(options.size == EXPECTED_OPTION_COUNT) {
            "문항 ${document.id}의 선택지는 ${EXPECTED_OPTION_COUNT}개여야 합니다."
        }

        return SurveyQuestionDto(
            id = document.id,
            order = document.getLong(ORDER_FIELD)?.toInt() ?: error("문항 순서가 없습니다."),
            dimension = requiredString(document, DIMENSION_FIELD),
            title = requiredString(document, PROMPT_FIELD),
            options = options,
        )
    }

    private fun mapResult(document: DocumentSnapshot): SurveyResultDto {
        val code = requiredString(document, CODE_FIELD)
        require(code == document.id) { "결과 문서 ID와 code가 일치하지 않습니다: ${document.id}" }
        return SurveyResultDto(
            code = code,
            emoji = requiredString(document, EMOJI_FIELD),
            name = requiredString(document, NAME_FIELD),
            summary = requiredString(document, SUMMARY_FIELD),
            hashtags = optionalStringList(document, HASHTAGS_FIELD),
            strengths = optionalStringList(document, STRENGTHS_FIELD),
            weaknesses = optionalStringList(document, WEAKNESSES_FIELD),
            characterKey = document.getString(CHARACTER_KEY_FIELD)?.takeIf(String::isNotBlank),
            compatibleTypes = compatibleTypes(document),
            travelRole = travelRole(document),
        )
    }

    private fun compatibleTypes(document: DocumentSnapshot): List<CompatibleTravelTypeDto> =
        optionalMapList(document, COMPATIBLE_TYPES_FIELD).map { item ->
            CompatibleTravelTypeDto(
                code = item.requiredString(CODE_FIELD, document),
                emoji = item.requiredString(EMOJI_FIELD, document),
                name = item.requiredString(NAME_FIELD, document),
            )
        }

    private fun travelRole(document: DocumentSnapshot): TravelRoleDto? {
        val item = document.get(TRAVEL_ROLE_FIELD) as? Map<*, *> ?: return null
        return TravelRoleDto(
            icon = item.requiredString(ICON_FIELD, document),
            title = item.requiredString(TITLE_FIELD, document),
            description = item.requiredString(DESCRIPTION_FIELD, document),
        )
    }

    private fun optionalMapList(document: DocumentSnapshot, field: String): List<Map<*, *>> =
        (document.get(field) as? List<*>)
            ?.map { it as? Map<*, *> ?: error("${document.reference.path}의 $field 값이 올바르지 않습니다.") }
            .orEmpty()

    private fun Map<*, *>.requiredString(field: String, document: DocumentSnapshot): String =
        (get(field) as? String)?.takeIf(String::isNotBlank)
            ?: error("${document.reference.path}의 $field 값이 없습니다.")

    private fun requiredString(document: DocumentSnapshot, field: String): String =
        document.getString(field)?.takeIf(String::isNotBlank)
            ?: error("${document.reference.path}의 $field 값이 없습니다.")

    /**
     * Reads an optional list field, returning an empty list when it is absent.
     *
     * Result copy is presentational, so a document seeded before these fields existed hides the
     * matching section instead of failing the whole survey load.
     */
    private fun optionalStringList(document: DocumentSnapshot, field: String): List<String> =
        (document.get(field) as? List<*>)
            ?.mapNotNull { (it as? String)?.takeIf(String::isNotBlank) }
            .orEmpty()

    private fun stringList(document: DocumentSnapshot, field: String): List<String> =
        (document.get(field) as? List<*>)
            ?.map { it as? String ?: error("${document.reference.path}의 $field 값이 올바르지 않습니다.") }
            ?: error("${document.reference.path}의 $field 값이 없습니다.")

    private companion object {
        const val SURVEYS_COLLECTION = "surveys"
        const val SURVEY_ID = "travel-personality-v1"
        const val QUESTIONS_COLLECTION = "questions"
        const val RESULTS_COLLECTION = "results"
        const val ORDER_FIELD = "order"
        const val RESULT_CODE_ORDER_FIELD = "resultCodeOrder"
        const val TITLE_FIELD = "title"
        const val OPTIONS_FIELD = "options"
        const val ID_FIELD = "id"
        const val TEXT_FIELD = "text"
        const val CODE_FIELD = "code"
        const val DIMENSION_FIELD = "dimension"
        const val PROMPT_FIELD = "prompt"
        const val EMOJI_FIELD = "emoji"
        const val NAME_FIELD = "name"
        const val SUMMARY_FIELD = "summary"
        const val HASHTAGS_FIELD = "hashtags"
        const val STRENGTHS_FIELD = "strengths"
        const val WEAKNESSES_FIELD = "weaknesses"
        const val CHARACTER_KEY_FIELD = "characterKey"
        const val COMPATIBLE_TYPES_FIELD = "compatibleTypes"
        const val TRAVEL_ROLE_FIELD = "travelRole"
        const val ICON_FIELD = "icon"
        const val DESCRIPTION_FIELD = "description"
        const val EXPECTED_QUESTION_COUNT = 9
        const val EXPECTED_RESULT_COUNT = 8
        const val EXPECTED_DIMENSION_COUNT = 3
        const val EXPECTED_OPTION_COUNT = 2
        val EXPECTED_RESULT_CODES = setOf("PNA", "PNR", "PCA", "PCR", "SNA", "SNR", "SCA", "SCR")
    }
}

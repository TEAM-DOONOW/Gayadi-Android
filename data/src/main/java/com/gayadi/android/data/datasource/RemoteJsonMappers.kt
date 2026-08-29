package com.gayadi.android.data.datasource

import com.gayadi.android.data.model.CompatibleTravelTypeDto
import com.gayadi.android.data.model.LegalDocumentDto
import com.gayadi.android.data.model.LegalDocumentSectionDto
import com.gayadi.android.data.model.NoticeDto
import com.gayadi.android.data.model.NoticeSectionDto
import com.gayadi.android.data.model.SurveyDefinitionDto
import com.gayadi.android.data.model.SurveyOptionDto
import com.gayadi.android.data.model.SurveyQuestionDto
import com.gayadi.android.data.model.SurveyResultDto
import com.gayadi.android.data.model.TravelRoleDto
import org.json.JSONArray
import org.json.JSONObject

internal fun JSONObject.toNoticeDto(): NoticeDto {
    val subject = "공지"
    val sections = requiredArray("sections", subject).objects("$subject 섹션").map { section ->
        NoticeSectionDto(
            title = section.requiredString("title", "$subject 섹션"),
            body = section.requiredString("body", "$subject 섹션"),
        )
    }
    require(sections.isNotEmpty()) { "공지 본문 섹션이 없습니다." }

    return NoticeDto(
        id = requiredString("id", subject),
        title = requiredString("title", subject),
        category = optionalString("category", subject),
        version = optionalString("version", subject),
        publishedAt = requiredString("publishedAt", subject),
        summary = requiredString("summary", subject),
        sections = sections,
        isPinned = requiredBoolean("isPinned", subject),
    )
}

internal fun JSONObject.toLegalDocumentDto(): LegalDocumentDto {
    val subject = "법적 문서"
    val sections = requiredArray("sections", subject).objects("$subject 섹션").map { section ->
        LegalDocumentSectionDto(
            title = section.requiredString("title", "$subject 섹션"),
            body = section.requiredString("body", "$subject 섹션"),
        )
    }
    require(sections.isNotEmpty()) { "법적 문서 본문 섹션이 없습니다." }

    return LegalDocumentDto(
        id = requiredString("id", subject),
        title = requiredString("title", subject),
        version = requiredString("version", subject),
        effectiveDate = requiredString("effectiveDate", subject),
        summary = requiredString("summary", subject),
        sections = sections,
        reviewNotice = optionalString("reviewNotice", subject),
    )
}

internal fun JSONObject.toSurveyDefinitionDto(): SurveyDefinitionDto {
    val subject = "여행 성향 설문"
    val questions = requiredArray("questions", subject).objects("설문 문항").map(JSONObject::toSurveyQuestionDto)
    val results = requiredArray("results", subject).objects("설문 결과").map(JSONObject::toSurveyResultDto)
    val resultCodeOrder = requiredArray("resultCodeOrder", subject).strings("결과 코드 차원")

    require(questions.size == EXPECTED_QUESTION_COUNT) {
        "설문 문항은 ${EXPECTED_QUESTION_COUNT}개여야 합니다."
    }
    require(results.size == EXPECTED_RESULT_COUNT) {
        "설문 결과는 ${EXPECTED_RESULT_COUNT}개여야 합니다."
    }
    require(resultCodeOrder.size == EXPECTED_DIMENSION_COUNT) {
        "결과 코드 차원은 ${EXPECTED_DIMENSION_COUNT}개여야 합니다."
    }
    require(questions.map(SurveyQuestionDto::order).sorted() == (1..EXPECTED_QUESTION_COUNT).toList()) {
        "문항 order는 1부터 $EXPECTED_QUESTION_COUNT 사이에서 중복 없이 존재해야 합니다."
    }
    require(questions.groupingBy(SurveyQuestionDto::dimension).eachCount() == resultCodeOrder.associateWith { 3 }) {
        "각 결과 차원에는 문항이 3개씩 있어야 합니다."
    }
    require(results.map(SurveyResultDto::code).toSet() == EXPECTED_RESULT_CODES) {
        "8개 결과 코드 구성이 올바르지 않습니다."
    }

    return SurveyDefinitionDto(
        id = requiredString("id", subject),
        title = requiredString("title", subject),
        resultCodeOrder = resultCodeOrder,
        questions = questions,
        results = results,
    )
}

internal fun JSONObject.toSurveyResultDto(): SurveyResultDto {
    val subject = "설문 결과"
    val compatibleTypes = optionalArray("compatibleTypes", subject)
        ?.objects("호환 성향")
        ?.map { compatible ->
            CompatibleTravelTypeDto(
                code = compatible.requiredString("code", "호환 성향"),
                emoji = compatible.requiredString("emoji", "호환 성향"),
                name = compatible.requiredString("name", "호환 성향"),
            )
        }
        .orEmpty()
    val travelRole = optionalObject("travelRole", subject)
        ?.takeIf { it.length() > 0 }
        ?.let { role ->
            TravelRoleDto(
                icon = role.requiredString("icon", "여행 역할"),
                title = role.requiredString("title", "여행 역할"),
                description = role.requiredString("description", "여행 역할"),
            )
        }

    return SurveyResultDto(
        code = requiredString("code", subject),
        emoji = requiredString("emoji", subject),
        name = requiredString("name", subject),
        summary = requiredString("summary", subject),
        hashtags = optionalArray("hashtags", subject)?.strings("설문 결과 해시태그").orEmpty(),
        strengths = optionalArray("strengths", subject)?.strings("설문 결과 장점").orEmpty(),
        weaknesses = optionalArray("weaknesses", subject)?.strings("설문 결과 단점").orEmpty(),
        characterKey = optionalString("characterKey", subject),
        compatibleTypes = compatibleTypes,
        travelRole = travelRole,
    )
}

private fun JSONObject.toSurveyQuestionDto(): SurveyQuestionDto {
    val subject = "설문 문항"
    val options = requiredArray("options", subject).objects("설문 선택지").map { option ->
        SurveyOptionDto(
            id = option.requiredString("id", "설문 선택지"),
            text = option.requiredString("text", "설문 선택지"),
            code = option.requiredString("code", "설문 선택지"),
        )
    }
    require(options.size == EXPECTED_OPTION_COUNT) {
        "문항 ${requiredString("id", subject)}의 선택지는 ${EXPECTED_OPTION_COUNT}개여야 합니다."
    }

    return SurveyQuestionDto(
        id = requiredString("id", subject),
        order = requiredInt("order", subject),
        dimension = requiredString("dimension", subject),
        title = requiredString("title", subject),
        options = options,
    )
}

private fun JSONObject.requiredString(key: String, subject: String): String {
    val value = if (has(key) && !isNull(key)) opt(key) else null
    return (value as? String)?.takeIf(String::isNotBlank)
        ?: error("$subject 응답의 $key 값이 없습니다.")
}

private fun JSONObject.optionalString(key: String, subject: String): String? {
    if (!has(key) || isNull(key)) return null
    val value = opt(key)
    require(value is String) { "$subject 응답의 $key 값이 올바르지 않습니다." }
    return value.takeIf(String::isNotBlank)
}

private fun JSONObject.requiredInt(key: String, subject: String): Int {
    val value = if (has(key) && !isNull(key)) opt(key) else null
    require(value is Number) { "$subject 응답의 $key 값이 없습니다." }
    return value.toInt()
}

private fun JSONObject.requiredBoolean(key: String, subject: String): Boolean {
    val value = if (has(key) && !isNull(key)) opt(key) else null
    require(value is Boolean) { "$subject 응답의 $key 값이 없습니다." }
    return value
}

private fun JSONObject.requiredArray(key: String, subject: String): JSONArray =
    optionalArray(key, subject) ?: error("$subject 응답의 $key 값이 없습니다.")

private fun JSONObject.optionalArray(key: String, subject: String): JSONArray? {
    if (!has(key) || isNull(key)) return null
    return opt(key) as? JSONArray
        ?: error("$subject 응답의 $key 값이 올바르지 않습니다.")
}

private fun JSONObject.optionalObject(key: String, subject: String): JSONObject? {
    if (!has(key) || isNull(key)) return null
    return opt(key) as? JSONObject
        ?: error("$subject 응답의 $key 값이 올바르지 않습니다.")
}

private fun JSONArray.objects(subject: String): List<JSONObject> =
    List(length()) { index ->
        opt(index) as? JSONObject
            ?: error("$subject 응답의 ${index + 1}번째 항목이 올바르지 않습니다.")
    }

private fun JSONArray.strings(subject: String): List<String> =
    List(length()) { index ->
        (opt(index) as? String)?.takeIf(String::isNotBlank)
            ?: error("$subject 응답의 ${index + 1}번째 항목이 올바르지 않습니다.")
    }

private const val EXPECTED_QUESTION_COUNT = 9
private const val EXPECTED_RESULT_COUNT = 8
private const val EXPECTED_DIMENSION_COUNT = 3
private const val EXPECTED_OPTION_COUNT = 2
private val EXPECTED_RESULT_CODES = setOf("PNA", "PNR", "PCA", "PCR", "SNA", "SNR", "SCA", "SCR")

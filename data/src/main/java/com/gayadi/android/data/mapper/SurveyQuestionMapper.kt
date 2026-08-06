package com.gayadi.android.data.mapper

import com.gayadi.android.data.model.SurveyDefinitionDto
import com.gayadi.android.data.model.SurveyOptionDto
import com.gayadi.android.data.model.SurveyQuestionDto
import com.gayadi.android.data.model.SurveyResultDto
import com.gayadi.android.domain.model.SurveyDefinition
import com.gayadi.android.domain.model.SurveyOption
import com.gayadi.android.domain.model.SurveyQuestion
import com.gayadi.android.domain.model.SurveyResult

/** Converts raw survey data into domain models. */
fun SurveyQuestionDto.toDomain(): SurveyQuestion = SurveyQuestion(
    id = id,
    order = order,
    dimension = dimension,
    title = title,
    options = options.map(SurveyOptionDto::toDomain),
)

fun SurveyOptionDto.toDomain(): SurveyOption = SurveyOption(id, text, code)

fun SurveyResultDto.toDomain(): SurveyResult = SurveyResult(
    code = code,
    emoji = emoji,
    name = name,
    summary = summary,
    traits = traits,
    compatibleCode = compatibleCode,
    oppositeCode = oppositeCode,
    characterKey = characterKey,
)

fun SurveyDefinitionDto.toDomain(): SurveyDefinition = SurveyDefinition(
    id = id,
    title = title,
    resultCodeOrder = resultCodeOrder,
    questions = questions.sortedBy { it.order }.map(SurveyQuestionDto::toDomain),
    results = results.map(SurveyResultDto::toDomain).associateBy { it.code },
)

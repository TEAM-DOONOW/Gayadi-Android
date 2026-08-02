package com.gayadi.android.data.mapper

import com.gayadi.android.data.model.SurveyQuestionDto
import com.gayadi.android.domain.model.SurveyQuestion

/** Converts a survey DTO into the domain question model. */
fun SurveyQuestionDto.toDomain(): SurveyQuestion = SurveyQuestion(id, title, options)

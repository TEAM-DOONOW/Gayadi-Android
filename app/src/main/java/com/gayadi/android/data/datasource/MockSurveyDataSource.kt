package com.gayadi.android.data.datasource

import com.gayadi.android.data.model.SurveyQuestionDto

/** Supplies deterministic survey data while a remote API is unavailable. */
class MockSurveyDataSource : SurveyDataSource {
    /** Returns the local mock survey question set. */
    override fun getQuestions(): List<SurveyQuestionDto> = listOf(
        SurveyQuestionDto(
            id = 1,
            title = "여행을 가게 된다면\n가장 먼저 무엇을 하나요?",
            options = listOf(
                "여행 일정과 동선을 꼼꼼하게 계획한다.",
                "맛집이나 유명한 관광지를 먼저 찾아본다.",
                "숙소만 예약하고 나머지는 즉흥적으로 결정한다.",
                "같이 가는 사람들과 무엇을 할지 먼저 이야기한다.",
            ),
        ),
        SurveyQuestionDto(
            id = 2,
            title = "여행 중 예상치 못한\n상황이 생기면 어떻게 하나요?",
            options = listOf(
                "미리 세워둔 대안 일정을 바로 꺼낸다.",
                "현지인 추천이나 리뷰를 검색해 본다.",
                "그냥 흐름에 맡기고 즐긴다.",
                "동행들과 상의해서 함께 결정한다.",
            ),
        ),
        SurveyQuestionDto(
            id = 3,
            title = "나에게 여행에서\n가장 중요한 것은 무엇인가요?",
            options = listOf(
                "계획대로 움직이는 안정감",
                "새로운 경험과 발견",
                "편안한 휴식과 여유",
                "함께하는 사람들과의 시간",
            ),
        ),
    )
}

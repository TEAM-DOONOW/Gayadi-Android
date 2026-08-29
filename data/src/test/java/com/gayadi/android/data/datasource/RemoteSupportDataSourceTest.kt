package com.gayadi.android.data.datasource

import com.gayadi.android.data.model.InquiryDto
import com.gayadi.android.data.model.LegalDocumentDto
import com.gayadi.android.data.model.NoticeDto
import com.gayadi.android.data.model.SurveyAnswerDto
import com.gayadi.android.data.model.SurveyDefinitionDto
import com.gayadi.android.data.model.SurveyResultDto
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RemoteSupportDataSourceTest {
    @Test
    fun noticeList_usesPublicEndpointAndMapsResponse() = runTest {
        val client = RecordingRemoteJsonClient(
            getArrayResponse = JSONArray().put(noticeJson()),
        )
        var result: Result<List<NoticeDto>>? = null

        RemoteNoticeDataSource(client, this).loadNotices { result = it }
        advanceUntilIdle()

        val call = client.calls.single()
        assertEquals("GET_ARRAY", call.method)
        assertEquals("/api/v1/notices", call.path)
        assertEquals(mapOf("limit" to "100", "offset" to "0"), call.query)
        assertFalse(call.authenticated)
        assertEquals("점검 안내", result?.getOrThrow()?.single()?.title)
        assertEquals("본문", result?.getOrThrow()?.single()?.sections?.single()?.body)
    }

    @Test
    fun noticeDetail_rejectsUnsafeIdWithoutNetworkRequest() = runTest {
        val client = RecordingRemoteJsonClient(getObjectResponse = noticeJson())
        var result: Result<NoticeDto>? = null

        RemoteNoticeDataSource(client, this).loadNotice("../private") { result = it }
        advanceUntilIdle()

        assertTrue(result?.isFailure == true)
        assertEquals("공지 식별자가 올바르지 않습니다.", result?.exceptionOrNull()?.message)
        assertTrue(client.calls.isEmpty())
    }

    @Test
    fun legalDocument_usesPublicEndpointAndMapsNullableReviewNotice() = runTest {
        val client = RecordingRemoteJsonClient(getObjectResponse = legalDocumentJson())
        var result: Result<LegalDocumentDto>? = null

        RemoteLegalDocumentDataSource(client, this).loadDocument("privacy-policy") { result = it }
        advanceUntilIdle()

        val call = client.calls.single()
        assertEquals("/api/v1/legal-documents/privacy-policy", call.path)
        assertFalse(call.authenticated)
        assertEquals("2026-08-12", result?.getOrThrow()?.effectiveDate)
        assertNull(result?.getOrThrow()?.reviewNotice)
    }

    @Test
    fun inquiry_postsServerContractWithAuthentication() = runTest {
        val client = RecordingRemoteJsonClient(postObjectResponse = JSONObject().put("id", 42L))
        var result: Result<Unit>? = null

        RemoteInquiryDataSource(client, this).submit(
            InquiryDto("bug", "지도 오류", "장소 검색 화면이 비어 있습니다.", "user@example.com"),
        ) { result = it }
        advanceUntilIdle()

        val call = client.calls.single()
        assertEquals("POST_OBJECT", call.method)
        assertEquals("/api/v1/inquiries", call.path)
        assertTrue(call.authenticated)
        assertEquals("bug", call.body?.getString("category"))
        assertEquals("지도 오류", call.body?.getString("title"))
        assertEquals("장소 검색 화면이 비어 있습니다.", call.body?.getString("message"))
        assertEquals("user@example.com", call.body?.getString("contactEmail"))
        assertTrue(result?.isSuccess == true)
    }

    @Test
    fun surveyLoad_mapsAggregateAndUsesPublicEndpoint() = runTest {
        val client = RecordingRemoteJsonClient(getObjectResponse = surveyJson())
        var result: Result<SurveyDefinitionDto>? = null

        RemoteSurveyDataSource(client, this).loadSurvey { result = it }
        advanceUntilIdle()

        val definition = result?.getOrThrow()
        assertEquals("/api/v1/surveys/travel-personality-v1", client.calls.single().path)
        assertFalse(client.calls.single().authenticated)
        assertEquals(9, definition?.questions?.size)
        assertEquals(listOf("preparation", "place", "energy"), definition?.resultCodeOrder)
        assertEquals("a", definition?.questions?.first()?.options?.first()?.id)
        assertEquals("코스 대장", definition?.results?.first()?.travelRole?.title)
    }

    @Test
    fun surveyResult_normalizesCodeAndMapsOptionalDetails() = runTest {
        val client = RecordingRemoteJsonClient(getObjectResponse = resultJson("PNA", detailed = false))
        var result: Result<SurveyResultDto>? = null

        RemoteSurveyDataSource(client, this).loadResult(" pna ") { result = it }
        advanceUntilIdle()

        assertEquals(
            "/api/v1/surveys/travel-personality-v1/results/PNA",
            client.calls.single().path,
        )
        assertFalse(client.calls.single().authenticated)
        assertEquals("PNA", result?.getOrThrow()?.code)
        assertTrue(result?.getOrThrow()?.compatibleTypes?.isEmpty() == true)
        assertNull(result?.getOrThrow()?.travelRole)
    }

    @Test
    fun surveySubmission_postsQuestionAndOptionIdsWithAuthentication() = runTest {
        val client = RecordingRemoteJsonClient(
            postObjectResponse = JSONObject().put("result", resultJson("PNR")),
        )
        var result: Result<SurveyResultDto>? = null

        RemoteSurveyDataSource(client, this).submitAnswers(
            listOf(SurveyAnswerDto(questionId = "q01", optionId = "b")),
        ) { result = it }
        advanceUntilIdle()

        val call = client.calls.single()
        assertEquals("/api/v1/surveys/travel-personality-v1/submissions", call.path)
        assertTrue(call.authenticated)
        val submitted = call.body?.getJSONArray("answers")?.getJSONObject(0)
        assertEquals("q01", submitted?.getString("questionId"))
        assertEquals("b", submitted?.getString("optionId"))
        assertEquals("PNR", result?.getOrThrow()?.code)
    }

    private fun noticeJson() = JSONObject()
        .put("id", "maintenance-1")
        .put("title", "점검 안내")
        .put("category", "notice")
        .put("version", JSONObject.NULL)
        .put("publishedAt", "2026-08-30T10:00:00")
        .put("summary", "서비스 점검 안내")
        .put("sections", JSONArray().put(JSONObject().put("title", "일정").put("body", "본문")))
        .put("isPinned", true)

    private fun legalDocumentJson() = JSONObject()
        .put("id", "privacy-policy")
        .put("title", "개인정보처리방침")
        .put("version", "1.0.0")
        .put("effectiveDate", "2026-08-12")
        .put("publicationStatus", "PUBLISHED")
        .put("summary", "개인정보 처리 안내")
        .put("sections", JSONArray().put(JSONObject().put("title", "처리 목적").put("body", "여행 기능 제공")))
        .put("reviewNotice", JSONObject.NULL)

    private fun surveyJson(): JSONObject {
        val dimensions = listOf("preparation", "place", "energy")
        val questions = JSONArray()
        repeat(9) { index ->
            questions.put(
                JSONObject()
                    .put("id", "q%02d".format(index + 1))
                    .put("title", "질문 ${index + 1}")
                    .put("dimension", dimensions[index / 3])
                    .put("order", index + 1)
                    .put(
                        "options",
                        JSONArray()
                            .put(JSONObject().put("id", "a").put("text", "왼쪽").put("code", "L"))
                            .put(JSONObject().put("id", "b").put("text", "오른쪽").put("code", "R")),
                    ),
            )
        }
        return JSONObject()
            .put("id", "travel-personality-v1")
            .put("title", "여행 성향 설문")
            .put("resultCodeOrder", JSONArray(dimensions))
            .put("questions", questions)
            .put("results", JSONArray(RESULT_CODES.map { resultJson(it) }))
    }

    private fun resultJson(code: String, detailed: Boolean = true) = JSONObject()
        .put("code", code)
        .put("emoji", "아이콘")
        .put("name", "$code 여행가")
        .put("summary", "결과 설명")
        .put("characterKey", JSONObject.NULL)
        .put("hashtags", JSONArray(listOf("#여행")))
        .put("strengths", JSONArray(listOf("계획")))
        .put("weaknesses", JSONArray(listOf("걱정")))
        .put(
            "compatibleTypes",
            if (detailed) {
                JSONArray().put(JSONObject().put("code", "PNR").put("emoji", "호환").put("name", "힐링잉"))
            } else {
                JSONArray()
            },
        )
        .put(
            "travelRole",
            if (detailed) {
                JSONObject().put("icon", "나침반").put("title", "코스 대장").put("description", "동선을 만들어요")
            } else {
                JSONObject()
            },
        )

    private data class Call(
        val method: String,
        val path: String,
        val query: Map<String, String?>,
        val authenticated: Boolean,
        val body: JSONObject? = null,
    )

    private class RecordingRemoteJsonClient(
        private val getObjectResponse: JSONObject = JSONObject(),
        private val getArrayResponse: JSONArray = JSONArray(),
        private val postObjectResponse: JSONObject = JSONObject(),
    ) : RemoteJsonClient {
        val calls = mutableListOf<Call>()

        override suspend fun getObject(
            path: String,
            query: Map<String, String?>,
            authenticated: Boolean,
        ): JSONObject {
            calls += Call("GET_OBJECT", path, query, authenticated)
            return getObjectResponse
        }

        override suspend fun getArray(
            path: String,
            query: Map<String, String?>,
            authenticated: Boolean,
        ): JSONArray {
            calls += Call("GET_ARRAY", path, query, authenticated)
            return getArrayResponse
        }

        override suspend fun postObject(
            path: String,
            body: JSONObject,
            authenticated: Boolean,
        ): JSONObject {
            calls += Call("POST_OBJECT", path, emptyMap(), authenticated, body)
            return postObjectResponse
        }
    }

    private companion object {
        val RESULT_CODES = listOf("PNA", "PNR", "PCA", "PCR", "SNA", "SNR", "SCA", "SCR")
    }
}

package com.gayadi.android.data

import com.gayadi.android.data.datasource.RestPublicContentDataSource
import com.gayadi.android.data.model.LegalDocumentDto
import com.gayadi.android.data.model.NoticeDto
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class PublicContentApiTest {
    private lateinit var server: MockWebServer
    private lateinit var source: RestPublicContentDataSource

    @Before fun setUp() {
        server = MockWebServer()
        server.start()
        source = RestPublicContentDataSource(server.url("/").toString())
    }

    @After fun tearDown() { server.shutdown() }

    @Test fun `notice detail maps server pinned flag date and nullable version`() {
        server.enqueue(json(notice("welcome")))
        val result = awaitResult<NoticeDto> { source.loadNotice("welcome", it) }.getOrThrow()
        assertEquals("/api/v1/notices/welcome", server.takeRequest().path)
        assertEquals("welcome", result.id)
        assertTrue(result.isPinned)
        assertNull(result.version)
        assertEquals("2026-09-07T14:00:00", result.publishedAt)
        assertEquals("내용", result.sections.single().body)
    }

    @Test fun `notice list fetches all pages and preserves server order`() {
        server.enqueue(json((0 until 100).joinToString(",", "[", "]") { notice("n$it") }))
        server.enqueue(json("[${notice("last")}]"))
        val result = awaitResult<List<NoticeDto>>(source::loadNotices).getOrThrow()
        assertEquals(101, result.size)
        assertEquals("n0", result.first().id)
        assertEquals("last", result.last().id)
        assertEquals("/api/v1/notices?limit=100&offset=0", server.takeRequest().path)
        assertEquals("/api/v1/notices?limit=100&offset=100", server.takeRequest().path)
    }

    @Test fun `empty notices are a successful empty state`() {
        server.enqueue(json("[]"))
        assertTrue(awaitResult<List<NoticeDto>>(source::loadNotices).getOrThrow().isEmpty())
    }

    @Test fun `later page failure does not return partial notices`() {
        server.enqueue(json((0 until 100).joinToString(",", "[", "]") { notice("n$it") }))
        server.enqueue(MockResponse().setResponseCode(500).setBody("internal diagnostics"))
        assertTrue(awaitResult<List<NoticeDto>>(source::loadNotices).isFailure)
    }

    @Test fun `legal documents accept publication status and optional review notice`() {
        for (id in listOf("terms-of-service", "privacy-policy")) {
            server.enqueue(json("""{
                "id":"$id","title":"문서","version":"2.0.0",
                "effectiveDate":"2026-09-07","publicationStatus":"PUBLISHED",
                "summary":"요약","sections":[{"title":"항목","body":"내용"}],
                "reviewNotice":null
            }"""))
            val document = awaitResult<LegalDocumentDto> { source.loadDocument(id, it) }.getOrThrow()
            assertEquals("/api/v1/legal-documents/$id", server.takeRequest().path)
            assertEquals(id, document.id)
            assertNull(document.reviewNotice)
            assertEquals("2026-09-07", document.effectiveDate)
        }
    }

    @Test fun `http failures empty bodies and malformed json use safe Korean errors`() {
        val responses = listOf(
            MockResponse().setResponseCode(404).setBody("private server diagnostics"),
            MockResponse().setResponseCode(429),
            MockResponse().setResponseCode(503),
            MockResponse().setResponseCode(204),
            json("{broken"),
            json("{\"id\":\"missing-required-fields\"}"),
        )
        for (response in responses) {
            server.enqueue(response)
            val result = awaitResult<LegalDocumentDto> { source.loadDocument("privacy-policy", it) }
            assertTrue(result.isFailure)
            assertEquals("문서를 불러오지 못했어요. 잠시 후 다시 시도해 주세요.", result.exceptionOrNull()?.message)
        }
    }

    @Test fun `path traversal and blank identifiers fail before network access`() {
        for (id in listOf("", "../notices", "x?limit=100", "a/b")) {
            assertTrue(awaitResult<NoticeDto> { source.loadNotice(id, it) }.isFailure)
            assertTrue(awaitResult<LegalDocumentDto> { source.loadDocument(id, it) }.isFailure)
        }
        assertEquals(0, server.requestCount)
    }

    private fun notice(id: String) = """{
        "id":"$id","title":"공지","category":"update","version":null,
        "publishedAt":"2026-09-07T14:00:00","summary":"요약",
        "sections":[{"title":"안내","body":"내용"}],"isPinned":true
    }"""

    private fun json(body: String) = MockResponse().setHeader("Content-Type", "application/json").setBody(body)

    private fun <T> awaitResult(action: ((Result<T>) -> Unit) -> Unit): Result<T> {
        val latch = CountDownLatch(1)
        var result: Result<T>? = null
        action { result = it; latch.countDown() }
        assertTrue("Callback was not delivered", latch.await(10, TimeUnit.SECONDS))
        return requireNotNull(result)
    }
}

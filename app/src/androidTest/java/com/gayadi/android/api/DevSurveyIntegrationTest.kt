package com.gayadi.android.api

import android.content.Intent
import android.graphics.Bitmap
import android.os.SystemClock
import android.view.accessibility.AccessibilityNodeInfo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.gayadi.android.BuildConfig
import com.gayadi.android.MainActivity
import com.gayadi.android.data.datasource.GayadiApiClient
import com.gayadi.android.data.datasource.HttpAuthApiDataSource
import com.gayadi.android.data.datasource.HttpProfileApiDataSource
import com.gayadi.android.data.datasource.RestInquiryDataSource
import com.gayadi.android.data.datasource.RestPublicContentDataSource
import com.gayadi.android.data.datasource.RestSessionApiDataSource
import com.gayadi.android.data.model.InquiryDto
import com.gayadi.android.data.repository.DefaultAuthRepository
import com.gayadi.android.data.repository.EncryptedFileAuthSessionStore
import com.gayadi.android.domain.model.AuthSession
import com.gayadi.android.domain.model.AuthUser
import com.gayadi.android.domain.model.BasicInfo
import java.io.File
import java.util.UUID
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Opt-in only: creates and deletes one temporary account on the configured dev server. */
@RunWith(AndroidJUnit4::class)
class DevSurveyIntegrationTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val automation get() = instrumentation.uiAutomation

    @Test fun surveyAnswersAreSavedAndRendered() = runBlocking {
        assumeTrue(InstrumentationRegistry.getArguments().getString("liveApi") == "true")
        check(BuildConfig.FLAVOR == "dev")
        val store = EncryptedFileAuthSessionStore(File(context.filesDir, "auth-session"))
        check(store.load() == null) { "Use a fresh emulator without an existing account session." }
        val publicApi = GayadiApiClient(BuildConfig.API_BASE_URL)
        val registration = JSONObject(publicApi.request("POST", "/api/v1/auth/registrations",
            JSONObject().put("email", "android-api-${UUID.randomUUID()}@example.invalid")
                .put("password", UUID.randomUUID().toString())
                .put("nickname", "연동테스트").toString(), authenticated = false))
        val user = registration.getJSONObject("user")
        val session = AuthSession(registration.getString("accessToken"), "Bearer",
            registration.getLong("expiresIn"), registration.getString("refreshToken"),
            registration.getLong("refreshExpiresIn"), System.currentTimeMillis() / 1000,
            AuthUser(user.getLong("id"), user.getString("nickname"), user.getString("email")))
        store.save(session)
        val auth = DefaultAuthRepository(HttpAuthApiDataSource(BuildConfig.API_BASE_URL), store)
        val api = GayadiApiClient(BuildConfig.API_BASE_URL, auth)
        try {
            HttpProfileApiDataSource(api).updateCurrentUser(BasicInfo("연동테스트", "개발 서버 연동 확인"))
            val refreshedSession = auth.refreshSession()
            assertNotEquals(session.refreshToken, refreshedSession.refreshToken)

            val publicContent = RestPublicContentDataSource(BuildConfig.API_BASE_URL)
            val notices = awaitResult(publicContent::loadNotices)
            assertTrue(notices.isNotEmpty())
            assertEquals(notices.first().id, awaitResult {
                publicContent.loadNotice(notices.first().id, it)
            }.id)
            listOf("terms-of-service", "privacy-policy").forEach { documentId ->
                assertEquals(documentId, awaitResult {
                    publicContent.loadDocument(documentId, it)
                }.id)
            }
            awaitResult { callback ->
                RestInquiryDataSource(api, this).submit(
                    InquiryDto(
                        category = "ETC",
                        title = "Android 연동 테스트",
                        message = "에뮬레이터 API 왕복 검증",
                        contactEmail = "android-api@example.invalid",
                    ),
                    callback,
                )
            }

            val survey = JSONObject(publicApi.request("GET", "/api/v1/surveys/travel-personality-v1",
                authenticated = false)).getJSONArray("questions")
            val activity = instrumentation.startActivitySync(Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
            try {
                click("테스트 시작하기")
                awaitText(survey.getJSONObject(0).getString("title"))
                screenshot("survey-question")
                for (index in 0 until survey.length()) {
                    val question = survey.getJSONObject(index)
                    click(question.getJSONArray("options").getJSONObject(0).getString("text"))
                    click(if (index == survey.length() - 1) "결과 보기" else "다음")
                }
                awaitText("가야디 시작하기")
                val profile = HttpProfileApiDataSource(api).currentUser()
                assertFalse(profile.resultCode.isNullOrBlank())
                assertFalse(profile.characterKey.isNullOrBlank())
                awaitText(requireNotNull(profile.travelStyleName))
                screenshot("survey-result")
                click("가야디 시작하기")
                awaitText("나의 여행")
                screenshot("survey-complete")
                click("설정")
                awaitText("공지사항")
                screenshot("settings-profile")
                repeat(3) {
                    nodes().firstOrNull { it.isScrollable }
                        ?.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
                    SystemClock.sleep(250)
                }
                awaitText("회원 탈퇴")
                screenshot("settings-actions")
                click("회원 탈퇴")
                awaitText("탈퇴하기")
                screenshot("delete-confirmation")
                click("취소")
                click("문의하기")
                awaitText("문의하기")
                screenshot("inquiry")
            } finally {
                instrumentation.runOnMainSync { activity.finish() }
            }
            RestSessionApiDataSource(api).logout(requireNotNull(auth.currentSession()).refreshToken)
        } finally {
            // Delete only the account created in this test. Never print tokens or passwords.
            try { HttpProfileApiDataSource(api).deleteCurrentUser() } finally {
                store.clear()
                File(context.filesDir, "user-profile.xml").delete()
            }
        }
    }

    private suspend fun <T> awaitResult(start: ((Result<T>) -> Unit) -> Unit): T {
        val result = CompletableDeferred<Result<T>>()
        start { result.complete(it) }
        return result.await().getOrThrow()
    }

    private fun awaitText(text: String): AccessibilityNodeInfo {
        val until = SystemClock.uptimeMillis() + 30_000
        while (SystemClock.uptimeMillis() < until) {
            val visibleNodes = nodes()
            var guideClose = visibleNodes.firstOrNull { it.contentDescription == "사용 안내 닫기" }
            if (guideClose != null) {
                while (guideClose != null && !guideClose.isClickable) guideClose = guideClose.parent
                guideClose?.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                SystemClock.sleep(250)
                continue
            }
            visibleNodes.firstOrNull {
                it.isVisibleToUser && (it.text?.toString() == text ||
                    it.contentDescription?.toString() == text)
            }?.let { return it }
            SystemClock.sleep(200)
        }
        screenshot("failure")
        error("Expected UI text was not shown: $text")
    }

    private fun nodes(): List<AccessibilityNodeInfo> {
        val result = mutableListOf<AccessibilityNodeInfo>()
        fun visit(node: AccessibilityNodeInfo?) {
            if (node == null) return
            result += node
            for (index in 0 until node.childCount) visit(node.getChild(index))
        }
        visit(automation.rootInActiveWindow)
        return result
    }

    private fun click(text: String) {
        var node: AccessibilityNodeInfo? = awaitText(text)
        while (node != null && !node.isClickable) node = node.parent
        check(node?.performAction(AccessibilityNodeInfo.ACTION_CLICK) == true) { "Cannot click: $text" }
        instrumentation.waitForIdleSync()
        SystemClock.sleep(350)
    }

    private fun screenshot(name: String) {
        val suffix = InstrumentationRegistry.getArguments().getString("screenshotSuffix").orEmpty()
        val file = File(context.getExternalFilesDir(null), "api-screenshots/$name$suffix.png")
        file.parentFile?.mkdirs()
        val bitmap = requireNotNull(automation.takeScreenshot())
        file.outputStream().use { assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
        bitmap.recycle()
    }
}

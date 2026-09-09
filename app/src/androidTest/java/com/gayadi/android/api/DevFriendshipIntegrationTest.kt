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
import com.gayadi.android.data.repository.DefaultAuthRepository
import com.gayadi.android.data.repository.EncryptedFileAuthSessionStore
import com.gayadi.android.domain.model.AuthSession
import com.gayadi.android.domain.model.AuthUser
import com.gayadi.android.domain.model.BasicInfo
import com.gayadi.android.data.remote.travel.ServerFriendshipGateway
import com.gayadi.android.domain.model.*
import com.gayadi.android.domain.repository.*
import java.io.File
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Opt-in dev integration. Mutates only two temporary accounts and their trip. */
@RunWith(AndroidJUnit4::class)
class DevFriendshipIntegrationTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val automation get() = instrumentation.uiAutomation

    @Test fun cleanupInterruptedTest() = runBlocking {
        assumeTrue(InstrumentationRegistry.getArguments().getString("cleanupInterruptedTest") == "true")
        check(BuildConfig.FLAVOR == "dev" && BuildConfig.API_BASE_URL == "http://223.130.134.57:8080")
        val store = EncryptedFileAuthSessionStore(File(context.filesDir, "auth-session"))
        val session = requireNotNull(store.load())
        check(session.user.nickname == "친구테스트" && session.user.email.startsWith("travel-") && session.user.email.endsWith("@example.invalid"))
        val auth = DefaultAuthRepository(HttpAuthApiDataSource(BuildConfig.API_BASE_URL), store)
        val api = GayadiApiClient(BuildConfig.API_BASE_URL, auth)
        val travel = com.gayadi.android.data.remote.travel.ServerTravelGateway(api)
        val trips = travel.listTrips()
        check(trips.all { it.name == "친구 초대 여행" && it.ownerId == session.user.id.toString() })
        trips.forEach { travel.deleteTrip(it.id) }
        HttpProfileApiDataSource(api).deleteCurrentUser()
        store.clear()
        File(context.filesDir, "user-profile.xml").delete()
        Unit
    }

    @Test fun friendshipRoundTrip() = runBlocking {
        assumeTrue(InstrumentationRegistry.getArguments().getString("liveApi") == "true")
        check(BuildConfig.FLAVOR == "dev" && BuildConfig.API_BASE_URL == "http://223.130.134.57:8080")
        val store = EncryptedFileAuthSessionStore(File(context.filesDir, "auth-session"))
        check(store.load() == null)
        val accounts = mutableListOf<AuthRepository>()
        val publicApi = GayadiApiClient(BuildConfig.API_BASE_URL)
        fun client(auth: AuthRepository) = GayadiApiClient(BuildConfig.API_BASE_URL, auth)
        suspend fun register(name: String): AuthRepository {
            val json = JSONObject(publicApi.request("POST", "/api/v1/auth/registrations",
                JSONObject().put("email", "travel-${UUID.randomUUID()}@example.invalid")
                    .put("password", UUID.randomUUID().toString()).put("nickname", name).toString(), false))
            val user = json.getJSONObject("user")
            val session = AuthSession(json.getString("accessToken"), "Bearer", json.getLong("expiresIn"),
                json.getString("refreshToken"), json.getLong("refreshExpiresIn"), System.currentTimeMillis()/1000,
                AuthUser(user.getLong("id"), name, user.getString("email")))
            val auth = object : AuthRepository {
                override fun currentSession() = session
                override fun clearSession() {}
                override suspend fun validAccessToken() = session.accessToken
                override suspend fun refreshSession() = error("Unexpected expiry during test")
                override suspend fun signInWithGoogle(idToken: String) = error("Unused")
            }
            accounts += auth
            return auth
        }
        fun step(name: String) { instrumentation.sendStatus(0, android.os.Bundle().apply { putString("stream", "STEP $name\n") }) }
        var tripId: String? = null
        var travel: com.gayadi.android.data.remote.travel.ServerTravelGateway? = null
        try {
            step("registration")
            val owner = register("친구테스트")
            val guest = register("동행친구테스트")
            val a = ServerFriendshipGateway(client(owner))
            val b = ServerFriendshipGateway(client(guest))
            val guestId = guest.currentSession()!!.user.id.toString()
            step("invitations")
            travel = com.gayadi.android.data.remote.travel.ServerTravelGateway(client(owner))
            val guestTravel = com.gayadi.android.data.remote.travel.ServerTravelGateway(client(guest))
            val trip = travel.createTrip(CreateTripCommand("친구 초대 여행", "2026.10.10", "2026.10.11", listOf("서울")))
            tripId = trip.id
            val cancelled = travel.createInvitation(trip.id, guestId)
            travel.updateInvitationStatus(trip.id, cancelled.id, InvitationDecision.CANCELLED)
            val declined = travel.createInvitation(trip.id, guestId)
            guestTravel.updateInvitationStatus(trip.id, declined.id, InvitationDecision.DECLINED)
            val accepted = travel.createInvitation(trip.id, guestId)
            assertEquals(8, accepted.code.length)
            assertEquals(trip.id, guestTravel.joinTrip(accepted.code).trip.id)
            assertTrue(travel.listInvitations(trip.id).any { it.id == accepted.id && it.status == InvitationStatus.ACCEPTED })
            step("friendships")
            assertTrue(a.search("동행친구테스트").any { it.id == guestId })
            a.request(guestId)
            val incoming = b.list().single()
            assertTrue(incoming.canDecide)
            assertFalse(incoming.requestedByMe)
            b.decide(incoming, true)
            assertEquals("ACCEPTED", a.list().single().status)
            HttpProfileApiDataSource(client(owner)).updateCurrentUser(BasicInfo("친구테스트", "친구 API 확인"))
            val questions = JSONObject(publicApi.request("GET", "/api/v1/surveys/travel-personality-v1", authenticated = false)).getJSONArray("questions")
            val answers = org.json.JSONArray()
            repeat(questions.length()) { i ->
                val q = questions.getJSONObject(i)
                answers.put(JSONObject().put("questionId", q.getString("id"))
                    .put("optionId", q.getJSONArray("options").getJSONObject(0).getString("id")))
            }
            client(owner).request("POST", "/api/v1/surveys/travel-personality-v1/submissions", JSONObject().put("answers", answers).toString())
            store.save(owner.currentSession()!!)
            step("ui")
            val activity = instrumentation.startActivitySync(Intent(Intent.ACTION_VIEW,
                android.net.Uri.parse("gayadi://invite/ABC123"), context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
            try {
                awaitText("함께할 여행메이트")
                awaitText("동행친구테스트")
                screenshot("friends-accepted")
            } finally { instrumentation.runOnMainSync { activity.finish() } }
            val tripActivity = instrumentation.startActivitySync(Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
            try {
                click("친구 초대 여행")
                click("함께하는 친구 2명 보기")
                awaitText("사용자 지정 초대")
                awaitText("동행친구테스트 · 수락됨")
                screenshot("invitations")
            } finally { instrumentation.runOnMainSync { tripActivity.finish() } }
            a.delete(a.list().single().id)
            assertTrue(a.list().isEmpty())
            a.request(guestId)
            b.decide(b.list().single(), false)
            assertEquals("REJECTED", a.list().single().status)
            a.delete(a.list().single().id)
            a.request(guestId)
            a.delete(a.list().single().id)
            assertTrue(b.list().isEmpty())
        } finally {
            step("cleanup")
            val cleanupErrors = mutableListOf<Throwable>()
            val tripRemoved = runCatching { tripId?.let { travel!!.deleteTrip(it) } }.onFailure(cleanupErrors::add).isSuccess
            if (tripRemoved) accounts.asReversed().forEach { account ->
                runCatching { HttpProfileApiDataSource(client(account)).deleteCurrentUser() }.onFailure(cleanupErrors::add)
            }
            step(if(cleanupErrors.isEmpty()) "cleanup-complete" else "cleanup-failed")
            store.clear()
            File(context.filesDir, "user-profile.xml").delete()
            check(cleanupErrors.isEmpty()) { "Temporary test data cleanup failed; dev connectivity must be checked" }
        }
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
            if (until - SystemClock.uptimeMillis() < 28_000) {
                visibleNodes.firstOrNull { it.isScrollable }
                    ?.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
            }
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
        instrumentation.waitForIdleSync()
        SystemClock.sleep(2000)
        val suffix = InstrumentationRegistry.getArguments().getString("screenshotSuffix").orEmpty()
        val file = File(context.getExternalFilesDir(null), "api-screenshots/$name$suffix.png")
        file.parentFile?.mkdirs()
        val bitmap = requireNotNull(automation.takeScreenshot())
        file.outputStream().use { assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
        bitmap.recycle()
    }
}

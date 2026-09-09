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
import com.gayadi.android.data.remote.travel.ServerTravelGateway
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
class DevTravelIntegrationTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val automation get() = instrumentation.uiAutomation

    @Test fun sharedTravelRoundTrip() = runBlocking {
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
        var cleanupTripId: String? = null
        try {
            val owner = register("여행테스트")
            val guest = register("동행테스트")
            val api = client(owner)
            val a = ServerTravelGateway(api)
            val b = ServerTravelGateway(client(guest))
            val ownerId = owner.currentSession()!!.user.id.toString()
            val guestId = guest.currentSession()!!.user.id.toString()
            var trip = a.createTrip(CreateTripCommand("서울 연동 여행", "2026.10.10", "2026.10.12", listOf("서울")))
            cleanupTripId = trip.id
            assertTrue(a.listTrips().any { it.id == trip.id })
            trip = a.updateTrip(trip.id, UpdateTripCommand("서울 함께 여행", trip.startDate, trip.endDate, trip.cities, trip.version))
            val joined = b.joinTrip(trip.inviteCode)
            assertEquals(trip.id, joined.trip.id)
            assertEquals(setOf(ownerId, guestId), a.listParticipants(trip.id).map { it.id }.toSet())
            instrumentation.sendStatus(0, android.os.Bundle().apply { putString("stream", "STEP dates\n") })
            val dates = listOf("2026.10.10", "2026.10.11", "2026.10.12")
            a.submitDateAvailability(trip.id, dates)
            b.submitDateAvailability(trip.id, dates)
            assertEquals(3, a.getDateCoordination(trip.id).commonDates.size)
            a.finalizeTripDates(trip.id, dates.first(), dates.last())
            assertEquals(dates.first(), b.getTrip(trip.id).startDate)
            instrumentation.sendStatus(0, android.os.Bundle().apply { putString("stream", "STEP places\n") })
            val places = JSONObject(publicApi.request("GET", "/api/v1/places?limit=1", authenticated=false)).getJSONArray("items")
            assertTrue(places.length() > 0)
            val placeId = places.getJSONObject(0).getLong("id").toString()
            a.saveFavoritePlace(placeId)
            assertTrue(placeId in a.listFavoritePlaceIds())
            a.deleteFavoritePlace(placeId)
            assertFalse(placeId in a.listFavoritePlaceIds())
            instrumentation.sendStatus(0, android.os.Bundle().apply { putString("stream", "STEP schedules\n") })
            var schedule = a.createSchedule(trip.id, TravelSchedule("draft", trip.id, "서울 나들이", placeId,
                dates.first(), "10:00", ScheduleType.MAIN, 0))
            val second = a.createSchedule(trip.id, schedule.copy(id="draft-2", title="점심 약속", time="12:00", placeId=null))
            assertEquals(2, b.listSchedules(trip.id).size)
            schedule = a.updateSchedule(trip.id, schedule.id, SchedulePatch(memo="함께 둘러보기", isVisited=true))
            assertTrue(b.listSchedules(trip.id).first { it.id == schedule.id }.isVisited)
            assertEquals(second.id, a.reorderSchedules(trip.id, listOf(second.id, schedule.id)).minBy { it.order }.id)
            instrumentation.sendStatus(0, android.os.Bundle().apply { putString("stream", "STEP fund\n") })
            a.contributeSharedFund(trip.id, 100000)
            assertEquals(100000L, b.getSharedFund(trip.id).balance)
            instrumentation.sendStatus(0, android.os.Bundle().apply { putString("stream", "STEP expense\n") })
            var expense = a.createExpense(trip.id, TravelExpense("draft", trip.id, schedule.id, "함께 먹은 점심", "", 30000,
                ownerId, listOf(ownerId, guestId), dates.first(), "12:00", ExpenseCategory.FOOD, ExpensePaymentSource.PERSONAL))
            expense = a.updateExpense(trip.id, expense.copy(amount=40000))
            assertEquals(40000L, b.listExpenses(trip.id).single().amount)
            assertEquals(40000L, b.getExpenseSettlement(trip.id).totalAmount)
            instrumentation.sendStatus(0, android.os.Bundle().apply { putString("stream", "STEP fund-expense\n") })
            val fundExpense = b.createExpense(trip.id, expense.copy(id="draft", title="공동 교통비", amount=10000,
                payerId="", category=ExpenseCategory.TRANSPORT, paymentSource=ExpensePaymentSource.SHARED_FUND))
            assertEquals(90000L, a.getSharedFund(trip.id).balance)
            assertEquals(50000L, a.getExpenseSettlement(trip.id).totalAmount)
            trip = a.updateTripStatus(trip.id, TripStatus.ONGOING)
            assertEquals(TripStatus.ONGOING, b.getTrip(trip.id).status)
            // Seed only this test account so the real app renders server-backed travel data.
            instrumentation.sendStatus(0, android.os.Bundle().apply { putString("stream", "STEP profile\n") })
            HttpProfileApiDataSource(api).updateCurrentUser(BasicInfo("여행테스트", "두 계정 여행 연동 확인"))
            val questions = JSONObject(publicApi.request("GET", "/api/v1/surveys/travel-personality-v1", authenticated=false)).getJSONArray("questions")
            val answers = org.json.JSONArray()
            repeat(questions.length()) { index ->
                val q = questions.getJSONObject(index)
                answers.put(JSONObject().put("questionId", q.getString("id")).put("optionId", q.getJSONArray("options").getJSONObject(0).getString("id")))
            }
            api.request("POST", "/api/v1/surveys/travel-personality-v1/submissions", JSONObject().put("answers", answers).toString())
            instrumentation.sendStatus(0, android.os.Bundle().apply { putString("stream", "STEP ui\n") })
            store.save(owner.currentSession()!!)
            val activity = instrumentation.startActivitySync(Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
            try {
                awaitText("나의 여행")
                awaitText("서울 함께 여행")
                screenshot("travel-list")
                click("서울 함께 여행")
                awaitText("여행 계획")
                SystemClock.sleep(1500)
                awaitText("여행 계획")
                // Exercise the scroll container and capture its rendered contents after navigation.
                nodes().firstOrNull { it.isScrollable }?.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
                SystemClock.sleep(400)
                nodes().firstOrNull { it.isScrollable }?.performAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD)
                SystemClock.sleep(400)
                screenshot("travel-detail")
                click("함께하는 친구 2명 보기")
                awaitText("동행테스트")
                screenshot("travel-participants")
                automation.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK)
                click("소비")
                awaitText("함께 먹은 점심")
                awaitText("공동 교통비")
                SystemClock.sleep(700)
                screenshot("travel-ledger")
            } finally { instrumentation.runOnMainSync { activity.finish() } }
            instrumentation.sendStatus(0, android.os.Bundle().apply { putString("stream", "STEP delete\n") })
            a.deleteExpense(trip.id, expense.id)
            b.deleteExpense(trip.id, fundExpense.id)
            a.deleteSchedule(trip.id, second.id)
            assertEquals(1, b.listSchedules(trip.id).size)
            a.updateTripStatus(trip.id, TripStatus.COMPLETED)
            a.deleteTrip(trip.id)
            cleanupTripId = null
            assertFalse(a.listTrips().any { it.id == trip.id })
        } finally {
            cleanupTripId?.let { id ->
                val gateway = ServerTravelGateway(client(accounts.first()))
                try { gateway.deleteTrip(id) } catch (error: com.gayadi.android.data.datasource.GayadiApiException) {
                    if (error.statusCode != 404) throw error
                }
            }
            for (account in accounts.asReversed()) {
                try { HttpProfileApiDataSource(client(account)).deleteCurrentUser() }
                finally { store.clear() }
            }
            File(context.filesDir, "travel-state.json").delete()
            File(context.filesDir, "user-profile.xml").delete()
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

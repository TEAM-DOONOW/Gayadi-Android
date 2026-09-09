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
import com.gayadi.android.data.remote.travel.ServerPlanningGateway
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

/** Dev-only test; cleans its temporary trip before removing its own account. */
@RunWith(AndroidJUnit4::class)
class DevPlanningIntegrationTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val automation get() = instrumentation.uiAutomation
    @Test fun planningAndRouteSelection() = runBlocking {
        assumeTrue(InstrumentationRegistry.getArguments().getString("liveApi") == "true")
        check(BuildConfig.FLAVOR == "dev" && BuildConfig.API_BASE_URL == "http://223.130.134.57:8080")
        val store=EncryptedFileAuthSessionStore(File(context.filesDir,"auth-session"))
        check(store.load()==null)
        val publicApi=GayadiApiClient(BuildConfig.API_BASE_URL)
        val json=JSONObject(publicApi.request("POST","/api/v1/auth/registrations",JSONObject()
            .put("email","planning-${UUID.randomUUID()}@example.invalid").put("password",UUID.randomUUID().toString())
            .put("nickname","경로테스트").toString(),false))
        val user=json.getJSONObject("user")
        val session=AuthSession(json.getString("accessToken"),"Bearer",json.getLong("expiresIn"),json.getString("refreshToken"),
            json.getLong("refreshExpiresIn"),System.currentTimeMillis()/1000,AuthUser(user.getLong("id"),"경로테스트",user.getString("email")))
        store.save(session)
        val auth=DefaultAuthRepository(HttpAuthApiDataSource(BuildConfig.API_BASE_URL),store)
        val api=GayadiApiClient(BuildConfig.API_BASE_URL,auth)
        val travel=ServerTravelGateway(api)
        val planning=ServerPlanningGateway(api,auth)
        var tripId:String?=null
        try {
            HttpProfileApiDataSource(api).updateCurrentUser(BasicInfo("경로테스트","자동 일정과 경로 검증"))
            val qs=JSONObject(publicApi.request("GET","/api/v1/surveys/travel-personality-v1",authenticated=false)).getJSONArray("questions")
            val answers=org.json.JSONArray()
            repeat(qs.length()) { i -> val q=qs.getJSONObject(i); answers.put(JSONObject().put("questionId",q.getString("id"))
                .put("optionId",q.getJSONArray("options").getJSONObject(0).getString("id"))) }
            api.request("POST","/api/v1/surveys/travel-personality-v1/submissions",JSONObject().put("answers",answers).toString())
            val trip=travel.createTrip(CreateTripCommand("서울 경로 여행","2026.10.10","2026.10.11",listOf("서울")))
            tripId=trip.id
            api.request("POST","/api/v1/trips/${trip.id}/survey-responses",JSONObject().put("answers",answers).toString())
            assertNull(planning.getPlan(trip.id))
            instrumentation.sendStatus(0, android.os.Bundle().apply { putString("stream", "STEP plan\n") })
            val plan=planning.generatePlan(trip.id)
            assertEquals(2,plan.days.size)
            assertTrue(plan.days.all { it.items.isNotEmpty() })
            assertEquals(plan,planning.getPlan(trip.id))
            instrumentation.sendStatus(0, android.os.Bundle().apply { putString("stream", "STEP routes\n") })
            val routes=planning.recommend(trip.id,PlanningRouteType.ITINERARY)
            assertTrue(routes.isNotEmpty())
            assertTrue(routes.all { it.stops.size >= 2 && it.stops.all(String::isNotBlank) })
            instrumentation.sendStatus(0, android.os.Bundle().apply { putString("stream", "STEP select\n") })
            val selected=planning.select(trip.id,routes.first())
            assertEquals(selected.id,planning.selectedRoutes(trip.id).single().id)
            instrumentation.sendStatus(0, android.os.Bundle().apply { putString("stream", "STEP clear\n") })
            planning.clearSelection(trip.id,PlanningRouteType.ITINERARY)
            assertTrue(planning.selectedRoutes(trip.id).isEmpty())
            // Enable after the participant-settings backend PR is deployed to dev.
            if (InstrumentationRegistry.getArguments().getString("participantSettings") == "true") {
            instrumentation.sendStatus(0, android.os.Bundle().apply { putString("stream", "STEP endpoint\n") })
            val places=planning.searchPlaces("서울")
            assertTrue(places.isNotEmpty())
            planning.setEndpoint(trip.id,PlanningRouteType.DEPARTURE,places.first().id)
            planning.setEndpoint(trip.id,PlanningRouteType.HOME,places.last().id)
            instrumentation.sendStatus(0, android.os.Bundle().apply { putString("stream", "STEP personal\n") })
            for(type in listOf(PlanningRouteType.DEPARTURE,PlanningRouteType.HOME)) {
                val route=planning.recommend(trip.id,type).first()
                assertEquals(type,route.type)
                planning.select(trip.id,route)
                assertTrue(planning.selectedRoutes(trip.id).any { it.type==type })
                planning.clearSelection(trip.id,type)
            }
            }
            instrumentation.sendStatus(0, android.os.Bundle().apply { putString("stream", "STEP ui\n") })
            val activity=instrumentation.startActivitySync(Intent(context,MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
            try {
                click("서울 경로 여행")
                click("전체 동선 보기")
                click("여행 동선 추천")
                awaitText("자동 일정 보기")
                screenshot("planning-overview")
                click("이 여행 성향 등록")
                click("테스트 시작하기")
                repeat(qs.length()) { index ->
                    val question=qs.getJSONObject(index)
                    click(question.getJSONArray("options").getJSONObject(0).getString("text"))
                    click(if(index==qs.length()-1) "결과 보기" else "다음")
                }
                awaitText("자동 일정 보기")
                click("경로 추천받기")
                awaitText("이 경로 사용")
                screenshot("planning-routes")
                click("이 경로 사용")
                awaitText("선택한 경로")
                assertTrue(planning.selectedRoutes(trip.id).any { it.type==PlanningRouteType.ITINERARY })
                screenshot("planning-selected")
            } finally { instrumentation.runOnMainSync { activity.finish() } }
        } catch (error: com.gayadi.android.data.datasource.GayadiApiException) {
            throw AssertionError("Planning API failure: ${error.statusCode} ${error.errorCode}", error)
        } finally {
            instrumentation.sendStatus(0, android.os.Bundle().apply { putString("stream", "STEP cleanup\n") })
            tripId?.let { travel.deleteTrip(it) }
            HttpProfileApiDataSource(api).deleteCurrentUser()
            store.clear()
            File(context.filesDir,"travel-state.json").delete()
            File(context.filesDir,"user-profile.xml").delete()
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

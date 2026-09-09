package com.gayadi.android.data

import com.gayadi.android.data.datasource.*
import com.gayadi.android.data.remote.travel.ServerPlanningGateway
import com.gayadi.android.domain.model.*
import com.gayadi.android.domain.repository.*
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.*
import org.json.JSONObject
import org.junit.*
import org.junit.Assert.*

class ServerPlanningGatewayTest {
    private lateinit var server:MockWebServer
    private lateinit var gateway:ServerPlanningGateway
    private val session=AuthSession("token","Bearer",3600,"refresh",3600,0,AuthUser(7,"테스트","test@example.invalid"))
    private val auth=object:AuthRepository {
        override fun currentSession()=session
        override fun clearSession() {}
        override suspend fun validAccessToken()=session.accessToken
        override suspend fun refreshSession()=session
        override suspend fun signInWithGoogle(idToken:String)=session
    }
    @Before fun setup() { server=MockWebServer().apply{start()};gateway=ServerPlanningGateway(GayadiApiClient(server.url("/").toString(),auth),auth) }
    @After fun teardown() { server.shutdown() }
    private fun json(value:String)=MockResponse().setHeader("Content-Type","application/json").setBody(value)
    @Test fun routePrerequisiteHasActionableMessageWithoutServerDiagnostics() = runTest {
        server.enqueue(json("""{"code":"ROUTE_DEPARTURE_PLACE_REQUIRED","message":"private diagnostics"}""").setResponseCode(400))
        val error=runCatching { gateway.recommend("2",PlanningRouteType.DEPARTURE) }.exceptionOrNull() as GayadiApiException
        assertEquals("ROUTE_DEPARTURE_PLACE_REQUIRED",error.errorCode)
        assertTrue(error.message!!.contains("출발 장소"));assertFalse(error.message!!.contains("private"))
    }
    @Test fun tripSurveyUsesTripEndpoint() = runTest {
        server.enqueue(json("""{"resultCode":"PNA"}"""))
        val api=GayadiApiClient(server.url("/").toString(),auth)
        com.gayadi.android.data.repository.RestSurveySubmissionRepository(api,"2").submit(mapOf("q01" to "a"))
        assertEquals("/api/v1/trips/2/survey-responses",server.takeRequest().path)
    }
    @Test fun omittedMetricsRemainUnknownAndLocalEstimateIsLabelled() {
        val route=ServerPlanningGateway.route(JSONObject("""{"id":3,"optionId":"balanced","name":"균형","type":"ITINERARY","durationMinutes":20,"provider":"LOCAL_ESTIMATE","fallback":false,"stops":[{"label":"서울역"},{"label":"경복궁"}]}"""))
        assertNull(route.distanceMeters);assertNull(route.fare);assertEquals(20,route.durationMinutes)
        assertTrue(route.isEstimate)
        assertEquals(listOf("서울역","경복궁"),route.stops)
    }
    @Test fun missingPlanIsEmptyButForbiddenIsAnError() = runTest {
        server.enqueue(MockResponse().setResponseCode(404));assertNull(gateway.getPlan("2"))
        server.enqueue(MockResponse().setResponseCode(403));assertTrue(runCatching{gateway.getPlan("2")}.isFailure)
    }
    @Test fun allPlanDaysAreMappedWithoutDuplicatingFirstDay() {
        val item="""{"id":3,"title":"장소","planned_start":"2026-10-10T10:00:00","planned_end":"2026-10-10T11:00:00","address":"서울"}"""
        val day="""{"plan_date":"2026-10-10","title":"1일차","items":[$item]}"""
        val result=ServerPlanningGateway.plan(JSONObject("""{"days":[$day],"items":[$item]}"""))
        assertEquals(1,result.days.size);assertEquals(1,result.days.single().items.size)
    }
}

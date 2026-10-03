package com.gayadi.android.api

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.gayadi.android.BuildConfig
import com.gayadi.android.data.datasource.GayadiApiClient
import com.gayadi.android.data.datasource.HttpProfileApiDataSource
import com.gayadi.android.data.remote.ranking.ServerRankingGateway
import com.gayadi.android.data.remote.travel.ServerTravelGateway
import com.gayadi.android.data.remote.travel.ServerTripSupportGateway
import com.gayadi.android.data.repository.EncryptedFileAuthSessionStore
import com.gayadi.android.domain.model.AuthSession
import com.gayadi.android.domain.model.AuthUser
import com.gayadi.android.domain.model.BasicInfo
import com.gayadi.android.domain.model.RankingType
import com.gayadi.android.domain.model.RouteTransportMode
import com.gayadi.android.domain.model.ScheduleType
import com.gayadi.android.domain.repository.AuthRepository
import com.gayadi.android.domain.repository.CreateTripCommand
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 홈 순위와 여행루트를 앱 데이터 계층 그대로 서버에 붙여 확인합니다.
 * `liveApi=true`일 때만 실행하며 임시 계정과 여행은 끝나면 지웁니다.
 * `seedSession=true`는 로컬 서버 UI 확인용으로 로그인 세션과 여행을 남깁니다.
 */
@RunWith(AndroidJUnit4::class)
class DevRouteFeatureIntegrationTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val dates = DateTimeFormatter.ofPattern("yyyy.MM.dd")

    @Test
    fun homeRankingsAndItineraryRoundTrip() = runBlocking {
        assumeTrue(InstrumentationRegistry.getArguments().getString("liveApi") == "true")
        check(BuildConfig.FLAVOR == "dev")
        val (auth, _) = register("루트연동")
        val api = GayadiApiClient(BuildConfig.API_BASE_URL, auth)
        val travel = ServerTravelGateway(api)
        val support = ServerTripSupportGateway(api)
        var tripId: String? = null
        try {
            val rankings = ServerRankingGateway(api)
            RankingType.entries.forEach { type ->
                val list = rankings.getRankings(type, "", 5)
                assertEquals(type, list.type)
                assertTrue(list.items.all { it.title.isNotBlank() && it.imageUrl != "null" })
            }

            val start = LocalDate.now().plusDays(2)
            val trip = travel.createTrip(CreateTripCommand(
                "여행루트 연동", start.format(dates), start.plusDays(1).format(dates), listOf("서울"),
            ))
            tripId = trip.id
            val date = start.format(dates)
            val preview = support.recommendItinerary(
                trip.id, date, "10:00", "18:00", RouteTransportMode.PUBLIC_TRANSIT, 0,
            )
            assertTrue(preview.stops.size >= 2)
            assertTrue(preview.stops.all { it.imageUrl != "null" })
            assertEquals(0, preview.stops.first().travelMinutesFromPrevious)
            preview.stops.zipWithNext().forEach { (before, after) ->
                assertTrue(after.arrivalTime >= before.departureTime)
            }
            assertTrue(preview.stops.last().departureTime <= "18:00")

            val other = support.recommendItinerary(
                trip.id, date, "10:00", "18:00", RouteTransportMode.PUBLIC_TRANSIT, 1,
            )
            assertNotEquals(preview.stops.map { it.placeId }, other.stops.map { it.placeId })

            val applied = support.applyItinerary(
                trip.id, date, "10:00", "18:00", RouteTransportMode.PUBLIC_TRANSIT, 0,
                preview.stops.map { it.placeId },
            )
            assertEquals(preview.stops.map { it.placeId }, applied.stops.map { it.placeId })
            val main = travel.listSchedules(trip.id)
                .filter { it.date == date && it.type == ScheduleType.MAIN }
                .sortedBy { it.time }
            assertEquals(preview.stops.map { it.placeId }, main.map { it.placeId })
            assertEquals(preview.stops.map { it.arrivalTime }, main.map { it.time })
        } finally {
            tripId?.let { travel.deleteTrip(it) }
            HttpProfileApiDataSource(api).deleteCurrentUser()
        }
    }

    /** 로컬 서버에서 Google 로그인 없이 화면을 확인하도록 앱 세션과 서울 여행 하나를 준비합니다. */
    @Test
    fun seedLocalSession() = runBlocking {
        assumeTrue(InstrumentationRegistry.getArguments().getString("seedSession") == "true")
        check(BuildConfig.FLAVOR == "dev")
        check(BuildConfig.API_BASE_URL.contains("10.0.2.2")) { "seedSession은 로컬 서버 전용입니다." }
        val (auth, session) = register("로컬확인")
        val api = GayadiApiClient(BuildConfig.API_BASE_URL, auth)
        HttpProfileApiDataSource(api).updateCurrentUser(BasicInfo("로컬확인", "여행루트 화면 확인"))
        val start = LocalDate.now().plusDays(1)
        ServerTravelGateway(api).createTrip(CreateTripCommand(
            "서울 여행루트", start.format(dates), start.plusDays(1).format(dates), listOf("서울"),
        ))
        EncryptedFileAuthSessionStore(File(context.filesDir, "auth-session")).save(session)
    }

    private suspend fun register(nickname: String): Pair<AuthRepository, AuthSession> {
        val response = JSONObject(GayadiApiClient(BuildConfig.API_BASE_URL).request(
            "POST",
            "/api/v1/auth/registrations",
            JSONObject()
                .put("email", "route-${UUID.randomUUID()}@example.invalid")
                .put("password", UUID.randomUUID().toString())
                .put("nickname", nickname)
                .toString(),
            authenticated = false,
        ))
        val user = response.getJSONObject("user")
        val session = AuthSession(
            accessToken = response.getString("accessToken"),
            tokenType = "Bearer",
            expiresInSeconds = response.getLong("expiresIn"),
            refreshToken = response.optString("refreshToken"),
            refreshExpiresInSeconds = response.optLong("refreshExpiresIn"),
            issuedAtEpochSeconds = System.currentTimeMillis() / 1000,
            user = AuthUser(user.getLong("id"), nickname, user.getString("email")),
        )
        val auth = object : AuthRepository {
            override fun currentSession() = session
            override fun clearSession() = Unit
            override suspend fun validAccessToken() = session.accessToken
            override suspend fun refreshSession() = error("Unexpected token expiry")
            override suspend fun signInWithGoogle(idToken: String) = error("Unused")
        }
        return auth to session
    }
}

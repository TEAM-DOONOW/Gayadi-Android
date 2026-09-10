package com.gayadi.android.api

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.gayadi.android.BuildConfig
import com.gayadi.android.data.datasource.GayadiApiClient
import com.gayadi.android.data.datasource.HttpProfileApiDataSource
import com.gayadi.android.data.remote.travel.ServerTravelGateway
import com.gayadi.android.data.remote.travel.ServerTripSupportGateway
import com.gayadi.android.domain.model.AuthSession
import com.gayadi.android.domain.model.AuthUser
import com.gayadi.android.domain.model.BasicInfo
import com.gayadi.android.domain.model.TripStatus
import com.gayadi.android.domain.repository.AuthRepository
import com.gayadi.android.domain.repository.CongestionCommand
import com.gayadi.android.domain.repository.CreateTripCommand
import com.gayadi.android.domain.repository.EventObservationCommand
import com.gayadi.android.domain.repository.ParticipantSettings
import com.gayadi.android.domain.repository.PlaceRecommendationCommand
import com.gayadi.android.domain.repository.SituationCommand
import com.gayadi.android.domain.repository.SurveyAnswer
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Opt-in emulator coverage for user-facing Swagger APIs not exercised by the core tests. */
@RunWith(AndroidJUnit4::class)
class DevAdvancedApiIntegrationTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()

    @Test
    fun advancedSwaggerApisRoundTrip() = runBlocking {
        assumeTrue(InstrumentationRegistry.getArguments().getString("liveApi") == "true")
        check(BuildConfig.FLAVOR == "dev")
        val publicApi = GayadiApiClient(BuildConfig.API_BASE_URL)
        val accounts = mutableListOf<AuthRepository>()

        suspend fun register(nickname: String): AuthRepository {
            val response = JSONObject(publicApi.request(
                "POST",
                "/api/v1/auth/registrations",
                JSONObject()
                    .put("email", "advanced-${UUID.randomUUID()}@example.invalid")
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
            return object : AuthRepository {
                override fun currentSession() = session
                override fun clearSession() = Unit
                override suspend fun validAccessToken() = session.accessToken
                override suspend fun refreshSession() = error("Unexpected token expiry")
                override suspend fun signInWithGoogle(idToken: String) = error("Unused")
            }.also(accounts::add)
        }

        fun client(auth: AuthRepository) = GayadiApiClient(BuildConfig.API_BASE_URL, auth)
        var tripId: String? = null
        try {
            val owner = register("고급연동")
            val guest = register("참여자연동")
            val ownerApi = client(owner)
            val travel = ServerTravelGateway(ownerApi)
            val support = ServerTripSupportGateway(ownerApi)
            val guestSupport = ServerTripSupportGateway(client(guest))
            val ownerId = owner.currentSession()!!.user.id.toString()
            val guestId = guest.currentSession()!!.user.id.toString()

            HttpProfileApiDataSource(ownerApi).updateCurrentUser(BasicInfo("고급연동", "Swagger API 확인"))
            HttpProfileApiDataSource(client(guest)).updateCurrentUser(BasicInfo("참여자연동", "Swagger API 확인"))
            val trip = travel.createTrip(
                CreateTripCommand("고급 API 연동 여행", "2026.10.20", "2026.10.21", listOf("서울")),
            )
            tripId = trip.id

            val places = JSONObject(publicApi.request("GET", "/api/v1/places?limit=4", authenticated = false))
                .getJSONArray("items")
            assertTrue(places.length() >= 4)
            val departurePlaceId = places.getJSONObject(0).getLong("id").toString()
            val returnPlaceId = places.getJSONObject(1).getLong("id").toString()
            assertEquals(departurePlaceId, support.getPlace(departurePlaceId).contentId)
            assertEquals(ownerId, travel.updateCurrentParticipant(
                trip.id,
                ParticipantSettings(departurePlaceId, returnPlaceId),
            ).id)

            assertEquals(guestId, travel.addParticipant(trip.id, guestId).id)
            travel.removeParticipant(trip.id, guestId)
            assertFalse(travel.listParticipants(trip.id).any { it.id == guestId })
            ServerTravelGateway(client(guest)).joinTrip(trip.inviteCode)

            val survey = JSONObject(publicApi.request(
                "GET", "/api/v1/surveys/travel-personality-v1", authenticated = false,
            )).getJSONArray("questions")
            val answers = List(survey.length()) { index ->
                val question = survey.getJSONObject(index)
                SurveyAnswer(
                    question.getString("id"),
                    question.getJSONArray("options").getJSONObject(0).getString("id"),
                )
            }
            assertEquals(trip.id, support.submitTripSurvey(trip.id, answers).tripId)
            guestSupport.submitTripSurvey(trip.id, answers)
            assertEquals(2L, support.getTripPersonality(trip.id).responseCount)

            val plan = support.generatePlan(trip.id)
            assertEquals(2, plan.dayCount)
            assertTrue(plan.itemCount >= 2)
            assertEquals(plan, support.getPlan(trip.id))
            val dashboard = support.getDashboard(trip.id)
            assertEquals(2, dashboard.participantCount)
            assertEquals(trip.id, dashboard.tripId)

            listOf("DEPARTURE", "ITINERARY", "HOME").forEach { type ->
                val userId = ownerId.takeIf { type != "ITINERARY" }
                val recommendations = support.recommendRoutes(trip.id, type, userId)
                assertEquals(2, recommendations.size)
                assertTrue(recommendations.all { it.id.isNotBlank() && it.stops.size >= 2 })
                val selected = support.selectRoute(trip.id, type, recommendations.first().optionId, userId)
                assertTrue(selected.selected)
            }
            assertEquals(3, support.listSelectedRoutes(trip.id).size)
            support.clearSelectedRoute(trip.id, "ITINERARY")
            assertEquals(2, support.listSelectedRoutes(trip.id).size)

            travel.updateTripStatus(trip.id, TripStatus.ONGOING)
            val lowImpact = support.observeEvent(
                trip.id,
                EventObservationCommand(
                    eventType = "TRANSPORT",
                    source = "ANDROID_EMULATOR",
                    severity = "LOW",
                    values = mapOf("delayMinutes" to 1),
                ),
            )
            assertFalse(lowImpact.impact)
            val highImpact = support.observeEvent(
                trip.id,
                EventObservationCommand(
                    placeId = departurePlaceId,
                    eventType = "WEATHER",
                    source = "ANDROID_EMULATOR",
                    severity = "HIGH",
                    values = mapOf("condition" to "RAIN"),
                ),
            )
            val proposal = requireNotNull(highImpact.proposal)
            assertTrue(support.listChangeProposals(trip.id).any { it.id == proposal.id })
            assertEquals("REJECTED", support.decideChangeProposal(
                trip.id, proposal.id, approve = false, selectedOptionKey = null,
                baseRevisionNo = proposal.baseRevisionNo,
            ).status)

            val placeRecommendations = support.recommendPlaces(
                PlaceRecommendationCommand(
                    destination = "서울",
                    profile = "계획적인 도시 여행",
                    latitude = 37.5665,
                    longitude = 126.9780,
                    keywords = listOf("박물관"),
                    limit = 2,
                    externalProcessingConsent = true,
                ),
            )
            assertTrue(placeRecommendations.places.isNotEmpty())
            val situation = support.respondToSituation(
                trip.id,
                SituationCommand(
                    latitude = 37.5665,
                    longitude = 126.9780,
                    keywords = listOf("실내"),
                    weatherCondition = "RAIN",
                    externalProcessingConsent = true,
                ),
            )
            assertTrue(situation.summary.isNotBlank())

            assertTrue(support.getWeatherNow(37.5665, 126.9780).temperature?.isNotBlank() == true)
            assertTrue(support.getUltraForecast(37.5665, 126.9780).forecastSlotCount > 0)
            assertTrue(support.getForecast(37.5665, 126.9780).forecastSlotCount > 0)
            assertTrue(support.getForecastVersion("SHRT", "202609100200").itemCount > 0)
            assertTrue(support.getCongestion(CongestionCommand("11", "110", "서울", "서울숲")).score >= 0)

            assertTrue(support.getTourAreas("서울").places.isNotEmpty())
            assertTrue(support.getNearbyTourPlaces(126.9780, 37.5665, 1000).places.isNotEmpty())
            assertTrue(support.searchTourPlaces("박물관").places.isNotEmpty())
            assertTrue(support.getTourFestivals("20260901", "20261231").places.isNotEmpty())
            assertTrue(support.getTourStays().places.isNotEmpty())
        } finally {
            tripId?.let { id ->
                runCatching { ServerTravelGateway(client(accounts.first())).deleteTrip(id) }
            }
            accounts.asReversed().forEach { account ->
                runCatching { HttpProfileApiDataSource(client(account)).deleteCurrentUser() }
            }
        }
    }
}

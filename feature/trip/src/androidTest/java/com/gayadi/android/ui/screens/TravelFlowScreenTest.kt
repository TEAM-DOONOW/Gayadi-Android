package com.gayadi.android.ui.screens

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gayadi.android.domain.model.TravelSchedule
import com.gayadi.android.domain.model.ScheduleType
import com.gayadi.android.domain.model.ExpenseSettlementSummary
import com.gayadi.android.domain.model.ParticipantExpenseBalance
import com.gayadi.android.domain.model.SettlementTransfer
import com.gayadi.android.domain.model.TravelExpense
import com.gayadi.android.domain.model.TravelParticipant
import com.gayadi.android.ui.theme.GayadiTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TravelFlowScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    /* ScheduleScreen was removed in favor of the inline schedule options sheet.
    @Test
    fun emptyScheduleCanOpenMainAlternativeEditor() {
        val saved = mutableListOf<TravelSchedule>()
        composeRule.setContent {
            GayadiTheme {
                ScheduleScreen(
                    tripId = "trip-28",
                    tripName = "제주 여행",
                    defaultDate = "2026.08.08",
                    schedules = emptyList(),
                    onBack = {},
                    onSave = { saved += it },
                    onDelete = {},
                    onMove = { _, _ -> },
                    onToggleVisited = {},
                    onRecommendRoute = {},
                )
            }
        }

        composeRule.onNodeWithText("아직 일정이 없어요").assertIsDisplayed()
        composeRule.onNodeWithText("일정 추가").performClick()
        composeRule.onNodeWithText("메인 일정").assertIsDisplayed()
        composeRule.onNodeWithText("대체 일정").assertIsDisplayed()
        composeRule.onNodeWithText("일정 이름").performTextInput("성산일출봉")
        composeRule.onNodeWithText("저장").performClick()
        composeRule.runOnIdle {
            assertEquals("trip-28", saved.single().tripId)
            assertEquals("2026.08.08", saved.single().date)
            assertEquals(ScheduleType.MAIN, saved.single().type)
        }

        composeRule.onNodeWithText("일정 추가").performClick()
        composeRule.onNodeWithText("일정 이름").performTextInput("우도")
        composeRule.onNodeWithText("대체 일정").performClick()
        composeRule.onNodeWithText("저장").performClick()
        composeRule.runOnIdle { assertEquals(ScheduleType.ALTERNATIVE, saved.last().type) }
    }

    @Test
    fun scheduleEditorRejectsEndTimeBeforeStartTime() {
        val saved = mutableListOf<TravelSchedule>()
        composeRule.setContent {
            GayadiTheme {
                ScheduleScreen(
                    tripId = "trip-28",
                    tripName = "제주 여행",
                    defaultDate = "2026.08.08",
                    schedules = emptyList(),
                    onBack = {},
                    onSave = { saved += it },
                    onDelete = {},
                    onMove = { _, _ -> },
                    onToggleVisited = {},
                    onRecommendRoute = {},
                )
            }
        }

        composeRule.onNodeWithText("일정 추가").performClick()
        composeRule.onNodeWithText("일정 이름").performTextInput("성산일출봉")
        composeRule.onNodeWithText("종료 (선택)").performTextClearance()
        composeRule.onNodeWithText("종료 (선택)").performTextInput("09:00")
        composeRule.onNodeWithText("저장").performClick()

        composeRule.onNodeWithText("종료 시간은 시작 시간보다 뒤여야 해요").assertIsDisplayed()
        composeRule.runOnIdle { assertTrue(saved.isEmpty()) }
    }

    @Test
    fun legacyScheduleCanBeEditedWithoutAddingEndTime() {
        val initial = TravelSchedule(
            id = "schedule-1",
            tripId = "trip-28",
            title = "기존 일정",
            date = "2026.08.08",
            time = "10:00",
            endTime = null,
            order = 0,
        )
        var saved: TravelSchedule? = null
        composeRule.setContent {
            GayadiTheme {
                ScheduleScreen(
                    tripId = "trip-28",
                    tripName = "제주 여행",
                    defaultDate = "2026.08.08",
                    schedules = listOf(initial),
                    onBack = {},
                    onSave = { saved = it },
                    onDelete = {},
                    onMove = { _, _ -> },
                    onToggleVisited = {},
                    onRecommendRoute = {},
                )
            }
        }

        composeRule.onNodeWithText("수정").performClick()
        composeRule.onNodeWithText("저장").performClick()

        composeRule.runOnIdle {
            assertEquals("schedule-1", saved?.id)
            assertEquals(null, saved?.endTime)
        }
    }

    @Test
    fun scheduleDeleteWarnsAboutLinkedExpensesAndRequiresConfirmation() {
        val schedule = TravelSchedule(
            id = "schedule-1",
            tripId = "trip-28",
            title = "성산일출봉",
            date = "2026.08.08",
            time = "10:00",
            order = 0,
        )
        var deletedScheduleId: String? = null
        composeRule.setContent {
            GayadiTheme {
                ScheduleScreen(
                    tripId = "trip-28",
                    tripName = "제주 여행",
                    defaultDate = "2026.08.08",
                    schedules = listOf(schedule),
                    expenseCountsBySchedule = mapOf(schedule.id to 2),
                    onBack = {},
                    onSave = {},
                    onDelete = { deletedScheduleId = it },
                    onMove = { _, _ -> },
                    onToggleVisited = {},
                    onRecommendRoute = {},
                )
            }
        }

        composeRule.onNodeWithText("삭제").performClick()
        composeRule.onNodeWithText("이 일정과 연결된 비용 2건도 함께 삭제되며 되돌릴 수 없어요.")
            .assertIsDisplayed()
        composeRule.onNodeWithText("취소").performClick()
        composeRule.runOnIdle { assertEquals(null, deletedScheduleId) }

        composeRule.onNodeWithText("삭제").performClick()
        composeRule.onNodeWithText("일정 삭제").performClick()
        composeRule.runOnIdle { assertEquals(schedule.id, deletedScheduleId) }
    }

    */
    @Test
    fun tripListDeleteWarnsAboutCascadedCostsAndRequiresConfirmation() {
        val trip = TripSummary(
            id = "trip-28",
            name = "제주 여행",
            startDate = "2026.08.19",
            endDate = "2026.08.21",
            cities = listOf("제주"),
            coverImageResList = emptyList(),
        )
        var deletedTripId: String? = null
        composeRule.setContent {
            GayadiTheme {
                MyTripScreen(
                    trips = listOf(trip),
                    onAddTrip = {},
                    onOpenTripDetail = {},
                    onDeleteTrip = { deletedTripId = it },
                    onOpenSettings = {},
                )
            }
        }

        composeRule.onNodeWithContentDescription("여행 메뉴").performClick()
        composeRule.onNodeWithContentDescription("여행 삭제").performClick()
        composeRule.onNodeWithText("일정과 연결된 모든 비용, 초대 정보도 함께 삭제되며 되돌릴 수 없어요.")
            .assertIsDisplayed()
        composeRule.onNodeWithText("취소").performClick()
        composeRule.runOnIdle { assertEquals(null, deletedTripId) }

        composeRule.onNodeWithContentDescription("여행 메뉴").performClick()
        composeRule.onNodeWithContentDescription("여행 삭제").performClick()
        composeRule.onNodeWithText("여행 삭제").performClick()
        composeRule.runOnIdle { assertEquals(trip.id, deletedTripId) }
    }

    @Test
    fun expenseEditorValidatesThenCreatesExpenseWithSelectedPeople() {
        val participants = listOf(
            TravelParticipant("local-user", "나"),
            TravelParticipant("friend-1", "여행곰"),
        )
        val schedule = TravelSchedule(
            id = "schedule-1",
            tripId = "trip-28",
            title = "성산일출봉",
            date = "2026.08.08",
            time = "10:00",
            endTime = "11:00",
            order = 0,
        )
        var saved: TravelExpense? = null
        composeRule.setContent {
            GayadiTheme {
                ExpenseEditorScreen(
                    expense = null,
                    schedule = schedule,
                    participants = participants,
                    initialPayerId = "local-user",
                    onBack = {},
                    onSave = { saved = it },
                )
            }
        }

        composeRule.onNodeWithText("비용 저장하기").performClick()
        composeRule.onNodeWithText("금액을 입력해 주세요").assertIsDisplayed()
        composeRule.onNodeWithText("비용 내용을 입력해 주세요").assertIsDisplayed()

        composeRule.onNodeWithText("금액").performTextInput("35001")
        composeRule.onNodeWithText("내용").performTextInput("점심 식사")
        composeRule.onNodeWithText("비용 저장하기").performClick()

        composeRule.runOnIdle {
            assertEquals(35_001L, saved?.amount)
            assertEquals("local-user", saved?.payerId)
            assertEquals(listOf("local-user", "friend-1"), saved?.participantIds)
            assertEquals("schedule-1", saved?.scheduleId)
        }
    }

    @Test
    fun expenseEditorShowsLoadingBeforeTravelStateIsAvailable() {
        composeRule.setContent {
            GayadiTheme {
                ExpenseEditorScreen(
                    expense = null,
                    schedule = null,
                    participants = emptyList(),
                    initialPayerId = null,
                    onBack = {},
                    onSave = {},
                    hasLoadedTravelState = false,
                    isLoadingTravelState = true,
                )
            }
        }

        composeRule.onNodeWithText("일정 정보를 불러오는 중이에요").assertIsDisplayed()
        composeRule.onNodeWithText("일정을 찾을 수 없어요").assertDoesNotExist()
    }

    @Test
    fun expenseEditorDistinguishesTravelLoadFailureFromMissingSchedule() {
        composeRule.setContent {
            GayadiTheme {
                ExpenseEditorScreen(
                    expense = null,
                    schedule = null,
                    participants = emptyList(),
                    initialPayerId = null,
                    onBack = {},
                    onSave = {},
                    hasLoadedTravelState = false,
                    isLoadingTravelState = false,
                )
            }
        }

        composeRule.onNodeWithText("여행 정보를 불러오지 못했어요").assertIsDisplayed()
        composeRule.onNodeWithText("일정 정보를 불러오는 중이에요").assertDoesNotExist()
        composeRule.onNodeWithText("일정을 찾을 수 없어요").assertDoesNotExist()
    }

    @Test
    fun expenseEditorBlocksMissingExpenseEditInsteadOfCreatingNewExpense() {
        val schedule = TravelSchedule(
            id = "schedule-1",
            tripId = "trip-28",
            title = "성산일출봉",
            date = "2026.08.08",
            time = "10:00",
            order = 0,
        )
        var saved = false
        composeRule.setContent {
            GayadiTheme {
                ExpenseEditorScreen(
                    expense = null,
                    isEditMode = true,
                    schedule = schedule,
                    participants = listOf(TravelParticipant("local-user", "나")),
                    initialPayerId = "local-user",
                    onBack = {},
                    onSave = { saved = true },
                )
            }
        }

        composeRule.onNodeWithText("비용 수정").assertIsDisplayed()
        composeRule.onNodeWithText("비용 내역을 찾을 수 없어요").assertIsDisplayed()
        composeRule.onNodeWithText("수정 내용 저장하기").assertDoesNotExist()
        composeRule.runOnIdle { assertFalse(saved) }
    }

    @Test
    fun repeatedExpenseSubmissionReusesTheSameDraftId() {
        val participant = TravelParticipant("local-user", "나")
        val schedule = TravelSchedule(
            id = "schedule-1",
            tripId = "trip-28",
            title = "성산일출봉",
            date = "2026.08.08",
            time = "10:00",
            order = 0,
        )
        val submittedExpenses = mutableListOf<TravelExpense>()
        composeRule.setContent {
            GayadiTheme {
                ExpenseEditorScreen(
                    expense = null,
                    schedule = schedule,
                    participants = listOf(participant),
                    initialPayerId = participant.id,
                    onBack = {},
                    onSave = submittedExpenses::add,
                )
            }
        }

        composeRule.onNodeWithText("금액").performTextInput("12000")
        composeRule.onNodeWithText("내용").performTextInput("KTX")
        composeRule.onNodeWithText("비용 저장하기").performClick()
        composeRule.onNodeWithText("비용 저장하기").performClick()

        composeRule.runOnIdle {
            assertEquals(2, submittedExpenses.size)
            assertEquals(submittedExpenses.first().id, submittedExpenses.last().id)
        }
    }

    @Test
    fun expenseDraftSurvivesSavedInstanceStateRestoration() {
        val participant = TravelParticipant("local-user", "나")
        val schedule = TravelSchedule(
            id = "schedule-restore",
            tripId = "trip-28",
            title = "여수 엑스포역",
            date = "2026.08.19",
            time = "10:00",
            order = 0,
        )
        val restorationTester = StateRestorationTester(composeRule)
        restorationTester.setContent {
            GayadiTheme {
                ExpenseEditorScreen(
                    expense = null,
                    schedule = schedule,
                    participants = listOf(participant),
                    initialPayerId = participant.id,
                    onBack = {},
                    onSave = {},
                )
            }
        }

        composeRule.onNodeWithText("금액").performTextInput("12000")
        composeRule.onNodeWithText("내용").performTextInput("KTX")
        restorationTester.emulateSavedInstanceStateRestore()

        composeRule.onNodeWithText("12000").assertIsDisplayed()
        composeRule.onNodeWithText("KTX").assertIsDisplayed()
    }

    /* ScheduleScreen was removed in favor of the inline schedule options sheet.
    @Test
    fun editedScheduleIdentityAndDraftSurviveSavedInstanceStateRestoration() {
        val schedule = TravelSchedule(
            id = "schedule-restore",
            tripId = "trip-28",
            title = "기존 일정",
            date = "2026.08.19",
            time = "10:00",
            order = 0,
        )
        var saved: TravelSchedule? = null
        val restorationTester = StateRestorationTester(composeRule)
        restorationTester.setContent {
            GayadiTheme {
                ScheduleScreen(
                    tripId = "trip-28",
                    tripName = "여수 여행",
                    defaultDate = "2026.08.19",
                    schedules = listOf(schedule),
                    onBack = {},
                    onSave = { saved = it },
                    onDelete = {},
                    onMove = { _, _ -> },
                    onToggleVisited = {},
                    onRecommendRoute = {},
                )
            }
        }

        composeRule.onNodeWithText("수정").performClick()
        composeRule.onNodeWithText("일정 이름").performTextClearance()
        composeRule.onNodeWithText("일정 이름").performTextInput("수정 중 일정")
        restorationTester.emulateSavedInstanceStateRestore()

        composeRule.onNodeWithText("수정 중 일정").assertIsDisplayed()
        composeRule.onNodeWithText("저장").performClick()
        composeRule.runOnIdle { assertEquals(schedule.id, saved?.id) }
    }

    */
    @Test
    fun expenseEditorLoadsAndUpdatesExistingExpense() {
        val participant = TravelParticipant("local-user", "나")
        val schedule = TravelSchedule(
            id = "schedule-1",
            tripId = "trip-28",
            title = "성산일출봉",
            date = "2026.08.08",
            time = "10:00",
            order = 0,
        )
        val expense = TravelExpense(
            id = "expense-1",
            tripId = "trip-28",
            scheduleId = schedule.id,
            title = "점심 식사",
            amount = 35_001,
            payerId = participant.id,
            participantIds = listOf(participant.id),
            date = "2026.08.08",
            time = "12:00",
        )
        var saved: TravelExpense? = null
        composeRule.setContent {
            GayadiTheme {
                ExpenseEditorScreen(
                    expense = expense,
                    schedule = schedule,
                    participants = listOf(participant),
                    initialPayerId = participant.id,
                    onBack = {},
                    onSave = { saved = it },
                )
            }
        }

        composeRule.onNodeWithText("비용 수정").assertIsDisplayed()
        composeRule.onNodeWithText("금액").performTextClearance()
        composeRule.onNodeWithText("금액").performTextInput("42000")
        composeRule.onNodeWithText("내용").performTextClearance()
        composeRule.onNodeWithText("내용").performTextInput("저녁 식사")
        composeRule.onNodeWithText("수정 내용 저장하기").performClick()

        composeRule.runOnIdle {
            assertEquals("expense-1", saved?.id)
            assertEquals(42_000L, saved?.amount)
            assertEquals("저녁 식사", saved?.title)
        }
    }

    @Test
    fun ledgerDeleteConfirmationRecomposesExpenseAndTotalsFromUpdatedState() {
        val participants = listOf(
            TravelParticipant("local-user", "나"),
            TravelParticipant("friend-1", "여행곰"),
        )
        val schedule = TravelSchedule(
            id = "schedule-1",
            tripId = "trip-28",
            title = "성산일출봉",
            date = "2026.08.08",
            time = "10:00",
            order = 0,
        )
        val expense = TravelExpense(
            id = "expense-1",
            tripId = "trip-28",
            scheduleId = schedule.id,
            title = "점심 식사",
            amount = 35_001,
            payerId = "local-user",
            participantIds = participants.map(TravelParticipant::id),
            date = "2026.08.08",
            time = "12:00",
        )
        val remainingExpense = expense.copy(
            id = "expense-2",
            title = "택시",
            amount = 10_000,
            time = "13:00",
        )
        val expenses = mutableStateOf(listOf(expense, remainingExpense))
        var deletedExpenseId: String? = null
        composeRule.setContent {
            val currentExpenses = expenses.value
            val hasLunchExpense = currentExpenses.any { it.id == expense.id }
            GayadiTheme {
                TravelLedgerScreen(
                    tripName = "제주 여행",
                    expenses = currentExpenses,
                    schedules = listOf(schedule),
                    participants = participants,
                    settlementSummary = ExpenseSettlementSummary(
                        totalAmount = currentExpenses.sumOf(TravelExpense::amount),
                        balances = if (hasLunchExpense) {
                            listOf(
                                ParticipantExpenseBalance("local-user", 45_001, 22_500, 22_501),
                                ParticipantExpenseBalance("friend-1", 0, 22_501, -22_501),
                            )
                        } else {
                            listOf(
                                ParticipantExpenseBalance("local-user", 10_000, 5_000, 5_000),
                                ParticipantExpenseBalance("friend-1", 0, 5_000, -5_000),
                            )
                        },
                        transfers = listOf(
                            SettlementTransfer(
                                fromParticipantId = "friend-1",
                                toParticipantId = "local-user",
                                amount = if (hasLunchExpense) 22_501 else 5_000,
                            ),
                        ),
                    ),
                    onBack = {},
                    onAddExpense = {},
                    onEditExpense = { _, _ -> },
                    onDeleteExpense = { expenseId ->
                        deletedExpenseId = expenseId
                        expenses.value = expenses.value.filterNot { it.id == expenseId }
                    },
                )
            }
        }

        composeRule.onAllNodesWithText("45,001원").onFirst().assertIsDisplayed()
        composeRule.onNodeWithText("비용 2건 · 참여자 2명").assertIsDisplayed()
        composeRule.onNodeWithText("정산 내역").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("각자 정산").assertDoesNotExist()
        composeRule.onNodeWithText("사람별 정산").assertDoesNotExist()
        composeRule.onNodeWithText("여행곰 → 나").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("22,501원").assertIsDisplayed()
        composeRule.onNodeWithText("12:00 · 나 결제").assertDoesNotExist()
        composeRule.onNodeWithText("분담 나 · 여행곰").assertDoesNotExist()
        composeRule.onNodeWithContentDescription("점심 식사 메뉴").performClick()
        composeRule.onNodeWithText("삭제").performClick()
        composeRule.onNodeWithText("비용을 삭제할까요?").assertIsDisplayed()
        composeRule.onNodeWithText("삭제").performClick()

        composeRule.runOnIdle { assertEquals("expense-1", deletedExpenseId) }
        composeRule.onNodeWithText("점심 식사").assertDoesNotExist()
        composeRule.onNodeWithText("비용 1건 · 참여자 2명").assertIsDisplayed()
        composeRule.onAllNodesWithText("10,000원").onFirst().assertIsDisplayed()
        composeRule.onNodeWithText("1인당 5,000원").assertIsDisplayed()
        composeRule.onNodeWithText("45,001원").assertDoesNotExist()
        composeRule.onNodeWithText("여행곰 → 나").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("5,000원").assertIsDisplayed()
        composeRule.onNodeWithText("22,501원").assertDoesNotExist()
    }

    @Test
    fun ledgerShowsEachTransferTargetWhenMultiplePeoplePaid() {
        val participants = listOf(
            TravelParticipant("local-user", "minji"),
            TravelParticipant("friend-1", "여행곰"),
            TravelParticipant("friend-2", "바다별"),
        )
        val schedule = TravelSchedule(
            id = "schedule-1",
            tripId = "trip-28",
            title = "저녁 일정",
            date = "2026.08.08",
            time = "18:00",
            order = 0,
        )
        val localExpense = TravelExpense(
            id = "expense-local",
            tripId = "trip-28",
            scheduleId = schedule.id,
            title = "저녁 식사",
            amount = 15_000,
            payerId = "local-user",
            participantIds = participants.map(TravelParticipant::id),
            date = schedule.date,
            time = schedule.time,
        )
        val friendExpense = localExpense.copy(
            id = "expense-friend",
            title = "카페",
            amount = 12_000,
            payerId = "friend-1",
            time = "18:30",
        )

        composeRule.setContent {
            GayadiTheme {
                TravelLedgerScreen(
                    tripName = "친구 여행",
                    expenses = listOf(localExpense, friendExpense),
                    schedules = listOf(schedule),
                    participants = participants,
                    settlementSummary = ExpenseSettlementSummary(
                        totalAmount = 27_000,
                        balances = listOf(
                            ParticipantExpenseBalance("local-user", 15_000, 9_000, 6_000),
                            ParticipantExpenseBalance("friend-1", 12_000, 9_000, 3_000),
                            ParticipantExpenseBalance("friend-2", 0, 9_000, -9_000),
                        ),
                        transfers = listOf(
                            SettlementTransfer("friend-2", "local-user", 6_000),
                            SettlementTransfer("friend-2", "friend-1", 3_000),
                        ),
                    ),
                    onBack = {},
                    onAddExpense = {},
                    onEditExpense = { _, _ -> },
                    onDeleteExpense = {},
                )
            }
        }

        composeRule.onNodeWithText("정산 내역").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("바다별 → minji").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("6,000원").assertIsDisplayed()
        composeRule.onNodeWithText("바다별 → 여행곰").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("3,000원").assertIsDisplayed()
        composeRule.onNodeWithText("저녁 식사").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("카페").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("18:00 · minji 결제").assertDoesNotExist()
        composeRule.onNodeWithText("18:30 · 여행곰 결제").assertDoesNotExist()
        composeRule.onNodeWithText("분담 minji · 여행곰 · 바다별").assertDoesNotExist()
    }

    @Test
    fun ledgerShowsSettlementFailureInsteadOfMisleadingZeroTotal() {
        composeRule.setContent {
            GayadiTheme {
                TravelLedgerScreen(
                    tripName = "제주 여행",
                    expenses = emptyList(),
                    schedules = emptyList(),
                    participants = emptyList(),
                    settlementSummary = ExpenseSettlementSummary(0L, emptyList(), emptyList()),
                    settlementErrorMessage = "결제자를 선택해 주세요.",
                    onBack = {},
                    onAddExpense = {},
                    onEditExpense = { _, _ -> },
                    onDeleteExpense = {},
                )
            }
        }

        composeRule.onNodeWithText("합계 계산 불가").assertIsDisplayed()
        composeRule.onNodeWithText("정산 정보를 계산하지 못했어요").assertIsDisplayed()
        composeRule.onNodeWithText("결제자를 선택해 주세요.").assertIsDisplayed()
    }
}

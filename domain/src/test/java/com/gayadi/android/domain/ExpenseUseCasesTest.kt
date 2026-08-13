package com.gayadi.android.domain

import com.gayadi.android.domain.model.ParticipantExpenseBalance
import com.gayadi.android.domain.model.SettlementTransfer
import com.gayadi.android.domain.model.TravelExpense
import com.gayadi.android.domain.usecase.CalculateExpenseSettlementUseCase
import com.gayadi.android.domain.usecase.ValidateTravelExpenseUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExpenseUseCasesTest {
    private val validateExpense = ValidateTravelExpenseUseCase()
    private val calculateSettlement = CalculateExpenseSettlementUseCase(validateExpense)

    @Test
    fun validationAcceptsLargeLongAmountAndRejectsInvalidRequiredFields() {
        assertTrue(validateExpense(expense(amount = 3_000_000_000L)).isSuccess)
        assertTrue(validateExpense(expense(amount = 0L)).isFailure)
        assertTrue(validateExpense(expense(participantIds = emptyList())).isFailure)
        assertTrue(validateExpense(expense(participantIds = listOf("a", "a"))).isFailure)
        assertTrue(validateExpense(expense(date = "2026.02.30")).isFailure)
        assertTrue(validateExpense(expense(time = "24:00")).isFailure)
    }

    @Test
    fun equalSplitAssignsRemainderBySortedParticipantIdAndPreservesEveryWon() {
        val summary = calculateSettlement(
            listOf(
                expense(
                    amount = 101L,
                    payerId = "c",
                    participantIds = listOf("c", "a", "b"),
                ),
            ),
        )

        assertEquals(101L, summary.totalAmount)
        assertEquals(
            listOf(
                ParticipantExpenseBalance("a", paidAmount = 0L, owedAmount = 34L, netAmount = -34L),
                ParticipantExpenseBalance("b", paidAmount = 0L, owedAmount = 34L, netAmount = -34L),
                ParticipantExpenseBalance("c", paidAmount = 101L, owedAmount = 33L, netAmount = 68L),
            ),
            summary.balances,
        )
        assertEquals(summary.totalAmount, summary.balances.sumOf { it.owedAmount })
        assertEquals(0L, summary.balances.sumOf { it.netAmount })
        assertEquals(
            listOf(
                SettlementTransfer("a", "c", 34L),
                SettlementTransfer("b", "c", 34L),
            ),
            summary.transfers,
        )
    }

    @Test
    fun multipleExpensesProduceDeterministicNetBalancesAndMinimalDirectTransfers() {
        val expenses = listOf(
            expense(id = "meal", amount = 100L, payerId = "a", participantIds = listOf("a", "b", "c")),
            expense(id = "taxi", amount = 50L, payerId = "b", participantIds = listOf("b", "a")),
        )

        val summary = calculateSettlement(expenses)

        assertEquals(150L, summary.totalAmount)
        assertEquals(
            listOf(
                ParticipantExpenseBalance("a", paidAmount = 100L, owedAmount = 59L, netAmount = 41L),
                ParticipantExpenseBalance("b", paidAmount = 50L, owedAmount = 58L, netAmount = -8L),
                ParticipantExpenseBalance("c", paidAmount = 0L, owedAmount = 33L, netAmount = -33L),
            ),
            summary.balances,
        )
        assertEquals(
            listOf(
                SettlementTransfer("c", "a", 33L),
                SettlementTransfer("b", "a", 8L),
            ),
            summary.transfers,
        )
    }

    @Test
    fun participantIdsIncludePeopleWithZeroTotals() {
        val summary = calculateSettlement(
            expenses = emptyList(),
            participantIds = listOf("b", "a", "b", ""),
        )

        assertEquals(
            listOf(
                ParticipantExpenseBalance("a", paidAmount = 0L, owedAmount = 0L, netAmount = 0L),
                ParticipantExpenseBalance("b", paidAmount = 0L, owedAmount = 0L, netAmount = 0L),
            ),
            summary.balances,
        )
        assertTrue(summary.transfers.isEmpty())
    }

    private fun expense(
        id: String = "expense-1",
        amount: Long = 12_000L,
        payerId: String = "a",
        participantIds: List<String> = listOf("a"),
        date: String = "2026.08.13",
        time: String = "12:30",
    ) = TravelExpense(
        id = id,
        tripId = "trip-1",
        scheduleId = "schedule-1",
        title = "점심",
        memo = "메모",
        amount = amount,
        payerId = payerId,
        participantIds = participantIds,
        date = date,
        time = time,
    )
}

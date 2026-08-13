package com.gayadi.android.domain.usecase

import com.gayadi.android.domain.model.ExpenseSettlementSummary
import com.gayadi.android.domain.model.ParticipantExpenseBalance
import com.gayadi.android.domain.model.SettlementTransfer
import com.gayadi.android.domain.model.TravelExpense
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle

/** Validates a manually entered expense without depending on Android or persistence. */
class ValidateTravelExpenseUseCase {
    operator fun invoke(expense: TravelExpense): Result<Unit> = runCatching {
        require(expense.id.isNotBlank()) { "비용 ID가 비어 있습니다." }
        require(expense.tripId.isNotBlank()) { "여행 ID가 비어 있습니다." }
        require(expense.scheduleId.isNotBlank()) { "일정 ID가 비어 있습니다." }
        require(expense.title.isNotBlank()) { "비용 내용을 입력해 주세요." }
        require(expense.title.length <= MAX_TITLE_LENGTH) {
            "비용 내용은 ${MAX_TITLE_LENGTH}자 이하로 입력해 주세요."
        }
        require(expense.memo.length <= MAX_MEMO_LENGTH) {
            "메모는 ${MAX_MEMO_LENGTH}자 이하로 입력해 주세요."
        }
        require(expense.amount > 0L) { "금액은 1원 이상이어야 합니다." }
        require(expense.payerId.isNotBlank()) { "결제자를 선택해 주세요." }
        require(expense.participantIds.isNotEmpty()) { "분담 참여자를 한 명 이상 선택해 주세요." }
        require(expense.participantIds.all(String::isNotBlank)) { "분담 참여자 ID가 비어 있습니다." }
        require(expense.participantIds.distinct().size == expense.participantIds.size) {
            "분담 참여자는 중복될 수 없습니다."
        }
        runCatching { LocalDate.parse(expense.date, DATE_FORMATTER) }
            .getOrElse { throw IllegalArgumentException("지출 날짜 형식이 올바르지 않습니다.", it) }
        runCatching { LocalTime.parse(expense.time, TIME_FORMATTER) }
            .getOrElse { throw IllegalArgumentException("지출 시각 형식이 올바르지 않습니다.", it) }
    }

    companion object {
        const val MAX_TITLE_LENGTH = 100
        const val MAX_MEMO_LENGTH = 1_000

        private val DATE_FORMATTER = DateTimeFormatter.ofPattern("uuuu.MM.dd")
            .withResolverStyle(ResolverStyle.STRICT)
        private val TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm")
            .withResolverStyle(ResolverStyle.STRICT)
    }
}

/**
 * Calculates exact won-denominated shares, balances, and deterministic settlement suggestions.
 *
 * Remainder won are assigned to split participant IDs in ascending order. Settlement pairs are
 * ordered by the largest outstanding balance and then participant ID; every transfer clears at
 * least one debtor or creditor, so the result needs at most `debtors + creditors - 1` transfers.
 */
class CalculateExpenseSettlementUseCase(
    private val validateExpense: ValidateTravelExpenseUseCase = ValidateTravelExpenseUseCase(),
) {
    operator fun invoke(
        expenses: List<TravelExpense>,
        participantIds: Collection<String> = emptyList(),
    ): ExpenseSettlementSummary {
        val totals = linkedMapOf<String, MutableParticipantTotals>()
        participantIds.filter(String::isNotBlank).distinct().forEach { participantId ->
            totals[participantId] = MutableParticipantTotals()
        }
        var totalAmount = 0L

        expenses.forEach { expense ->
            validateExpense(expense).getOrThrow()
            totalAmount = Math.addExact(totalAmount, expense.amount)

            val payerTotals = totals.getOrPut(expense.payerId, ::MutableParticipantTotals)
            payerTotals.paid = Math.addExact(payerTotals.paid, expense.amount)

            val participantIds = expense.participantIds.sorted()
            val baseShare = expense.amount / participantIds.size
            val remainder = (expense.amount % participantIds.size).toInt()
            participantIds.forEachIndexed { index, participantId ->
                val share = baseShare + if (index < remainder) 1L else 0L
                val participantTotals = totals.getOrPut(participantId, ::MutableParticipantTotals)
                participantTotals.owed = Math.addExact(participantTotals.owed, share)
            }
        }

        val balances = totals.entries.sortedBy(Map.Entry<String, MutableParticipantTotals>::key).map { (id, total) ->
            ParticipantExpenseBalance(
                participantId = id,
                paidAmount = total.paid,
                owedAmount = total.owed,
                netAmount = total.paid - total.owed,
            )
        }
        check(balances.sumOf(ParticipantExpenseBalance::paidAmount) == totalAmount)
        check(balances.sumOf(ParticipantExpenseBalance::owedAmount) == totalAmount)

        return ExpenseSettlementSummary(
            totalAmount = totalAmount,
            balances = balances,
            transfers = calculateTransfers(balances),
        )
    }

    private fun calculateTransfers(
        balances: List<ParticipantExpenseBalance>,
    ): List<SettlementTransfer> {
        val debtors = balances.filter { it.netAmount < 0L }
            .map { MutableSettlementParty(it.participantId, -it.netAmount) }
            .sortedWith(compareByDescending<MutableSettlementParty> { it.remaining }.thenBy { it.id })
        val creditors = balances.filter { it.netAmount > 0L }
            .map { MutableSettlementParty(it.participantId, it.netAmount) }
            .sortedWith(compareByDescending<MutableSettlementParty> { it.remaining }.thenBy { it.id })
        val transfers = mutableListOf<SettlementTransfer>()
        var debtorIndex = 0
        var creditorIndex = 0

        while (debtorIndex < debtors.size && creditorIndex < creditors.size) {
            val debtor = debtors[debtorIndex]
            val creditor = creditors[creditorIndex]
            val amount = minOf(debtor.remaining, creditor.remaining)
            check(amount > 0L)
            transfers += SettlementTransfer(
                fromParticipantId = debtor.id,
                toParticipantId = creditor.id,
                amount = amount,
            )
            debtor.remaining -= amount
            creditor.remaining -= amount
            if (debtor.remaining == 0L) debtorIndex += 1
            if (creditor.remaining == 0L) creditorIndex += 1
        }

        check(debtors.all { it.remaining == 0L } && creditors.all { it.remaining == 0L })
        return transfers
    }
}

private data class MutableParticipantTotals(
    var paid: Long = 0L,
    var owed: Long = 0L,
)

private data class MutableSettlementParty(
    val id: String,
    var remaining: Long,
)

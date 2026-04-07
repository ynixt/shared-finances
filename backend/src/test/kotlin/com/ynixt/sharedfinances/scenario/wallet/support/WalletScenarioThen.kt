package com.ynixt.sharedfinances.scenario.wallet.support

import com.ynixt.sharedfinances.scenario.support.util.toBigDecimalSafe
import org.assertj.core.api.Assertions.assertThat
import java.time.LocalDate
import java.util.UUID

class WalletScenarioThen internal constructor(
    private val resolver: WalletScenarioResolver,
) {
    suspend fun availableLimitShouldBe(
        expected: Number,
        creditCardId: UUID? = null,
    ) {
        val card = resolver.resolveCreditCard(creditCardId)

        assertThat(card.balance)
            .describedAs("available limit")
            .isEqualByComparingTo(expected.toBigDecimalSafe())
    }

    suspend fun billValueShouldBe(
        expected: Number,
        billId: UUID? = null,
        billDate: LocalDate? = null,
        creditCardId: UUID? = null,
    ) {
        val resolvedBill =
            resolver.resolveBill(
                billId = billId,
                billDate = billDate,
                creditCardId = creditCardId,
            )

        assertThat(resolvedBill.value)
            .describedAs("bill value")
            .isEqualByComparingTo(expected.toBigDecimalSafe())
    }

    suspend fun billShouldExist(
        billDate: LocalDate,
        creditCardId: UUID? = null,
    ) {
        val bill =
            resolver.findBillEntity(
                billDate = billDate,
                creditCardId = creditCardId,
            )

        assertThat(bill)
            .describedAs("bill for date $billDate should exist")
            .isNotNull()
    }

    suspend fun billShouldNotExist(
        billDate: LocalDate,
        creditCardId: UUID? = null,
    ) {
        val bill =
            resolver.findBillEntity(
                billDate = billDate,
                creditCardId = creditCardId,
            )

        assertThat(bill)
            .describedAs("bill for date $billDate should not exist")
            .isNull()
    }

    suspend fun billDueDateShouldBe(
        expected: LocalDate,
        billDate: LocalDate,
        creditCardId: UUID? = null,
    ) {
        val bill =
            resolver.requireBillEntity(
                billDate = billDate,
                creditCardId = creditCardId,
            )

        assertThat(bill.dueDate)
            .describedAs("bill due date")
            .isEqualTo(expected)
    }

    suspend fun billClosingDateShouldBe(
        expected: LocalDate,
        billDate: LocalDate,
        creditCardId: UUID? = null,
    ) {
        val bill =
            resolver.requireBillEntity(
                billDate = billDate,
                creditCardId = creditCardId,
            )

        assertThat(bill.closingDate)
            .describedAs("bill closing date")
            .isEqualTo(expected)
    }

    suspend fun balanceShouldBe(
        expected: Number,
        bankAccountId: UUID? = null,
    ) {
        val item = resolver.resolveBankAccountItem(bankAccountId)

        assertThat(item.balance)
            .describedAs("balance")
            .isEqualByComparingTo(expected.toBigDecimalSafe())
    }

    suspend fun recurrenceExecutionCountShouldBe(
        expected: Int,
        recurrenceConfigId: UUID? = null,
    ) {
        val recurrence = resolver.requireRecurrenceEntity(recurrenceConfigId)

        assertThat(recurrence.qtyExecuted)
            .describedAs("recurrence execution count")
            .isEqualTo(expected)
    }

    suspend fun recurrenceLimitShouldBe(
        expected: Int?,
        recurrenceConfigId: UUID? = null,
    ) {
        val recurrence = resolver.requireRecurrenceEntity(recurrenceConfigId)

        assertThat(recurrence.qtyLimit)
            .describedAs("recurrence qty limit")
            .isEqualTo(expected)
    }

    suspend fun recurrenceNextExecutionShouldBe(
        expected: LocalDate?,
        recurrenceConfigId: UUID? = null,
    ) {
        val recurrence = resolver.requireRecurrenceEntity(recurrenceConfigId)

        assertThat(recurrence.nextExecution)
            .describedAs("recurrence next execution")
            .isEqualTo(expected)
    }
}

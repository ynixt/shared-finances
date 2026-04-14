package com.ynixt.sharedfinances.scenarios.wallet.support

import com.ynixt.sharedfinances.domain.enums.ActionEventType
import com.ynixt.sharedfinances.domain.enums.ScheduledExecutionFilter
import com.ynixt.sharedfinances.domain.models.dashboard.OverviewDashboardCardKey
import com.ynixt.sharedfinances.scenarios.support.util.toBigDecimalSafe
import org.assertj.core.api.Assertions.assertThat
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

class WalletScenarioThen internal constructor(
    private val resolver: WalletScenarioResolver,
    private val context: WalletScenarioContext,
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

    fun publishedWalletEventsShouldBe(
        type: ActionEventType,
        expected: Int,
    ) {
        val count = resolver.countPublishedWalletEvents(type)

        assertThat(count)
            .describedAs("published wallet events ($type)")
            .isEqualTo(expected)
    }

    fun fetchedWalletEventShouldExist() {
        assertThat(resolver.lastFetchedWalletEvent())
            .describedAs("fetched wallet event")
            .isNotNull()
    }

    fun fetchedWalletEventShouldNotExist() {
        assertThat(resolver.lastFetchedWalletEvent())
            .describedAs("fetched wallet event")
            .isNull()
    }

    fun fetchedWalletEventShouldContainBillDate() {
        val fetched = resolver.lastFetchedWalletEvent()

        assertThat(fetched)
            .describedAs("fetched wallet event")
            .isNotNull()

        assertThat(fetched!!.entries.any { it.billDate != null })
            .describedAs("fetched wallet event entries should contain bill date")
            .isTrue()
    }

    fun fetchedScheduledWalletEventShouldExist() {
        assertThat(resolver.lastFetchedScheduledWalletEvent())
            .describedAs("fetched scheduled wallet event")
            .isNotNull()
    }

    fun fetchedScheduledWalletEventShouldNotExist() {
        assertThat(resolver.lastFetchedScheduledWalletEvent())
            .describedAs("fetched scheduled wallet event")
            .isNull()
    }

    fun fetchedScheduledWalletEventShouldContainBillDate() {
        val fetched = resolver.lastFetchedScheduledWalletEvent()

        assertThat(fetched)
            .describedAs("fetched scheduled wallet event")
            .isNotNull()

        assertThat(fetched!!.entries.any { it.billDate != null })
            .describedAs("fetched scheduled wallet event entries should contain bill date")
            .isTrue()
    }

    fun fetchedScheduledWalletEventShouldHideTargetValue() {
        val fetched = resolver.lastFetchedScheduledWalletEvent()

        assertThat(fetched)
            .describedAs("fetched scheduled wallet event")
            .isNotNull()

        assertThat(fetched!!.targetValue)
            .describedAs("scheduled transfer target value should stay hidden")
            .isNull()
    }

    suspend fun scheduledManagerCountShouldBe(
        filter: ScheduledExecutionFilter,
        expected: Int,
    ) {
        val count = resolver.listScheduledExecutions(filter)

        assertThat(count)
            .describedAs("scheduled manager count for $filter")
            .isEqualTo(expected)
    }

    suspend fun scheduledInstallmentsShouldBe(
        filter: ScheduledExecutionFilter,
        expectedInstallments: List<Int>,
        expectedTotal: Int,
    ) {
        val entries = resolver.listScheduledExecutionEntries(filter)
        val installmentEntries = entries.filter { it.installment != null }
        val installments = installmentEntries.mapNotNull { it.installment }.sorted()

        assertThat(installments)
            .describedAs("scheduled installments for $filter")
            .containsExactlyElementsOf(expectedInstallments.sorted())

        assertThat(
            installmentEntries.all { entry ->
                val total =
                    entry.recurrenceConfig?.seriesQtyTotal
                        ?: entry.recurrenceConfig?.qtyLimit
                total == expectedTotal
            },
        ).describedAs("scheduled installment totals for $filter")
            .isTrue()
    }

    suspend fun recurrenceSeriesQtyTotalShouldBe(
        expected: Int?,
        recurrenceConfigId: UUID? = null,
    ) {
        val total = resolver.findRecurrenceSeriesQtyTotal(recurrenceConfigId)

        assertThat(total)
            .describedAs("recurrence series qty_total")
            .isEqualTo(expected)
    }

    suspend fun recurrenceSegmentsShouldBe(
        expected: Int,
        recurrenceConfigId: UUID? = null,
    ) {
        val count = resolver.countRecurrenceSegments(recurrenceConfigId)

        assertThat(count)
            .describedAs("recurrence segments count")
            .isEqualTo(expected)
    }

    suspend fun recurrenceSeriesTagsShouldBeNull(recurrenceConfigId: UUID? = null) {
        val allNull = resolver.recurrenceSeriesHasOnlyNullTags(recurrenceConfigId)

        assertThat(allNull)
            .describedAs("recurrence tags across series should remain null")
            .isTrue()
    }

    fun overviewCardShouldBe(
        key: OverviewDashboardCardKey,
        expected: Number,
    ) {
        val overview = requireNotNull(context.lastOverview) { "Overview was not fetched yet" }
        val actual = overview.cards.first { it.key == key }.value

        assertThat(actual)
            .describedAs("overview card $key")
            .isEqualByComparingTo(expected.toBigDecimalSafe())
    }

    fun overviewCardDetailLabelsShouldContain(
        key: OverviewDashboardCardKey,
        expectedLabels: Collection<String>,
    ) {
        val overview = requireNotNull(context.lastOverview) { "Overview was not fetched yet" }
        val labels = overview.cards.first { it.key == key }.details.map { it.label }

        assertThat(labels)
            .describedAs("overview card detail labels for $key")
            .containsAll(expectedLabels)
    }

    fun overviewCardDetailLabelsShouldNotContain(
        key: OverviewDashboardCardKey,
        unexpectedLabels: Collection<String>,
    ) {
        val overview = requireNotNull(context.lastOverview) { "Overview was not fetched yet" }
        val labels = overview.cards.first { it.key == key }.details.map { it.label }

        assertThat(labels)
            .describedAs("overview card detail labels for $key")
            .doesNotContainAnyElementsOf(unexpectedLabels)
    }

    fun overviewBalanceForMonthShouldBe(
        month: YearMonth,
        expected: Number,
    ) {
        val overview = requireNotNull(context.lastOverview) { "Overview was not fetched yet" }
        val actual =
            overview.charts.balance
                .first { it.month == month }
                .value

        assertThat(actual)
            .describedAs("overview balance chart for $month")
            .isEqualByComparingTo(expected.toBigDecimalSafe())
    }

    fun overviewExpenseForMonthShouldBe(
        month: YearMonth,
        expected: Number,
    ) {
        val overview = requireNotNull(context.lastOverview) { "Overview was not fetched yet" }
        val actual =
            overview.charts.expense
                .first { it.month == month }
                .value

        assertThat(actual)
            .describedAs("overview expense for $month")
            .isEqualByComparingTo(expected.toBigDecimalSafe())
    }

    fun overviewExpenseGroupSliceShouldBe(
        label: String,
        expected: Number,
    ) {
        val overview = requireNotNull(context.lastOverview) { "Overview was not fetched yet" }
        val actual =
            overview.charts.expenseByGroup
                .first { it.label == label }
                .value

        assertThat(actual)
            .describedAs("overview expense-by-group slice $label")
            .isEqualByComparingTo(expected.toBigDecimalSafe())
    }

    fun overviewExpenseCategorySliceShouldBe(
        label: String,
        expected: Number,
    ) {
        val overview = requireNotNull(context.lastOverview) { "Overview was not fetched yet" }
        val actual =
            overview.charts.expenseByCategory
                .first { it.label == label }
                .value

        assertThat(actual)
            .describedAs("overview expense-by-category slice $label")
            .isEqualByComparingTo(expected.toBigDecimalSafe())
    }

    fun overviewGoalCommittedShouldBe(expected: Number) {
        val overview = requireNotNull(context.lastOverview) { "Overview was not fetched yet" }
        assertThat(overview.goalCommittedTotal)
            .describedAs("overview goal committed total")
            .isEqualByComparingTo(expected.toBigDecimalSafe())
    }

    fun overviewFreeBalanceShouldBe(expected: Number) {
        val overview = requireNotNull(context.lastOverview) { "Overview was not fetched yet" }
        assertThat(overview.freeBalanceTotal)
            .describedAs("overview free balance")
            .isEqualByComparingTo(expected.toBigDecimalSafe())
    }

    fun overviewGoalOverCommittedWarningShouldBe(expected: Boolean) {
        val overview = requireNotNull(context.lastOverview) { "Overview was not fetched yet" }
        assertThat(overview.goalOverCommittedWarning)
            .describedAs("overview goal over-committed warning")
            .isEqualTo(expected)
    }
}

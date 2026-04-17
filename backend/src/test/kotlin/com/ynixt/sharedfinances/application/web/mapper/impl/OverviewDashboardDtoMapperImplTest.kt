package com.ynixt.sharedfinances.application.web.mapper.impl

import com.ynixt.sharedfinances.domain.models.dashboard.OverviewDashboard
import com.ynixt.sharedfinances.domain.models.dashboard.OverviewDashboardCard
import com.ynixt.sharedfinances.domain.models.dashboard.OverviewDashboardCardKey
import com.ynixt.sharedfinances.domain.models.dashboard.OverviewDashboardChartPoint
import com.ynixt.sharedfinances.domain.models.dashboard.OverviewDashboardCharts
import com.ynixt.sharedfinances.domain.models.dashboard.OverviewDashboardDetail
import com.ynixt.sharedfinances.domain.models.dashboard.OverviewDashboardDetailSourceType
import com.ynixt.sharedfinances.domain.models.dashboard.OverviewDashboardPieSlice
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.YearMonth
import java.util.UUID

class OverviewDashboardDtoMapperImplTest {
    private val mapper = OverviewDashboardDtoMapperImpl()

    @Test
    fun `should include goal cards inside cards payload preserving order and details`() {
        val goalId = UUID.randomUUID()
        val accountId = UUID.randomUUID()
        val overview =
            OverviewDashboard(
                selectedMonth = YearMonth.of(2026, 4),
                currency = "BRL",
                cards =
                    listOf(
                        OverviewDashboardCard(
                            key = OverviewDashboardCardKey.BALANCE,
                            value = BigDecimal("1000.00"),
                            details =
                                listOf(
                                    OverviewDashboardDetail(
                                        sourceId = accountId,
                                        sourceType = OverviewDashboardDetailSourceType.BANK_ACCOUNT,
                                        label = "Main",
                                        value = BigDecimal("1000.00"),
                                    ),
                                ),
                        ),
                        OverviewDashboardCard(
                            key = OverviewDashboardCardKey.GOAL_COMMITTED,
                            value = BigDecimal("250.00"),
                            details =
                                listOf(
                                    OverviewDashboardDetail(
                                        sourceId = accountId,
                                        sourceType = OverviewDashboardDetailSourceType.BANK_ACCOUNT,
                                        label = "Main",
                                        value = BigDecimal("250.00"),
                                        children =
                                            listOf(
                                                OverviewDashboardDetail(
                                                    sourceId = goalId,
                                                    sourceType = OverviewDashboardDetailSourceType.GOAL,
                                                    label = "Emergency fund",
                                                    value = BigDecimal("250.00"),
                                                ),
                                            ),
                                        accountOverCommitted = false,
                                    ),
                                ),
                        ),
                        OverviewDashboardCard(
                            key = OverviewDashboardCardKey.GOAL_FREE_BALANCE,
                            value = BigDecimal("750.00"),
                            details =
                                listOf(
                                    OverviewDashboardDetail(
                                        sourceId = null,
                                        sourceType = OverviewDashboardDetailSourceType.FORMULA,
                                        label = "financesPage.overviewPage.detail.formula.balance",
                                        value = BigDecimal("1000.00"),
                                    ),
                                    OverviewDashboardDetail(
                                        sourceId = accountId,
                                        sourceType = OverviewDashboardDetailSourceType.BANK_ACCOUNT,
                                        label = "Main",
                                        value = BigDecimal("750.00"),
                                        children =
                                            listOf(
                                                OverviewDashboardDetail(
                                                    sourceId = goalId,
                                                    sourceType = OverviewDashboardDetailSourceType.GOAL,
                                                    label = "Emergency fund",
                                                    value = BigDecimal("-250.00"),
                                                ),
                                            ),
                                        accountOverCommitted = false,
                                    ),
                                ),
                        ),
                    ),
                charts =
                    OverviewDashboardCharts(
                        balance =
                            listOf(
                                OverviewDashboardChartPoint(
                                    YearMonth.of(2026, 4),
                                    BigDecimal("1000.00"),
                                    BigDecimal("1000.00"),
                                    BigDecimal.ZERO,
                                ),
                            ),
                        cashIn = emptyList(),
                        cashOut = emptyList(),
                        expense = emptyList(),
                        cashInByCategory =
                            listOf(
                                OverviewDashboardPieSlice(
                                    id = UUID.randomUUID(),
                                    label = "Salary",
                                    value = BigDecimal("120.00"),
                                    executedValue = BigDecimal("100.00"),
                                    projectedValue = BigDecimal("20.00"),
                                ),
                            ),
                        cashOutByCategory = emptyList(),
                        expenseByGroup = emptyList(),
                        expenseByCategory = emptyList(),
                    ),
                goalCommittedTotal = BigDecimal("250.00"),
                freeBalanceTotal = BigDecimal("750.00"),
                goalOverCommittedWarning = false,
            )

        val dto = mapper.toDto(overview)

        assertThat(dto.cards.map { it.key }).containsExactly("BALANCE", "GOAL_COMMITTED", "GOAL_FREE_BALANCE")
        assertThat(dto.cards[1].details).singleElement().satisfies({ parent ->
            assertThat(parent.sourceId).isEqualTo(accountId)
            assertThat(parent.sourceType).isEqualTo("BANK_ACCOUNT")
            assertThat(parent.label).isEqualTo("Main")
            assertThat(parent.value).isEqualByComparingTo("250.00")
            assertThat(parent.accountOverCommitted).isFalse()
            assertThat(parent.children).singleElement().satisfies({ child ->
                assertThat(child.sourceId).isEqualTo(goalId)
                assertThat(child.sourceType).isEqualTo("GOAL")
                assertThat(child.label).isEqualTo("Emergency fund")
                assertThat(child.value).isEqualByComparingTo("250.00")
            })
        })
        assertThat(dto.cards[2].details).hasSize(2)
        assertThat(
            dto.cards[2].details[0].sourceType to
                dto.cards[2]
                    .details[0]
                    .value
                    .toPlainString(),
        ).isEqualTo(
            "FORMULA" to "1000.00",
        )
        assertThat(dto.cards[2].details[1]).satisfies({ main ->
            assertThat(main.sourceType).isEqualTo("BANK_ACCOUNT")
            assertThat(main.value).isEqualByComparingTo("750.00")
            assertThat(main.accountOverCommitted).isFalse()
            assertThat(main.children).singleElement().satisfies({ child ->
                assertThat(child.sourceType).isEqualTo("GOAL")
                assertThat(child.value).isEqualByComparingTo("-250.00")
            })
        })
        assertThat(dto.charts.balance).singleElement().satisfies({ point ->
            assertThat(point.value).isEqualByComparingTo("1000.00")
            assertThat(point.executedValue).isEqualByComparingTo("1000.00")
            assertThat(point.projectedValue).isEqualByComparingTo("0.00")
        })
        assertThat(dto.charts.cashInByCategory).singleElement().satisfies({ slice ->
            assertThat(slice.label).isEqualTo("Salary")
            assertThat(slice.value).isEqualByComparingTo("120.00")
            assertThat(slice.executedValue).isEqualByComparingTo("100.00")
            assertThat(slice.projectedValue).isEqualByComparingTo("20.00")
        })
    }
}

package com.ynixt.sharedfinances.application.web.mapper.impl

import com.ynixt.sharedfinances.domain.models.dashboard.OverviewDashboard
import com.ynixt.sharedfinances.domain.models.dashboard.OverviewDashboardCard
import com.ynixt.sharedfinances.domain.models.dashboard.OverviewDashboardCardKey
import com.ynixt.sharedfinances.domain.models.dashboard.OverviewDashboardChartPoint
import com.ynixt.sharedfinances.domain.models.dashboard.OverviewDashboardCharts
import com.ynixt.sharedfinances.domain.models.dashboard.OverviewDashboardDetail
import com.ynixt.sharedfinances.domain.models.dashboard.OverviewDashboardDetailSourceType
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
                                        sourceId = goalId,
                                        sourceType = OverviewDashboardDetailSourceType.GOAL,
                                        label = "Emergency fund",
                                        value = BigDecimal("250.00"),
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
                                        sourceId = goalId,
                                        sourceType = OverviewDashboardDetailSourceType.GOAL,
                                        label = "Emergency fund",
                                        value = BigDecimal("-250.00"),
                                    ),
                                ),
                        ),
                    ),
                charts =
                    OverviewDashboardCharts(
                        balance = listOf(OverviewDashboardChartPoint(YearMonth.of(2026, 4), BigDecimal("1000.00"))),
                        cashIn = emptyList(),
                        cashOut = emptyList(),
                        expense = emptyList(),
                        cashInByCategory = emptyList(),
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
        assertThat(dto.cards[1].details).singleElement().satisfies({ detail ->
            assertThat(detail.sourceId).isEqualTo(goalId)
            assertThat(detail.sourceType).isEqualTo("GOAL")
            assertThat(detail.label).isEqualTo("Emergency fund")
            assertThat(detail.value).isEqualByComparingTo("250.00")
        })
        assertThat(dto.cards[2].details.map { it.sourceType to it.value.toPlainString() }).containsExactly(
            "FORMULA" to "1000.00",
            "GOAL" to "-250.00",
        )
    }
}

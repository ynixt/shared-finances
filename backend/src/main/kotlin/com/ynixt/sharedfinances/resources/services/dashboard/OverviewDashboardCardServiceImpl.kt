package com.ynixt.sharedfinances.resources.services.dashboard

import com.ynixt.sharedfinances.domain.models.dashboard.OverviewDashboardCard
import com.ynixt.sharedfinances.domain.models.dashboard.OverviewDashboardCardKey
import com.ynixt.sharedfinances.domain.models.dashboard.OverviewDashboardDetail
import com.ynixt.sharedfinances.domain.models.dashboard.OverviewDashboardDetailSourceType
import org.springframework.stereotype.Service
import java.math.BigDecimal

@Service
internal class OverviewDashboardCardServiceImpl {
    internal fun buildCards(
        convertedDetailByCardKey: Map<OverviewDashboardCardKey, List<OverviewDashboardDetail>>,
        balanceTotal: BigDecimal,
        goalCommittedTotal: BigDecimal,
        goalCommittedDetails: List<OverviewDashboardDetail>,
        freeBalanceTotal: BigDecimal,
        freeBalanceDetails: List<OverviewDashboardDetail>,
        periodCashInTotal: BigDecimal,
        periodCashOutTotal: BigDecimal,
        periodNetCashFlowTotal: BigDecimal,
        projectedCashInTotal: BigDecimal,
        projectedCashOutTotal: BigDecimal,
        endOfPeriodNetCashFlowTotal: BigDecimal,
        expensesTotal: BigDecimal,
        projectedExpensesTotal: BigDecimal,
        periodExpensesTotal: BigDecimal,
        endOfPeriodBalanceTotal: BigDecimal,
    ): List<OverviewDashboardCard> {
        val periodNetCashFlowDetails =
            listOf(
                OverviewDashboardDetail(
                    sourceId = null,
                    sourceType = OverviewDashboardDetailSourceType.FORMULA,
                    label = "financesPage.overviewPage.detail.formula.periodCashIn",
                    value = periodCashInTotal,
                ),
                OverviewDashboardDetail(
                    sourceId = null,
                    sourceType = OverviewDashboardDetailSourceType.FORMULA,
                    label = "financesPage.overviewPage.detail.formula.periodCashOut",
                    value = periodCashOutTotal.negate().asMoney(),
                ),
            )
        val endOfPeriodBalanceDetails =
            listOf(
                OverviewDashboardDetail(
                    sourceId = null,
                    sourceType = OverviewDashboardDetailSourceType.FORMULA,
                    label = "financesPage.overviewPage.detail.formula.balance",
                    value = balanceTotal,
                ),
                OverviewDashboardDetail(
                    sourceId = null,
                    sourceType = OverviewDashboardDetailSourceType.FORMULA,
                    label = "financesPage.overviewPage.detail.formula.projectedCashIn",
                    value = projectedCashInTotal,
                ),
                OverviewDashboardDetail(
                    sourceId = null,
                    sourceType = OverviewDashboardDetailSourceType.FORMULA,
                    label = "financesPage.overviewPage.detail.formula.projectedCashOut",
                    value = projectedCashOutTotal.negate().asMoney(),
                ),
            )
        val endOfPeriodNetCashFlowDetails =
            listOf(
                OverviewDashboardDetail(
                    sourceId = null,
                    sourceType = OverviewDashboardDetailSourceType.FORMULA,
                    label = "financesPage.overviewPage.detail.formula.endOfPeriodBalance",
                    value = endOfPeriodBalanceTotal,
                ),
                OverviewDashboardDetail(
                    sourceId = null,
                    sourceType = OverviewDashboardDetailSourceType.FORMULA,
                    label = "financesPage.overviewPage.detail.formula.balance",
                    value = balanceTotal.negate().asMoney(),
                ),
            )
        val periodExpensesDetails =
            listOf(
                OverviewDashboardDetail(
                    sourceId = null,
                    sourceType = OverviewDashboardDetailSourceType.FORMULA,
                    label = "financesPage.overviewPage.cards.expenses",
                    value = expensesTotal,
                ),
                OverviewDashboardDetail(
                    sourceId = null,
                    sourceType = OverviewDashboardDetailSourceType.FORMULA,
                    label = "financesPage.overviewPage.cards.projectedExpenses",
                    value = projectedExpensesTotal,
                ),
            )

        return listOf(
            OverviewDashboardCard(
                key = OverviewDashboardCardKey.BALANCE,
                value = balanceTotal,
                details = convertedDetailByCardKey[OverviewDashboardCardKey.BALANCE].orEmpty(),
            ),
            OverviewDashboardCard(
                key = OverviewDashboardCardKey.GOAL_FREE_BALANCE,
                value = freeBalanceTotal,
                details = freeBalanceDetails,
            ),
            OverviewDashboardCard(
                key = OverviewDashboardCardKey.GOAL_COMMITTED,
                value = goalCommittedTotal,
                details = goalCommittedDetails,
            ),
            OverviewDashboardCard(
                key = OverviewDashboardCardKey.PERIOD_CASH_IN,
                value = periodCashInTotal,
                details = convertedDetailByCardKey[OverviewDashboardCardKey.PERIOD_CASH_IN].orEmpty(),
            ),
            OverviewDashboardCard(
                key = OverviewDashboardCardKey.PERIOD_CASH_OUT,
                value = periodCashOutTotal,
                details = convertedDetailByCardKey[OverviewDashboardCardKey.PERIOD_CASH_OUT].orEmpty(),
            ),
            OverviewDashboardCard(
                key = OverviewDashboardCardKey.PERIOD_NET_CASH_FLOW,
                value = periodNetCashFlowTotal,
                details = periodNetCashFlowDetails,
            ),
            OverviewDashboardCard(
                key = OverviewDashboardCardKey.PROJECTED_CASH_IN,
                value = projectedCashInTotal,
                details = convertedDetailByCardKey[OverviewDashboardCardKey.PROJECTED_CASH_IN].orEmpty(),
            ),
            OverviewDashboardCard(
                key = OverviewDashboardCardKey.PROJECTED_CASH_OUT,
                value = projectedCashOutTotal,
                details = convertedDetailByCardKey[OverviewDashboardCardKey.PROJECTED_CASH_OUT].orEmpty(),
            ),
            OverviewDashboardCard(
                key = OverviewDashboardCardKey.END_OF_PERIOD_NET_CASH_FLOW,
                value = endOfPeriodNetCashFlowTotal,
                details = endOfPeriodNetCashFlowDetails,
            ),
            OverviewDashboardCard(
                key = OverviewDashboardCardKey.EXPENSES,
                value = expensesTotal,
                details = convertedDetailByCardKey[OverviewDashboardCardKey.EXPENSES].orEmpty(),
            ),
            OverviewDashboardCard(
                key = OverviewDashboardCardKey.PROJECTED_EXPENSES,
                value = projectedExpensesTotal,
                details = convertedDetailByCardKey[OverviewDashboardCardKey.PROJECTED_EXPENSES].orEmpty(),
            ),
            OverviewDashboardCard(
                key = OverviewDashboardCardKey.PERIOD_EXPENSES,
                value = periodExpensesTotal,
                details = periodExpensesDetails,
            ),
            OverviewDashboardCard(
                key = OverviewDashboardCardKey.END_OF_PERIOD_BALANCE,
                value = endOfPeriodBalanceTotal,
                details = endOfPeriodBalanceDetails,
            ),
        )
    }
}

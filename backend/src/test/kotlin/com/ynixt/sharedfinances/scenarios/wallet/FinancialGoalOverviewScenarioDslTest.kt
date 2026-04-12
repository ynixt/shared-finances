package com.ynixt.sharedfinances.scenarios.wallet

import com.ynixt.sharedfinances.domain.models.dashboard.OverviewDashboardCardKey
import com.ynixt.sharedfinances.scenarios.wallet.support.walletScenario
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.YearMonth

class FinancialGoalOverviewScenarioDslTest {
    @Test
    fun `overview shows zero goal commitment and free balance equal to bank balance without goals`() {
        val today = LocalDate.of(2026, 1, 8)
        val selectedMonth = YearMonth.from(today)

        walletScenario(initialDate = today) {
            given {
                user(defaultCurrency = "BRL")
                bankAccount(
                    name = "Main Account",
                    balance = 1000,
                    currency = "BRL",
                )
            }

            `when` {
                fetchOverview(selectedMonth)
            }

            then {
                overviewCardShouldBe(OverviewDashboardCardKey.BALANCE, 1000)
                overviewGoalCommittedShouldBe(0)
                overviewFreeBalanceShouldBe(1000)
                overviewGoalOverCommittedWarningShouldBe(false)
            }
        }
    }
}

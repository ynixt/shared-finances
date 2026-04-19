package com.ynixt.sharedfinances.resources.services.simulation

import com.ynixt.sharedfinances.domain.models.simulation.planning.PlanningCurrencyMonthResult
import com.ynixt.sharedfinances.domain.models.simulation.planning.PlanningSimulatedExpenseRequest
import com.ynixt.sharedfinances.domain.models.simulation.planning.PlanningSimulationRequest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import tools.jackson.module.kotlin.jacksonObjectMapper
import tools.jackson.module.kotlin.readValue
import java.math.BigDecimal
import java.time.LocalDate

class PlanningSimulationPayloadContractTest {
    private val objectMapper = jacksonObjectMapper()

    @Test
    fun `request payload should use expenses field`() {
        val payload =
            PlanningSimulationRequest(
                startDate = LocalDate.of(2026, 4, 1),
                horizonMonths = 6,
                expenses =
                    listOf(
                        PlanningSimulatedExpenseRequest(
                            amount = BigDecimal("120.00"),
                            installments = 2,
                            firstPaymentDate = LocalDate.of(2026, 5, 1),
                            currency = "BRL",
                        ),
                    ),
            )

        val json = objectMapper.writeValueAsString(payload)
        val parsed = objectMapper.readTree(json)

        assertThat(parsed.has("expenses")).isTrue()
        assertThat(parsed.has("debts")).isFalse()
        assertThat(parsed["expenses"]).hasSize(1)
    }

    @Test
    fun `request payload should deserialize expenses field`() {
        val payload =
            objectMapper.readValue<PlanningSimulationRequest>(
                """
                {
                  "startDate": "2026-04-01",
                  "horizonMonths": 6,
                  "expenses": [
                    {
                      "amount": 120.00,
                      "installments": 2,
                      "firstPaymentDate": "2026-05-01",
                      "currency": "BRL"
                    }
                  ]
                }
                """.trimIndent(),
            )

        assertThat(payload.expenses).hasSize(1)
        assertThat(payload.expenses!!.single().amount).isEqualByComparingTo(BigDecimal("120.00"))
    }

    @Test
    fun `monthly result payload should expose simulated expense and debt flow fields`() {
        val payload =
            PlanningCurrencyMonthResult(
                openingBalance = BigDecimal("1000.00"),
                projectedCashFlow = BigDecimal("100.00"),
                creditCardBillOutflow = BigDecimal("50.00"),
                simulatedExpenseOutflow = BigDecimal("40.00"),
                debtOutflow = BigDecimal("15.00"),
                debtInflow = BigDecimal("25.00"),
                closingBalance = BigDecimal("1020.00"),
                scheduledGoalContribution = BigDecimal("20.00"),
                closingBalanceWithGoalContributions = BigDecimal("1000.00"),
            )

        val json = objectMapper.writeValueAsString(payload)
        val parsed = objectMapper.readTree(json)

        assertThat(parsed.has("simulatedExpenseOutflow")).isTrue()
        assertThat(parsed.has("debtOutflow")).isTrue()
        assertThat(parsed.has("debtInflow")).isTrue()
    }
}

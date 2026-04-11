package com.ynixt.sharedfinances.scenarios.wallet

import com.ynixt.sharedfinances.domain.exceptions.http.InvalidWalletSourceSplitException
import com.ynixt.sharedfinances.scenarios.wallet.support.walletScenario
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

class MultiOriginWalletScenarioDslTest {
    @Test
    fun `multi origin expense splits balance impact by percentage`() {
        val today = LocalDate.of(2026, 1, 8)
        lateinit var idA: UUID
        lateinit var idB: UUID

        walletScenario(initialDate = today) {
            given {
                user(defaultCurrency = "BRL")
                idA = bankAccount(name = "A", balance = BigDecimal("1000.00"), currency = "BRL")
                idB = bankAccount(name = "B", balance = BigDecimal("1000.00"), currency = "BRL")
            }

            `when` {
                multiOriginExpense(
                    value = BigDecimal("100.00"),
                    date = today,
                    sources =
                        listOf(
                            idA to BigDecimal("60.00"),
                            idB to BigDecimal("40.00"),
                        ),
                )
            }

            then {
                balanceShouldBe(BigDecimal("940.00"), idA)
                balanceShouldBe(BigDecimal("960.00"), idB)
            }
        }
    }

    @Test
    fun `reject multi origin when percents do not sum to 100`() {
        val today = LocalDate.of(2026, 1, 8)
        lateinit var idA: UUID
        lateinit var idB: UUID

        assertThrows<InvalidWalletSourceSplitException> {
            runBlocking {
                walletScenario(initialDate = today) {
                    given {
                        user(defaultCurrency = "BRL")
                        idA = bankAccount(balance = BigDecimal("1000.00"), currency = "BRL")
                        idB = bankAccount(balance = BigDecimal("1000.00"), currency = "BRL")
                    }

                    `when` {
                        multiOriginExpense(
                            value = BigDecimal("100.00"),
                            date = today,
                            sources =
                                listOf(
                                    idA to BigDecimal("60.00"),
                                    idB to BigDecimal("50.00"),
                                ),
                        )
                    }
                }
            }
        }
    }

    @Test
    fun `installment template with split materializes same split on each occurrence`() {
        val today = LocalDate.of(2026, 1, 8)
        lateinit var idA: UUID
        lateinit var idB: UUID

        walletScenario(initialDate = today) {
            given {
                user(defaultCurrency = "BRL")
                idA = bankAccount(balance = BigDecimal("1000.00"), currency = "BRL")
                idB = bankAccount(balance = BigDecimal("1000.00"), currency = "BRL")
            }

            `when` {
                multiOriginInstallmentPurchase(
                    total = BigDecimal("200.00"),
                    installments = 2,
                    date = today,
                    sources =
                        listOf(
                            idA to BigDecimal("50.00"),
                            idB to BigDecimal("50.00"),
                        ),
                )
            }

            then {
                balanceShouldBe(BigDecimal("950.00"), idA)
                balanceShouldBe(BigDecimal("950.00"), idB)
            }

            `when` {
                advanceTimeToNextRecurrenceExecution()
                runRecurrence()
            }

            then {
                balanceShouldBe(BigDecimal("900.00"), idA)
                balanceShouldBe(BigDecimal("900.00"), idB)
            }
        }
    }
}

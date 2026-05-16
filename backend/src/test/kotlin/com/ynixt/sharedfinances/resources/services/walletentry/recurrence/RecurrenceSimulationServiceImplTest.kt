package com.ynixt.sharedfinances.resources.services.walletentry.recurrence

import com.ynixt.sharedfinances.domain.enums.PaymentType
import com.ynixt.sharedfinances.domain.enums.RecurrenceType
import com.ynixt.sharedfinances.domain.enums.WalletEntryType
import com.ynixt.sharedfinances.domain.models.walletentry.NewEntryRequest
import com.ynixt.sharedfinances.domain.models.walletentry.NewWalletBeneficiaryLeg
import com.ynixt.sharedfinances.resources.services.groups.deriveProjectedDebtMovementsFromEvent
import com.ynixt.sharedfinances.scenarios.support.ScenarioGroupService
import com.ynixt.sharedfinances.scenarios.support.ScenarioRuntime
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

class RecurrenceSimulationServiceImplTest {
    @Test
    fun `simulated group credit card installments should keep beneficiaries for projected debt derivation`() {
        runBlocking {
            val today = LocalDate.of(2026, 5, 14)
            val groupService = ScenarioGroupService()
            val groupId = groupService.createGroup(name = "Debt projections")
            val runtime = ScenarioRuntime(initialDate = today, groupService = groupService)

            val gabriel =
                runtime.userService.createUser(
                    com.ynixt.sharedfinances.application.web.dto.auth.RegisterDto(
                        email = "gabriel@example.com",
                        password = "password123",
                        firstName = "Gabriel",
                        lastName = "User",
                        lang = "pt-BR",
                        defaultCurrency = "BRL",
                        tmz = "America/Sao_Paulo",
                        acceptTerms = true,
                        acceptPrivacy = true,
                        gravatarOptIn = false,
                    ),
                )
            val debora =
                runtime.userService.createUser(
                    com.ynixt.sharedfinances.application.web.dto.auth.RegisterDto(
                        email = "debora@example.com",
                        password = "password123",
                        firstName = "Debora",
                        lastName = "User",
                        lang = "pt-BR",
                        defaultCurrency = "BRL",
                        tmz = "America/Sao_Paulo",
                        acceptTerms = true,
                        acceptPrivacy = true,
                        gravatarOptIn = false,
                    ),
                )

            val gabrielId = requireNotNull(gabriel.id)
            val deboraId = requireNotNull(debora.id)

            val creditCard =
                runtime.creditCardService.create(
                    userId = gabrielId,
                    request =
                        com.ynixt.sharedfinances.domain.models.creditcard.NewCreditCardRequest(
                            name = "Gabriel card",
                            currency = "BRL",
                            totalLimit = BigDecimal("5000.00"),
                            dueDay = 10,
                            daysBetweenDueAndClosing = 10,
                            dueOnNextBusinessDay = false,
                            showOnDashboard = true,
                        ),
                )

            val creditCardId = requireNotNull(creditCard.id)

            groupService.upsertMemberScope(
                groupId = groupId,
                userId = gabrielId,
                associatedItemIds = setOf(creditCardId),
            )
            groupService.upsertMemberScope(
                groupId = groupId,
                userId = deboraId,
                associatedItemIds = emptySet(),
            )

            runtime.walletEntryCreateService.create(
                userId = gabrielId,
                newEntryRequest =
                    NewEntryRequest(
                        type = WalletEntryType.EXPENSE,
                        groupId = groupId,
                        originId = creditCardId,
                        date = LocalDate.of(2026, 4, 26),
                        value = BigDecimal("87.41"),
                        name = "Relogio Debora",
                        confirmed = true,
                        paymentType = PaymentType.INSTALLMENTS,
                        installments = 5,
                        periodicity = RecurrenceType.MONTHLY,
                        originBillDate = LocalDate.of(2026, 5, 1),
                        beneficiaries =
                            listOf(
                                NewWalletBeneficiaryLeg(
                                    userId = deboraId,
                                    benefitPercent = BigDecimal("100.00"),
                                ),
                            ),
                    ),
            )

            val simulatedEvents =
                runtime.recurrenceSimulationService.simulateGenerationWithFilters(
                    minimumEndExecution = LocalDate.of(2026, 5, 1),
                    maximumNextExecution = LocalDate.of(2026, 6, 30),
                    userId = gabrielId,
                    walletItemId = null,
                    billDate = null,
                    groupIds = setOf(groupId),
                    userIds = emptySet(),
                    walletItemIds = emptySet(),
                    entryTypes = setOf(WalletEntryType.EXPENSE),
                    categoryConceptIds = emptySet(),
                    includeUncategorized = false,
                )

            val secondInstallment =
                simulatedEvents.single { simulated ->
                    simulated.installment == 2 &&
                        simulated.entries.single().billDate == LocalDate.of(2026, 6, 1)
                }
            val beneficiary = secondInstallment.beneficiaries.orEmpty().single()
            assertThat(beneficiary.userId).isEqualTo(deboraId)
            assertThat(beneficiary.benefitPercent).isEqualByComparingTo("100.00")
            assertThat(secondInstallment.installment).isEqualTo(2)
            assertThat(secondInstallment.entries.single().billDate).isEqualTo(LocalDate.of(2026, 6, 1))
            assertThat(
                secondInstallment.entries
                    .single()
                    .walletItem.user
                    ?.id,
            ).isEqualTo(gabrielId)

            val projected = deriveProjectedDebtMovementsFromEvent(secondInstallment).single()
            assertThat(projected.payerId).isEqualTo(deboraId)
            assertThat(projected.receiverId).isEqualTo(gabrielId)
            assertThat(projected.amount).isEqualByComparingTo("87.41")
            assertThat(projected.month).isEqualTo(YearMonth.of(2026, 6))
        }
    }
}

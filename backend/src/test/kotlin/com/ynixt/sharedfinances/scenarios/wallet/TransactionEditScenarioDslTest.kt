package com.ynixt.sharedfinances.scenarios.wallet

import com.ynixt.sharedfinances.domain.enums.ActionEventType
import com.ynixt.sharedfinances.domain.enums.GroupPermissions
import com.ynixt.sharedfinances.domain.enums.PaymentType
import com.ynixt.sharedfinances.domain.enums.RecurrenceType
import com.ynixt.sharedfinances.domain.enums.ScheduledEditScope
import com.ynixt.sharedfinances.domain.enums.ScheduledExecutionFilter
import com.ynixt.sharedfinances.domain.enums.WalletEntryType
import com.ynixt.sharedfinances.domain.exceptions.http.InvalidPastScheduledExecutionEditException
import com.ynixt.sharedfinances.domain.exceptions.http.InvalidRecurrenceQtyLimitException
import com.ynixt.sharedfinances.domain.exceptions.http.InvalidWalletSourceSplitException
import com.ynixt.sharedfinances.domain.exceptions.http.UnauthorizedException
import com.ynixt.sharedfinances.domain.models.creditcard.CreditCard
import com.ynixt.sharedfinances.domain.models.walletentry.NewEntryRequest
import com.ynixt.sharedfinances.domain.models.walletentry.NewWalletSourceLeg
import com.ynixt.sharedfinances.scenarios.support.ScenarioGroupService
import com.ynixt.sharedfinances.scenarios.wallet.support.walletScenario
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

class TransactionEditScenarioDslTest {
    @Test
    fun `should allow owner to fetch own one-off transaction by id`() {
        val today = LocalDate.of(2026, 1, 8)
        lateinit var bankAccountId: UUID

        walletScenario(initialDate = today) {
            given {
                user(defaultCurrency = "BRL")
                bankAccountId = bankAccount(balance = BigDecimal("500.00"), currency = "BRL")
            }

            `when` {
                expense(
                    value = BigDecimal("40.00"),
                    originId = bankAccountId,
                    date = today,
                    confirmed = true,
                )
                fetchWalletEventById()
            }

            then {
                fetchedWalletEventShouldExist()
            }
        }
    }

    @Test
    fun `should deny fetch by id for non-owner on user-owned transaction`() {
        val today = LocalDate.of(2026, 1, 8)
        lateinit var ownerEventId: UUID

        walletScenario(initialDate = today) {
            given {
                user(email = "owner@example.com", defaultCurrency = "BRL")
                bankAccount(balance = BigDecimal("500.00"), currency = "BRL")
            }

            `when` {
                expense(
                    value = BigDecimal("40.00"),
                    date = today,
                    confirmed = true,
                )
                ownerEventId = lastWalletEventId()
            }

            given {
                user(email = "outsider@example.com", defaultCurrency = "BRL")
            }

            `when` {
                fetchWalletEventById(ownerEventId)
            }

            then {
                fetchedWalletEventShouldNotExist()
            }
        }
    }

    @Test
    fun `should allow owner to fetch scheduled edit payload by recurrence config id`() {
        val today = LocalDate.of(2026, 1, 8)
        val firstOccurrence = today.plusDays(2)
        lateinit var bankAccountId: UUID

        walletScenario(initialDate = today) {
            given {
                user(defaultCurrency = "BRL")
                bankAccountId = bankAccount(balance = BigDecimal("500.00"), currency = "BRL")
            }

            `when` {
                createEntry(
                    NewEntryRequest(
                        type = WalletEntryType.EXPENSE,
                        originId = bankAccountId,
                        date = firstOccurrence,
                        value = BigDecimal("40.00"),
                        confirmed = true,
                        paymentType = PaymentType.RECURRING,
                        periodicity = RecurrenceType.MONTHLY,
                        periodicityQtyLimit = 6,
                    ),
                )
                fetchScheduledByRecurrenceConfigId()
            }

            then {
                fetchedScheduledWalletEventShouldExist()
            }
        }
    }

    @Test
    fun `should deny scheduled edit payload fetch by recurrence config id for non-owner`() {
        val today = LocalDate.of(2026, 1, 8)
        val firstOccurrence = today.plusDays(2)
        lateinit var ownerBankAccountId: UUID

        walletScenario(initialDate = today) {
            given {
                user(email = "scheduled-owner@example.com", defaultCurrency = "BRL")
                ownerBankAccountId = bankAccount(balance = BigDecimal("500.00"), currency = "BRL")
            }

            `when` {
                createEntry(
                    NewEntryRequest(
                        type = WalletEntryType.EXPENSE,
                        originId = ownerBankAccountId,
                        date = firstOccurrence,
                        value = BigDecimal("40.00"),
                        confirmed = true,
                        paymentType = PaymentType.RECURRING,
                        periodicity = RecurrenceType.MONTHLY,
                        periodicityQtyLimit = 6,
                    ),
                )
            }

            given {
                user(email = "scheduled-outsider@example.com", defaultCurrency = "BRL")
            }

            `when` {
                fetchScheduledByRecurrenceConfigId()
            }

            then {
                fetchedScheduledWalletEventShouldNotExist()
            }
        }
    }

    @Test
    fun `should return no scheduled edit payload for unknown recurrence config id`() {
        val today = LocalDate.of(2026, 1, 8)

        walletScenario(initialDate = today) {
            given {
                user(defaultCurrency = "BRL")
            }

            `when` {
                fetchScheduledByRecurrenceConfigId(UUID.randomUUID())
            }

            then {
                fetchedScheduledWalletEventShouldNotExist()
            }
        }
    }

    @Test
    fun `should include bill date when fetching credit card transaction by id`() {
        val today = LocalDate.of(2026, 1, 8)
        lateinit var creditCardId: UUID

        walletScenario(initialDate = today) {
            given {
                user(defaultCurrency = "BRL")
                creditCardId =
                    creditCard(
                        currency = "BRL",
                        limit = BigDecimal("4000.00"),
                        dueDay = 20,
                        daysBetweenDueAndClosing = 10,
                        dueOnNextBusinessDay = false,
                    )
            }

            `when` {
                expense(
                    value = BigDecimal("200.00"),
                    originId = creditCardId,
                    date = today,
                    confirmed = true,
                )
                fetchWalletEventById()
            }

            then {
                fetchedWalletEventShouldExist()
                fetchedWalletEventShouldContainBillDate()
            }
        }
    }

    @Test
    fun `should include bill date when fetching scheduled credit card transaction by recurrence config id`() {
        val today = LocalDate.of(2026, 1, 8)
        val firstOccurrence = today.plusDays(2)
        val dueDay = 20
        val daysBetweenDueAndClosing = 10
        val dueOnNextBusinessDay = false
        val expectedCardModel =
            CreditCard(
                name = "Expected Card",
                enabled = true,
                userId = UUID.randomUUID(),
                currency = "BRL",
                totalLimit = BigDecimal("4000.00"),
                balance = BigDecimal("4000.00"),
                dueDay = dueDay,
                daysBetweenDueAndClosing = daysBetweenDueAndClosing,
                dueOnNextBusinessDay = dueOnNextBusinessDay,
            )
        val billDate = expectedCardModel.getBestBill(firstOccurrence)
        lateinit var creditCardId: UUID

        walletScenario(initialDate = today) {
            given {
                user(defaultCurrency = "BRL")
                creditCardId =
                    creditCard(
                        currency = "BRL",
                        limit = BigDecimal("4000.00"),
                        dueDay = dueDay,
                        daysBetweenDueAndClosing = daysBetweenDueAndClosing,
                        dueOnNextBusinessDay = dueOnNextBusinessDay,
                    )
            }

            `when` {
                createEntry(
                    NewEntryRequest(
                        type = WalletEntryType.EXPENSE,
                        originId = creditCardId,
                        date = firstOccurrence,
                        value = BigDecimal("200.00"),
                        confirmed = true,
                        paymentType = PaymentType.RECURRING,
                        periodicity = RecurrenceType.MONTHLY,
                        periodicityQtyLimit = 3,
                        originBillDate = billDate,
                    ),
                )
                fetchScheduledByRecurrenceConfigId()
            }

            then {
                fetchedScheduledWalletEventShouldExist()
                fetchedScheduledWalletEventShouldContainBillDate()
            }
        }
    }

    @Test
    fun `should allow fetch by id for member in same group`() {
        val today = LocalDate.of(2026, 1, 8)
        val groupService = ScenarioGroupService()
        val groupId = groupService.createGroup(name = "Shared Group")

        lateinit var ownerId: UUID
        lateinit var memberId: UUID
        lateinit var groupBankAccountId: UUID
        lateinit var groupEventId: UUID

        walletScenario(initialDate = today, groupService = groupService) {
            given {
                ownerId = user(email = "group-owner@example.com", defaultCurrency = "BRL")
                groupBankAccountId = bankAccount(balance = BigDecimal("1000.00"), currency = "BRL")
            }

            `when` {
                groupService.upsertMemberScope(
                    groupId = groupId,
                    userId = ownerId,
                    permissions = GroupPermissions.entries.toSet(),
                    associatedItemIds = setOf(groupBankAccountId),
                )

                createEntry(
                    NewEntryRequest(
                        type = WalletEntryType.EXPENSE,
                        groupId = groupId,
                        originId = groupBankAccountId,
                        date = today,
                        value = BigDecimal("25.00"),
                        confirmed = true,
                        paymentType = PaymentType.UNIQUE,
                    ),
                )

                groupEventId = lastWalletEventId()
            }

            given {
                memberId = user(email = "group-member@example.com", defaultCurrency = "BRL")
            }

            `when` {
                groupService.upsertMemberScope(
                    groupId = groupId,
                    userId = memberId,
                    permissions = emptySet(),
                )

                fetchWalletEventById(groupEventId)
            }

            then {
                fetchedWalletEventShouldExist()
            }
        }
    }

    @Test
    fun `should deny fetch by id for non-member on group transaction`() {
        val today = LocalDate.of(2026, 1, 8)
        val groupService = ScenarioGroupService()
        val groupId = groupService.createGroup(name = "Shared Group")

        lateinit var ownerId: UUID
        lateinit var groupBankAccountId: UUID
        lateinit var groupEventId: UUID

        walletScenario(initialDate = today, groupService = groupService) {
            given {
                ownerId = user(email = "group-owner-deny@example.com", defaultCurrency = "BRL")
                groupBankAccountId = bankAccount(balance = BigDecimal("1000.00"), currency = "BRL")
            }

            `when` {
                groupService.upsertMemberScope(
                    groupId = groupId,
                    userId = ownerId,
                    permissions = GroupPermissions.entries.toSet(),
                    associatedItemIds = setOf(groupBankAccountId),
                )

                createEntry(
                    NewEntryRequest(
                        type = WalletEntryType.EXPENSE,
                        groupId = groupId,
                        originId = groupBankAccountId,
                        date = today,
                        value = BigDecimal("25.00"),
                        confirmed = true,
                        paymentType = PaymentType.UNIQUE,
                    ),
                )

                groupEventId = lastWalletEventId()
            }

            given {
                user(email = "group-outsider@example.com", defaultCurrency = "BRL")
            }

            `when` {
                fetchWalletEventById(groupEventId)
            }

            then {
                fetchedWalletEventShouldNotExist()
            }
        }
    }

    @Test
    fun `should deny group create without SEND_ENTRIES keeping original financial state`() {
        val today = LocalDate.of(2026, 1, 8)
        val groupService = ScenarioGroupService()
        val groupId = groupService.createGroup(name = "Shared Group")
        lateinit var userId: UUID
        lateinit var groupBankAccountId: UUID
        var unauthorizedThrown = false

        walletScenario(initialDate = today, groupService = groupService) {
            given {
                userId = user(defaultCurrency = "BRL")
                groupBankAccountId = bankAccount(balance = BigDecimal("1000.00"), currency = "BRL")
            }

            `when` {
                groupService.upsertMemberScope(
                    groupId = groupId,
                    userId = userId,
                    permissions = emptySet(),
                    associatedItemIds = setOf(groupBankAccountId),
                )
                clearPublishedEvents()

                try {
                    createEntry(
                        NewEntryRequest(
                            type = WalletEntryType.EXPENSE,
                            groupId = groupId,
                            originId = groupBankAccountId,
                            date = today,
                            value = BigDecimal("25.00"),
                            confirmed = true,
                            paymentType = PaymentType.UNIQUE,
                        ),
                    )
                } catch (_: UnauthorizedException) {
                    unauthorizedThrown = true
                }
            }

            then {
                assertThat(unauthorizedThrown).isTrue()
                balanceShouldBe(BigDecimal("1000.00"), groupBankAccountId)
                publishedWalletEventsShouldBe(ActionEventType.INSERT, 0)
            }
        }
    }

    @Test
    fun `should edit one-off transaction recomputing posted balance and publishing update event`() {
        val today = LocalDate.of(2026, 1, 8)
        val initialBalance = BigDecimal("1000.00")
        lateinit var bankAccountId: UUID

        walletScenario(initialDate = today) {
            given {
                user(defaultCurrency = "BRL")
                bankAccountId =
                    bankAccount(
                        balance = initialBalance,
                        currency = "BRL",
                    )
            }

            `when` {
                expense(
                    value = BigDecimal("100.00"),
                    originId = bankAccountId,
                    date = today,
                    confirmed = true,
                )
            }

            then {
                balanceShouldBe(BigDecimal("900.00"), bankAccountId)
            }

            `when` {
                clearPublishedEvents()
                editOneOff(
                    newEntryRequest =
                        NewEntryRequest(
                            type = WalletEntryType.EXPENSE,
                            originId = bankAccountId,
                            date = today,
                            value = BigDecimal("50.00"),
                            confirmed = true,
                            paymentType = PaymentType.UNIQUE,
                        ),
                )
            }

            then {
                balanceShouldBe(BigDecimal("950.00"), bankAccountId)
                publishedWalletEventsShouldBe(ActionEventType.UPDATE, 1)
            }
        }
    }

    @Test
    fun `should allow changing one-off expense to revenue during edit`() {
        val today = LocalDate.of(2026, 1, 8)
        lateinit var bankAccountId: UUID

        walletScenario(initialDate = today) {
            given {
                user(defaultCurrency = "BRL")
                bankAccountId = bankAccount(balance = BigDecimal("1000.00"), currency = "BRL")
            }

            `when` {
                expense(
                    value = BigDecimal("100.00"),
                    originId = bankAccountId,
                    date = today,
                    confirmed = true,
                )
            }

            then {
                balanceShouldBe(BigDecimal("900.00"), bankAccountId)
            }

            `when` {
                clearPublishedEvents()
                editOneOff(
                    newEntryRequest =
                        NewEntryRequest(
                            type = WalletEntryType.REVENUE,
                            originId = bankAccountId,
                            date = today,
                            value = BigDecimal("70.00"),
                            confirmed = true,
                            paymentType = PaymentType.UNIQUE,
                        ),
                )
            }

            then {
                balanceShouldBe(BigDecimal("1070.00"), bankAccountId)
                publishedWalletEventsShouldBe(ActionEventType.UPDATE, 1)
            }
        }
    }

    @Test
    fun `should reject changing one-off transfer to expense during edit`() {
        val today = LocalDate.of(2026, 1, 8)
        lateinit var originId: UUID
        lateinit var targetId: UUID

        walletScenario(initialDate = today) {
            given {
                user(defaultCurrency = "BRL")
                originId = bankAccount(name = "Origin", balance = BigDecimal("1000.00"), currency = "BRL")
                targetId = bankAccount(name = "Target", balance = BigDecimal("500.00"), currency = "BRL")
            }

            `when` {
                transfer(
                    value = BigDecimal("100.00"),
                    originId = originId,
                    targetId = targetId,
                    date = today,
                    confirmed = true,
                )
            }

            then {
                balanceShouldBe(BigDecimal("900.00"), originId)
                balanceShouldBe(BigDecimal("600.00"), targetId)
            }

            `when` {
                clearPublishedEvents()
                editOneOff(
                    newEntryRequest =
                        NewEntryRequest(
                            type = WalletEntryType.EXPENSE,
                            originId = originId,
                            date = today,
                            value = BigDecimal("50.00"),
                            confirmed = true,
                            paymentType = PaymentType.UNIQUE,
                        ),
                )
            }

            then {
                balanceShouldBe(BigDecimal("900.00"), originId)
                balanceShouldBe(BigDecimal("600.00"), targetId)
                publishedWalletEventsShouldBe(ActionEventType.UPDATE, 0)
            }
        }
    }

    @Test
    fun `should reject changing one-off expense to transfer during edit`() {
        val today = LocalDate.of(2026, 1, 8)
        lateinit var originId: UUID
        lateinit var targetId: UUID

        walletScenario(initialDate = today) {
            given {
                user(defaultCurrency = "BRL")
                originId = bankAccount(name = "Origin", balance = BigDecimal("1000.00"), currency = "BRL")
                targetId = bankAccount(name = "Target", balance = BigDecimal("500.00"), currency = "BRL")
            }

            `when` {
                expense(
                    value = BigDecimal("100.00"),
                    originId = originId,
                    date = today,
                    confirmed = true,
                )
            }

            then {
                balanceShouldBe(BigDecimal("900.00"), originId)
                balanceShouldBe(BigDecimal("500.00"), targetId)
            }

            `when` {
                clearPublishedEvents()
                editOneOff(
                    newEntryRequest =
                        NewEntryRequest(
                            type = WalletEntryType.TRANSFER,
                            originId = originId,
                            targetId = targetId,
                            date = today,
                            value = null,
                            originValue = BigDecimal("50.00"),
                            confirmed = true,
                            paymentType = PaymentType.UNIQUE,
                        ),
                )
            }

            then {
                balanceShouldBe(BigDecimal("900.00"), originId)
                balanceShouldBe(BigDecimal("500.00"), targetId)
                publishedWalletEventsShouldBe(ActionEventType.UPDATE, 0)
            }
        }
    }

    @Test
    fun `should reject changing user-owned transaction to group during edit`() {
        val today = LocalDate.of(2026, 1, 8)
        lateinit var bankAccountId: UUID

        walletScenario(initialDate = today) {
            given {
                user(defaultCurrency = "BRL")
                bankAccountId = bankAccount(balance = BigDecimal("1000.00"), currency = "BRL")
            }

            `when` {
                expense(
                    value = BigDecimal("100.00"),
                    originId = bankAccountId,
                    date = today,
                    confirmed = true,
                )
            }

            then {
                balanceShouldBe(BigDecimal("900.00"), bankAccountId)
            }

            `when` {
                clearPublishedEvents()
                editOneOff(
                    newEntryRequest =
                        NewEntryRequest(
                            type = WalletEntryType.EXPENSE,
                            groupId = UUID.randomUUID(),
                            originId = bankAccountId,
                            date = today,
                            value = BigDecimal("50.00"),
                            confirmed = true,
                            paymentType = PaymentType.UNIQUE,
                        ),
                )
            }

            then {
                balanceShouldBe(BigDecimal("900.00"), bankAccountId)
                publishedWalletEventsShouldBe(ActionEventType.UPDATE, 0)
            }
        }
    }

    @Test
    fun `should reject changing group transaction to individual during edit`() {
        val today = LocalDate.of(2026, 1, 8)
        val groupService = ScenarioGroupService()
        val groupId = groupService.createGroup(name = "Shared Group")
        lateinit var userId: UUID
        lateinit var groupBankAccountId: UUID
        lateinit var walletEventId: UUID

        walletScenario(initialDate = today, groupService = groupService) {
            given {
                userId = user(defaultCurrency = "BRL")
                groupBankAccountId = bankAccount(balance = BigDecimal("1000.00"), currency = "BRL")
            }

            `when` {
                groupService.upsertMemberScope(
                    groupId = groupId,
                    userId = userId,
                    permissions = GroupPermissions.entries.toSet(),
                    associatedItemIds = setOf(groupBankAccountId),
                )
                createEntry(
                    NewEntryRequest(
                        type = WalletEntryType.EXPENSE,
                        groupId = groupId,
                        originId = groupBankAccountId,
                        date = today,
                        value = BigDecimal("100.00"),
                        confirmed = true,
                        paymentType = PaymentType.UNIQUE,
                    ),
                )
                walletEventId = lastWalletEventId()
            }

            then {
                balanceShouldBe(BigDecimal("900.00"), groupBankAccountId)
            }

            `when` {
                clearPublishedEvents()
                editOneOff(
                    walletEventId = walletEventId,
                    newEntryRequest =
                        NewEntryRequest(
                            type = WalletEntryType.EXPENSE,
                            groupId = null,
                            originId = groupBankAccountId,
                            date = today,
                            value = BigDecimal("50.00"),
                            confirmed = true,
                            paymentType = PaymentType.UNIQUE,
                        ),
                )
            }

            then {
                balanceShouldBe(BigDecimal("900.00"), groupBankAccountId)
                publishedWalletEventsShouldBe(ActionEventType.UPDATE, 0)
            }
        }
    }

    @Test
    fun `should reject changing group transaction to another group during edit`() {
        val today = LocalDate.of(2026, 1, 8)
        val groupService = ScenarioGroupService()
        val sourceGroupId = groupService.createGroup(name = "Source Group")
        val targetGroupId = groupService.createGroup(name = "Target Group")
        lateinit var userId: UUID
        lateinit var groupBankAccountId: UUID
        lateinit var walletEventId: UUID

        walletScenario(initialDate = today, groupService = groupService) {
            given {
                userId = user(defaultCurrency = "BRL")
                groupBankAccountId = bankAccount(balance = BigDecimal("1000.00"), currency = "BRL")
            }

            `when` {
                groupService.upsertMemberScope(
                    groupId = sourceGroupId,
                    userId = userId,
                    permissions = GroupPermissions.entries.toSet(),
                    associatedItemIds = setOf(groupBankAccountId),
                )
                createEntry(
                    NewEntryRequest(
                        type = WalletEntryType.EXPENSE,
                        groupId = sourceGroupId,
                        originId = groupBankAccountId,
                        date = today,
                        value = BigDecimal("100.00"),
                        confirmed = true,
                        paymentType = PaymentType.UNIQUE,
                    ),
                )
                walletEventId = lastWalletEventId()
            }

            then {
                balanceShouldBe(BigDecimal("900.00"), groupBankAccountId)
            }

            `when` {
                clearPublishedEvents()
                editOneOff(
                    walletEventId = walletEventId,
                    newEntryRequest =
                        NewEntryRequest(
                            type = WalletEntryType.EXPENSE,
                            groupId = targetGroupId,
                            originId = groupBankAccountId,
                            date = today,
                            value = BigDecimal("50.00"),
                            confirmed = true,
                            paymentType = PaymentType.UNIQUE,
                        ),
                )
            }

            then {
                balanceShouldBe(BigDecimal("900.00"), groupBankAccountId)
                publishedWalletEventsShouldBe(ActionEventType.UPDATE, 0)
            }
        }
    }

    @Test
    fun `should reject changing group ownership during scheduled edit`() {
        val today = LocalDate.of(2026, 1, 8)
        val firstOccurrence = today.plusDays(1)
        val selectedOccurrence = firstOccurrence.plusMonths(1)
        val groupService = ScenarioGroupService()
        val sourceGroupId = groupService.createGroup(name = "Source Group")
        lateinit var userId: UUID
        lateinit var groupBankAccountId: UUID

        walletScenario(initialDate = today, groupService = groupService) {
            given {
                userId = user(defaultCurrency = "BRL")
                groupBankAccountId = bankAccount(balance = BigDecimal("1000.00"), currency = "BRL")
            }

            `when` {
                groupService.upsertMemberScope(
                    groupId = sourceGroupId,
                    userId = userId,
                    permissions = GroupPermissions.entries.toSet(),
                    associatedItemIds = setOf(groupBankAccountId),
                )
                createEntry(
                    NewEntryRequest(
                        type = WalletEntryType.EXPENSE,
                        groupId = sourceGroupId,
                        originId = groupBankAccountId,
                        date = firstOccurrence,
                        value = BigDecimal("100.00"),
                        confirmed = true,
                        paymentType = PaymentType.RECURRING,
                        periodicity = RecurrenceType.MONTHLY,
                        periodicityQtyLimit = 3,
                    ),
                )
            }

            then {
                balanceShouldBe(BigDecimal("1000.00"), groupBankAccountId)
            }

            `when` {
                clearPublishedEvents()
                editScheduled(
                    occurrenceDate = selectedOccurrence,
                    scope = ScheduledEditScope.THIS_AND_FUTURE,
                    newEntryRequest =
                        NewEntryRequest(
                            type = WalletEntryType.EXPENSE,
                            groupId = null,
                            originId = groupBankAccountId,
                            date = selectedOccurrence,
                            value = BigDecimal("50.00"),
                            confirmed = true,
                            paymentType = PaymentType.RECURRING,
                            periodicity = RecurrenceType.MONTHLY,
                            periodicityQtyLimit = 3,
                        ),
                )
            }

            then {
                balanceShouldBe(BigDecimal("1000.00"), groupBankAccountId)
                publishedWalletEventsShouldBe(ActionEventType.UPDATE, 0)
            }
        }
    }

    @Test
    fun `should deny group edit without SEND_ENTRIES keeping original financial state`() {
        val today = LocalDate.of(2026, 1, 8)
        val groupService = ScenarioGroupService()
        val groupId = groupService.createGroup(name = "Shared Group")
        lateinit var userId: UUID
        lateinit var originId: UUID
        lateinit var targetId: UUID
        var unauthorizedThrown = false

        walletScenario(initialDate = today, groupService = groupService) {
            given {
                userId = user(defaultCurrency = "BRL")
                originId = bankAccount(name = "Origin", balance = BigDecimal("1000.00"), currency = "BRL")
                targetId = bankAccount(name = "Target", balance = BigDecimal("500.00"), currency = "BRL")
            }

            `when` {
                groupService.upsertMemberScope(
                    groupId = groupId,
                    userId = userId,
                    permissions = GroupPermissions.entries.toSet(),
                    associatedItemIds = setOf(originId, targetId),
                )

                transfer(
                    value = BigDecimal("100.00"),
                    date = today,
                    groupId = groupId,
                    originId = originId,
                    targetId = targetId,
                )
            }

            then {
                balanceShouldBe(BigDecimal("900.00"), originId)
                balanceShouldBe(BigDecimal("600.00"), targetId)
            }

            `when` {
                groupService.upsertMemberScope(
                    groupId = groupId,
                    userId = userId,
                    permissions = emptySet(),
                    associatedItemIds = setOf(originId, targetId),
                )

                clearPublishedEvents()
                try {
                    editOneOff(
                        newEntryRequest =
                            NewEntryRequest(
                                type = WalletEntryType.TRANSFER,
                                groupId = groupId,
                                originId = originId,
                                targetId = targetId,
                                date = today,
                                value = null,
                                originValue = BigDecimal("50.00"),
                                confirmed = true,
                                paymentType = PaymentType.UNIQUE,
                            ),
                    )
                } catch (_: UnauthorizedException) {
                    unauthorizedThrown = true
                }
            }

            then {
                assertThat(unauthorizedThrown).isTrue()
                balanceShouldBe(BigDecimal("900.00"), originId)
                balanceShouldBe(BigDecimal("600.00"), targetId)
                publishedWalletEventsShouldBe(ActionEventType.UPDATE, 0)
            }
        }
    }

    @Test
    fun `should apply THIS_AND_FUTURE from selected non-generated occurrence`() {
        val today = LocalDate.of(2026, 1, 8)
        val firstOccurrence = today.plusDays(1)
        val secondOccurrence = firstOccurrence.plusMonths(1)
        lateinit var bankAccountId: UUID

        walletScenario(initialDate = today) {
            given {
                user(defaultCurrency = "BRL")
                bankAccountId = bankAccount(balance = BigDecimal("1000.00"), currency = "BRL")
            }

            `when` {
                createEntry(
                    NewEntryRequest(
                        type = WalletEntryType.EXPENSE,
                        originId = bankAccountId,
                        date = firstOccurrence,
                        value = BigDecimal("100.00"),
                        confirmed = true,
                        paymentType = PaymentType.RECURRING,
                        periodicity = RecurrenceType.MONTHLY,
                        periodicityQtyLimit = 3,
                    ),
                )

                editScheduled(
                    occurrenceDate = secondOccurrence,
                    scope = ScheduledEditScope.THIS_AND_FUTURE,
                    newEntryRequest =
                        NewEntryRequest(
                            type = WalletEntryType.EXPENSE,
                            originId = bankAccountId,
                            date = secondOccurrence,
                            value = BigDecimal("50.00"),
                            confirmed = true,
                            paymentType = PaymentType.RECURRING,
                            periodicity = RecurrenceType.MONTHLY,
                            periodicityQtyLimit = 3,
                        ),
                )
            }

            then {
                balanceShouldBe(BigDecimal("1000.00"), bankAccountId)
            }

            `when` {
                advanceTimeToNextRecurrenceExecution()
                runRecurrence()
            }

            then {
                balanceShouldBe(BigDecimal("900.00"), bankAccountId)
            }

            `when` {
                advanceTimeToNextRecurrenceExecution()
                runRecurrence()
            }

            then {
                balanceShouldBe(BigDecimal("850.00"), bankAccountId)
            }

            `when` {
                advanceTimeToNextRecurrenceExecution()
                runRecurrence()
            }

            then {
                balanceShouldBe(BigDecimal("800.00"), bankAccountId)
            }
        }
    }

    @Test
    fun `should keep unlimited recurring series when applying THIS_AND_FUTURE from first future occurrence`() {
        val today = LocalDate.of(2026, 1, 8)
        val firstOccurrence = today.plusDays(1)
        lateinit var bankAccountId: UUID

        walletScenario(initialDate = today) {
            given {
                user(defaultCurrency = "BRL")
                bankAccountId = bankAccount(balance = BigDecimal("1000.00"), currency = "BRL")
            }

            `when` {
                createEntry(
                    NewEntryRequest(
                        type = WalletEntryType.EXPENSE,
                        originId = bankAccountId,
                        date = firstOccurrence,
                        value = BigDecimal("100.00"),
                        confirmed = true,
                        paymentType = PaymentType.RECURRING,
                        periodicity = RecurrenceType.MONTHLY,
                        periodicityQtyLimit = null,
                    ),
                )

                editScheduled(
                    occurrenceDate = firstOccurrence,
                    scope = ScheduledEditScope.THIS_AND_FUTURE,
                    newEntryRequest =
                        NewEntryRequest(
                            type = WalletEntryType.EXPENSE,
                            originId = bankAccountId,
                            date = firstOccurrence,
                            value = BigDecimal("2.00"),
                            confirmed = true,
                            paymentType = PaymentType.RECURRING,
                            periodicity = RecurrenceType.MONTHLY,
                            periodicityQtyLimit = null,
                        ),
                )
            }

            then {
                recurrenceSegmentsShouldBe(1)
                recurrenceLimitShouldBe(null)
                recurrenceSeriesQtyTotalShouldBe(null)
                scheduledManagerCountShouldBe(ScheduledExecutionFilter.FUTURE, 1)
            }

            `when` {
                advanceTimeToNextRecurrenceExecution()
                runRecurrence()
            }

            then {
                balanceShouldBe(BigDecimal("998.00"), bankAccountId)
            }
        }
    }

    @Test
    fun `should allow changing recurring expense to revenue on THIS_AND_FUTURE edit`() {
        val today = LocalDate.of(2026, 1, 8)
        val firstOccurrence = today.plusDays(1)
        val secondOccurrence = firstOccurrence.plusMonths(1)
        lateinit var bankAccountId: UUID

        walletScenario(initialDate = today) {
            given {
                user(defaultCurrency = "BRL")
                bankAccountId = bankAccount(balance = BigDecimal("1000.00"), currency = "BRL")
            }

            `when` {
                createEntry(
                    NewEntryRequest(
                        type = WalletEntryType.EXPENSE,
                        originId = bankAccountId,
                        date = firstOccurrence,
                        value = BigDecimal("100.00"),
                        confirmed = true,
                        paymentType = PaymentType.RECURRING,
                        periodicity = RecurrenceType.MONTHLY,
                        periodicityQtyLimit = 3,
                    ),
                )

                editScheduled(
                    occurrenceDate = secondOccurrence,
                    scope = ScheduledEditScope.THIS_AND_FUTURE,
                    newEntryRequest =
                        NewEntryRequest(
                            type = WalletEntryType.REVENUE,
                            originId = bankAccountId,
                            date = secondOccurrence,
                            value = BigDecimal("50.00"),
                            confirmed = true,
                            paymentType = PaymentType.RECURRING,
                            periodicity = RecurrenceType.MONTHLY,
                            periodicityQtyLimit = 3,
                        ),
                )
            }

            then {
                balanceShouldBe(BigDecimal("1000.00"), bankAccountId)
            }

            `when` {
                advanceTimeToNextRecurrenceExecution()
                runRecurrence()
            }

            then {
                balanceShouldBe(BigDecimal("900.00"), bankAccountId)
            }

            `when` {
                advanceTimeToNextRecurrenceExecution()
                runRecurrence()
            }

            then {
                balanceShouldBe(BigDecimal("950.00"), bankAccountId)
            }

            `when` {
                advanceTimeToNextRecurrenceExecution()
                runRecurrence()
            }

            then {
                balanceShouldBe(BigDecimal("1000.00"), bankAccountId)
            }
        }
    }

    @Test
    fun `should apply ONLY_THIS for selected non-generated occurrence and keep surrounding occurrences unchanged`() {
        val today = LocalDate.of(2026, 1, 8)
        val firstOccurrence = today.plusDays(1)
        val secondOccurrence = firstOccurrence.plusMonths(1)
        lateinit var bankAccountId: UUID

        walletScenario(initialDate = today) {
            given {
                user(defaultCurrency = "BRL")
                bankAccountId = bankAccount(balance = BigDecimal("1000.00"), currency = "BRL")
            }

            `when` {
                createEntry(
                    NewEntryRequest(
                        type = WalletEntryType.EXPENSE,
                        originId = bankAccountId,
                        date = firstOccurrence,
                        value = BigDecimal("100.00"),
                        confirmed = true,
                        paymentType = PaymentType.RECURRING,
                        periodicity = RecurrenceType.MONTHLY,
                        periodicityQtyLimit = 3,
                    ),
                )

                editScheduled(
                    occurrenceDate = secondOccurrence,
                    scope = ScheduledEditScope.ONLY_THIS,
                    newEntryRequest =
                        NewEntryRequest(
                            type = WalletEntryType.EXPENSE,
                            originId = bankAccountId,
                            date = secondOccurrence,
                            value = BigDecimal("50.00"),
                            confirmed = true,
                            paymentType = PaymentType.RECURRING,
                            periodicity = RecurrenceType.MONTHLY,
                            periodicityQtyLimit = 3,
                        ),
                )
            }

            then {
                scheduledManagerCountShouldBe(ScheduledExecutionFilter.FUTURE, 3)
                scheduledManagerCountShouldBe(ScheduledExecutionFilter.ALREADY_GENERATED, 0)
                scheduledManagerCountShouldBe(ScheduledExecutionFilter.ALL, 3)
            }

            `when` {
                advanceTimeToNextRecurrenceExecution()
                runRecurrence()
            }

            then {
                balanceShouldBe(BigDecimal("900.00"), bankAccountId)
                scheduledManagerCountShouldBe(ScheduledExecutionFilter.FUTURE, 2)
                scheduledManagerCountShouldBe(ScheduledExecutionFilter.ALREADY_GENERATED, 1)
                scheduledManagerCountShouldBe(ScheduledExecutionFilter.ALL, 3)
            }

            `when` {
                advanceTimeToNextRecurrenceExecution()
                runRecurrence()
            }

            then {
                balanceShouldBe(BigDecimal("850.00"), bankAccountId)
            }

            `when` {
                advanceTimeToNextRecurrenceExecution()
                runRecurrence()
            }

            then {
                balanceShouldBe(BigDecimal("750.00"), bankAccountId)
            }
        }
    }

    @Test
    fun `scheduled manager should list scheduled entries in ascending date order`() {
        val today = LocalDate.of(2026, 1, 8)
        val firstOccurrence = today.plusDays(1)
        val secondOccurrence = firstOccurrence.plusMonths(1)
        val thirdOccurrence = secondOccurrence.plusMonths(1)
        lateinit var bankAccountId: UUID

        walletScenario(initialDate = today) {
            given {
                user(defaultCurrency = "BRL")
                bankAccountId = bankAccount(balance = BigDecimal("1000.00"), currency = "BRL")
            }

            `when` {
                createEntry(
                    NewEntryRequest(
                        type = WalletEntryType.EXPENSE,
                        originId = bankAccountId,
                        date = firstOccurrence,
                        value = BigDecimal("100.00"),
                        confirmed = true,
                        paymentType = PaymentType.RECURRING,
                        periodicity = RecurrenceType.MONTHLY,
                        periodicityQtyLimit = 3,
                    ),
                )
                editScheduled(
                    occurrenceDate = secondOccurrence,
                    scope = ScheduledEditScope.ONLY_THIS,
                    newEntryRequest =
                        NewEntryRequest(
                            type = WalletEntryType.EXPENSE,
                            originId = bankAccountId,
                            date = secondOccurrence,
                            value = BigDecimal("50.00"),
                            confirmed = true,
                            paymentType = PaymentType.RECURRING,
                            periodicity = RecurrenceType.MONTHLY,
                            periodicityQtyLimit = 3,
                        ),
                )
            }

            then {
                scheduledManagerDatesShouldBe(
                    filter = ScheduledExecutionFilter.FUTURE,
                    expectedDates = listOf(firstOccurrence, secondOccurrence, thirdOccurrence),
                )
            }

            `when` {
                advanceTime(firstOccurrence)
                runRecurrence()
            }

            then {
                scheduledManagerDatesShouldBe(
                    filter = ScheduledExecutionFilter.ALL,
                    expectedDates = listOf(firstOccurrence, secondOccurrence, thirdOccurrence),
                )
            }
        }
    }

    @Test
    fun `should immediately generate edited scheduled occurrence moved to today`() {
        val today = LocalDate.of(2026, 1, 8)
        val firstOccurrence = today.plusDays(2)
        val secondOccurrence = firstOccurrence.plusMonths(1)
        val thirdOccurrence = secondOccurrence.plusMonths(1)
        lateinit var bankAccountId: UUID

        walletScenario(initialDate = today) {
            given {
                user(defaultCurrency = "BRL")
                bankAccountId = bankAccount(balance = BigDecimal("1000.00"), currency = "BRL")
            }

            `when` {
                createEntry(
                    NewEntryRequest(
                        type = WalletEntryType.EXPENSE,
                        originId = bankAccountId,
                        date = firstOccurrence,
                        value = BigDecimal("100.00"),
                        confirmed = true,
                        paymentType = PaymentType.RECURRING,
                        periodicity = RecurrenceType.MONTHLY,
                        periodicityQtyLimit = 3,
                    ),
                )
                clearPublishedEvents()
                editScheduled(
                    occurrenceDate = firstOccurrence,
                    scope = ScheduledEditScope.ONLY_THIS,
                    newEntryRequest =
                        NewEntryRequest(
                            type = WalletEntryType.EXPENSE,
                            originId = bankAccountId,
                            date = today,
                            value = BigDecimal("40.00"),
                            confirmed = true,
                            paymentType = PaymentType.RECURRING,
                            periodicity = RecurrenceType.MONTHLY,
                            periodicityQtyLimit = 3,
                        ),
                )
            }

            then {
                balanceShouldBe(BigDecimal("960.00"), bankAccountId)
                scheduledManagerCountShouldBe(ScheduledExecutionFilter.FUTURE, 1)
                scheduledManagerCountShouldBe(ScheduledExecutionFilter.ALREADY_GENERATED, 1)
                scheduledManagerDatesShouldBe(
                    filter = ScheduledExecutionFilter.ALL,
                    expectedDates = listOf(today, secondOccurrence),
                )
                lastPublishedWalletEventShouldConvert(ActionEventType.UPDATE)
            }
        }
    }

    @Test
    fun `should reschedule only selected scheduled occurrence when edited to another future date`() {
        val today = LocalDate.of(2026, 1, 8)
        val firstOccurrence = today.plusDays(2)
        val movedOccurrence = today.plusDays(5)
        val secondOccurrence = firstOccurrence.plusMonths(1)
        lateinit var bankAccountId: UUID

        walletScenario(initialDate = today) {
            given {
                user(defaultCurrency = "BRL")
                bankAccountId = bankAccount(balance = BigDecimal("1000.00"), currency = "BRL")
            }

            `when` {
                createEntry(
                    NewEntryRequest(
                        type = WalletEntryType.EXPENSE,
                        originId = bankAccountId,
                        date = firstOccurrence,
                        value = BigDecimal("100.00"),
                        confirmed = true,
                        paymentType = PaymentType.RECURRING,
                        periodicity = RecurrenceType.MONTHLY,
                        periodicityQtyLimit = 2,
                    ),
                )
                clearPublishedEvents()
                editScheduled(
                    occurrenceDate = firstOccurrence,
                    scope = ScheduledEditScope.ONLY_THIS,
                    newEntryRequest =
                        NewEntryRequest(
                            type = WalletEntryType.EXPENSE,
                            originId = bankAccountId,
                            date = movedOccurrence,
                            value = BigDecimal("100.00"),
                            confirmed = true,
                            paymentType = PaymentType.RECURRING,
                            periodicity = RecurrenceType.MONTHLY,
                            periodicityQtyLimit = 2,
                        ),
                )
            }

            then {
                balanceShouldBe(BigDecimal("1000.00"), bankAccountId)
                scheduledManagerDatesShouldBe(
                    filter = ScheduledExecutionFilter.FUTURE,
                    expectedDates = listOf(movedOccurrence, secondOccurrence),
                )
            }
        }
    }

    @Test
    fun `should immediately generate installment occurrence and recalculate future installments on THIS_AND_FUTURE edit to today`() {
        val today = LocalDate.of(2026, 1, 8)
        val firstOccurrence = today.plusDays(3)
        val secondOccurrence = today.plusMonths(1)
        val thirdOccurrence = today.plusMonths(2)
        lateinit var bankAccountId: UUID

        walletScenario(initialDate = today) {
            given {
                user(defaultCurrency = "BRL")
                bankAccountId = bankAccount(balance = BigDecimal("1000.00"), currency = "BRL")
            }

            `when` {
                createEntry(
                    NewEntryRequest(
                        type = WalletEntryType.EXPENSE,
                        originId = bankAccountId,
                        date = firstOccurrence,
                        value = BigDecimal("100.00"),
                        confirmed = true,
                        paymentType = PaymentType.INSTALLMENTS,
                        installments = 3,
                        periodicity = RecurrenceType.MONTHLY,
                    ),
                )
                clearPublishedEvents()
                editScheduled(
                    occurrenceDate = firstOccurrence,
                    scope = ScheduledEditScope.THIS_AND_FUTURE,
                    newEntryRequest =
                        NewEntryRequest(
                            type = WalletEntryType.EXPENSE,
                            originId = bankAccountId,
                            date = today,
                            value = BigDecimal("100.00"),
                            confirmed = true,
                            paymentType = PaymentType.INSTALLMENTS,
                            installments = 3,
                            periodicity = RecurrenceType.MONTHLY,
                        ),
                )
            }

            then {
                balanceShouldBe(BigDecimal("900.00"), bankAccountId)
                scheduledManagerCountShouldBe(ScheduledExecutionFilter.ALREADY_GENERATED, 1)
                scheduledManagerDatesShouldBe(
                    filter = ScheduledExecutionFilter.ALL,
                    expectedDates = listOf(today, secondOccurrence),
                )
                scheduledInstallmentsShouldBe(
                    filter = ScheduledExecutionFilter.FUTURE,
                    expectedInstallments = listOf(2),
                    expectedTotal = 3,
                )
                lastPublishedWalletEventShouldConvert(ActionEventType.UPDATE)
            }

            `when` {
                advanceTime(secondOccurrence)
                runRecurrence()
            }

            then {
                balanceShouldBe(BigDecimal("800.00"), bankAccountId)
                scheduledManagerDatesShouldBe(
                    filter = ScheduledExecutionFilter.ALL,
                    expectedDates = listOf(today, secondOccurrence, thirdOccurrence),
                )
                scheduledInstallmentsShouldBe(
                    filter = ScheduledExecutionFilter.FUTURE,
                    expectedInstallments = listOf(3),
                    expectedTotal = 3,
                )
            }
        }
    }

    @Test
    fun `should immediately generate recurring occurrence and recalculate future recurrences on THIS_AND_FUTURE edit to today`() {
        val today = LocalDate.of(2026, 1, 8)
        val firstOccurrence = today.plusDays(4)
        val secondOccurrence = today.plusWeeks(1)
        val thirdOccurrence = today.plusWeeks(2)
        lateinit var bankAccountId: UUID

        walletScenario(initialDate = today) {
            given {
                user(defaultCurrency = "BRL")
                bankAccountId = bankAccount(balance = BigDecimal("1000.00"), currency = "BRL")
            }

            `when` {
                createEntry(
                    NewEntryRequest(
                        type = WalletEntryType.EXPENSE,
                        originId = bankAccountId,
                        date = firstOccurrence,
                        value = BigDecimal("40.00"),
                        confirmed = true,
                        paymentType = PaymentType.RECURRING,
                        periodicity = RecurrenceType.WEEKLY,
                        periodicityQtyLimit = 3,
                    ),
                )
                clearPublishedEvents()
                editScheduled(
                    occurrenceDate = firstOccurrence,
                    scope = ScheduledEditScope.THIS_AND_FUTURE,
                    newEntryRequest =
                        NewEntryRequest(
                            type = WalletEntryType.EXPENSE,
                            originId = bankAccountId,
                            date = today,
                            value = BigDecimal("40.00"),
                            confirmed = true,
                            paymentType = PaymentType.RECURRING,
                            periodicity = RecurrenceType.WEEKLY,
                            periodicityQtyLimit = 3,
                        ),
                )
            }

            then {
                balanceShouldBe(BigDecimal("960.00"), bankAccountId)
                scheduledManagerCountShouldBe(ScheduledExecutionFilter.ALREADY_GENERATED, 1)
                scheduledManagerCountShouldBe(ScheduledExecutionFilter.FUTURE, 1)
                scheduledManagerDatesShouldBe(
                    filter = ScheduledExecutionFilter.ALL,
                    expectedDates = listOf(today, secondOccurrence),
                )
                lastPublishedWalletEventShouldConvert(ActionEventType.UPDATE)
            }

            `when` {
                advanceTime(secondOccurrence)
                runRecurrence()
            }

            then {
                balanceShouldBe(BigDecimal("920.00"), bankAccountId)
                scheduledManagerCountShouldBe(ScheduledExecutionFilter.FUTURE, 1)
                scheduledManagerDatesShouldBe(
                    filter = ScheduledExecutionFilter.ALL,
                    expectedDates = listOf(today, secondOccurrence, thirdOccurrence),
                )
            }
        }
    }

    @Test
    fun `should backfill retroactive installment purchase up to today and keep later installments scheduled`() {
        val today = LocalDate.of(2026, 4, 14)
        val firstOccurrence = LocalDate.of(2026, 2, 14)
        val secondOccurrence = LocalDate.of(2026, 3, 14)
        val thirdOccurrence = LocalDate.of(2026, 4, 14)
        val fourthOccurrence = LocalDate.of(2026, 5, 14)
        val fifthOccurrence = LocalDate.of(2026, 6, 14)
        lateinit var bankAccountId: UUID

        walletScenario(initialDate = today) {
            given {
                user(defaultCurrency = "BRL")
                bankAccountId = bankAccount(balance = BigDecimal("1000.00"), currency = "BRL")
            }

            `when` {
                createEntry(
                    NewEntryRequest(
                        type = WalletEntryType.EXPENSE,
                        originId = bankAccountId,
                        date = firstOccurrence,
                        value = BigDecimal("100.00"),
                        confirmed = true,
                        paymentType = PaymentType.INSTALLMENTS,
                        installments = 5,
                        periodicity = RecurrenceType.MONTHLY,
                    ),
                )
            }

            then {
                balanceShouldBe(BigDecimal("700.00"), bankAccountId)
                recurrenceExecutionCountShouldBe(3)
                recurrenceLimitShouldBe(5)
                recurrenceSeriesQtyTotalShouldBe(5)
                recurrenceNextExecutionShouldBe(fourthOccurrence)
                scheduledManagerCountShouldBe(ScheduledExecutionFilter.ALREADY_GENERATED, 3)
                scheduledManagerCountShouldBe(ScheduledExecutionFilter.FUTURE, 1)
                scheduledManagerDatesShouldBe(
                    filter = ScheduledExecutionFilter.ALREADY_GENERATED,
                    expectedDates = listOf(firstOccurrence, secondOccurrence, thirdOccurrence),
                )
                scheduledManagerDatesShouldBe(
                    filter = ScheduledExecutionFilter.ALL,
                    expectedDates = listOf(firstOccurrence, secondOccurrence, thirdOccurrence, fourthOccurrence),
                )
                scheduledInstallmentsShouldBe(
                    filter = ScheduledExecutionFilter.FUTURE,
                    expectedInstallments = listOf(4),
                    expectedTotal = 5,
                )
            }

            `when` {
                advanceTime(fourthOccurrence)
                runRecurrence()
            }

            then {
                balanceShouldBe(BigDecimal("600.00"), bankAccountId)
                recurrenceExecutionCountShouldBe(4)
                recurrenceNextExecutionShouldBe(fifthOccurrence)
                scheduledManagerDatesShouldBe(
                    filter = ScheduledExecutionFilter.ALL,
                    expectedDates = listOf(firstOccurrence, secondOccurrence, thirdOccurrence, fourthOccurrence, fifthOccurrence),
                )
                scheduledInstallmentsShouldBe(
                    filter = ScheduledExecutionFilter.FUTURE,
                    expectedInstallments = listOf(5),
                    expectedTotal = 5,
                )
            }
        }
    }

    @Test
    fun `should backfill retroactive unlimited recurring expense up to today and keep next recurrence scheduled`() {
        val today = LocalDate.of(2026, 4, 14)
        val firstOccurrence = LocalDate.of(2026, 2, 14)
        val secondOccurrence = LocalDate.of(2026, 3, 14)
        val thirdOccurrence = LocalDate.of(2026, 4, 14)
        val fourthOccurrence = LocalDate.of(2026, 5, 14)
        val fifthOccurrence = LocalDate.of(2026, 6, 14)
        lateinit var bankAccountId: UUID

        walletScenario(initialDate = today) {
            given {
                user(defaultCurrency = "BRL")
                bankAccountId = bankAccount(balance = BigDecimal("1000.00"), currency = "BRL")
            }

            `when` {
                createEntry(
                    NewEntryRequest(
                        type = WalletEntryType.EXPENSE,
                        originId = bankAccountId,
                        date = firstOccurrence,
                        value = BigDecimal("40.00"),
                        confirmed = true,
                        paymentType = PaymentType.RECURRING,
                        periodicity = RecurrenceType.MONTHLY,
                        periodicityQtyLimit = null,
                    ),
                )
            }

            then {
                balanceShouldBe(BigDecimal("880.00"), bankAccountId)
                recurrenceExecutionCountShouldBe(3)
                recurrenceLimitShouldBe(null)
                recurrenceSeriesQtyTotalShouldBe(3)
                recurrenceNextExecutionShouldBe(fourthOccurrence)
                scheduledManagerCountShouldBe(ScheduledExecutionFilter.ALREADY_GENERATED, 3)
                scheduledManagerCountShouldBe(ScheduledExecutionFilter.FUTURE, 1)
                scheduledManagerDatesShouldBe(
                    filter = ScheduledExecutionFilter.ALREADY_GENERATED,
                    expectedDates = listOf(firstOccurrence, secondOccurrence, thirdOccurrence),
                )
                scheduledManagerDatesShouldBe(
                    filter = ScheduledExecutionFilter.ALL,
                    expectedDates = listOf(firstOccurrence, secondOccurrence, thirdOccurrence, fourthOccurrence),
                )
            }

            `when` {
                advanceTime(fourthOccurrence)
                runRecurrence()
            }

            then {
                balanceShouldBe(BigDecimal("840.00"), bankAccountId)
                recurrenceExecutionCountShouldBe(4)
                recurrenceLimitShouldBe(null)
                recurrenceSeriesQtyTotalShouldBe(4)
                recurrenceNextExecutionShouldBe(fifthOccurrence)
                scheduledManagerDatesShouldBe(
                    filter = ScheduledExecutionFilter.ALL,
                    expectedDates = listOf(firstOccurrence, secondOccurrence, thirdOccurrence, fourthOccurrence, fifthOccurrence),
                )
            }
        }
    }

    @Test
    fun `should allow moving next scheduled recurring execution to a date after previous generated execution`() {
        val today = LocalDate.of(2026, 2, 10)
        val firstOccurrence = today
        val secondOccurrence = today.plusMonths(1)
        val thirdOccurrence = today.plusMonths(2)
        val allowedPastDate = firstOccurrence.plusDays(1)
        val inspectionDate = secondOccurrence.plusDays(10)
        lateinit var bankAccountId: UUID

        walletScenario(initialDate = today) {
            given {
                user(defaultCurrency = "BRL")
                bankAccountId = bankAccount(balance = BigDecimal("1000.00"), currency = "BRL")
            }

            `when` {
                createEntry(
                    NewEntryRequest(
                        type = WalletEntryType.EXPENSE,
                        originId = bankAccountId,
                        date = firstOccurrence,
                        value = BigDecimal("50.00"),
                        confirmed = true,
                        paymentType = PaymentType.RECURRING,
                        periodicity = RecurrenceType.MONTHLY,
                        periodicityQtyLimit = 3,
                    ),
                )
                advanceTime(inspectionDate)
                editScheduled(
                    occurrenceDate = secondOccurrence,
                    scope = ScheduledEditScope.ONLY_THIS,
                    newEntryRequest =
                        NewEntryRequest(
                            type = WalletEntryType.EXPENSE,
                            originId = bankAccountId,
                            date = allowedPastDate,
                            value = BigDecimal("50.00"),
                            confirmed = true,
                            paymentType = PaymentType.RECURRING,
                            periodicity = RecurrenceType.MONTHLY,
                            periodicityQtyLimit = 3,
                        ),
                )
            }

            then {
                balanceShouldBe(BigDecimal("900.00"), bankAccountId)
                recurrenceExecutionCountShouldBe(1)
                scheduledManagerCountShouldBe(ScheduledExecutionFilter.ALREADY_GENERATED, 2)
                scheduledManagerCountShouldBe(ScheduledExecutionFilter.FUTURE, 1)
                scheduledManagerDatesShouldBe(
                    filter = ScheduledExecutionFilter.ALL,
                    expectedDates = listOf(firstOccurrence, allowedPastDate, thirdOccurrence),
                )
            }
        }
    }

    @Test
    fun `should reject moving next scheduled recurring execution to the same date as previous generated execution`() {
        val today = LocalDate.of(2026, 1, 8)
        val firstOccurrence = today
        val secondOccurrence = today.plusMonths(1)
        val thirdOccurrence = today.plusMonths(2)
        val forbiddenDate = firstOccurrence
        lateinit var bankAccountId: UUID

        val error =
            assertThrows<InvalidPastScheduledExecutionEditException> {
                walletScenario(initialDate = today) {
                    given {
                        user(defaultCurrency = "BRL")
                        bankAccountId = bankAccount(balance = BigDecimal("1000.00"), currency = "BRL")
                    }

                    `when` {
                        createEntry(
                            NewEntryRequest(
                                type = WalletEntryType.EXPENSE,
                                originId = bankAccountId,
                                date = firstOccurrence,
                                value = BigDecimal("50.00"),
                                confirmed = true,
                                paymentType = PaymentType.RECURRING,
                                periodicity = RecurrenceType.MONTHLY,
                                periodicityQtyLimit = null,
                            ),
                        )
                        advanceTime(secondOccurrence)
                        runRecurrence()
                        editScheduled(
                            occurrenceDate = thirdOccurrence,
                            scope = ScheduledEditScope.ONLY_THIS,
                            newEntryRequest =
                                NewEntryRequest(
                                    type = WalletEntryType.EXPENSE,
                                    originId = bankAccountId,
                                    date = forbiddenDate,
                                    value = BigDecimal("50.00"),
                                    confirmed = true,
                                    paymentType = PaymentType.RECURRING,
                                    periodicity = RecurrenceType.MONTHLY,
                                    periodicityQtyLimit = null,
                                ),
                        )
                    }
                }
            }

        assertThat(error.message)
            .isEqualTo("The next scheduled execution cannot be moved to the same day as or before the previous generated execution.")

        walletScenario(initialDate = today) {
            given {
                user(defaultCurrency = "BRL")
                bankAccountId = bankAccount(balance = BigDecimal("1000.00"), currency = "BRL")
            }

            `when` {
                createEntry(
                    NewEntryRequest(
                        type = WalletEntryType.EXPENSE,
                        originId = bankAccountId,
                        date = firstOccurrence,
                        value = BigDecimal("50.00"),
                        confirmed = true,
                        paymentType = PaymentType.RECURRING,
                        periodicity = RecurrenceType.MONTHLY,
                        periodicityQtyLimit = null,
                    ),
                )
                advanceTime(secondOccurrence)
                runRecurrence()
            }

            then {
                balanceShouldBe(BigDecimal("900.00"), bankAccountId)
                recurrenceExecutionCountShouldBe(2)
                recurrenceNextExecutionShouldBe(thirdOccurrence)
                scheduledManagerDatesShouldBe(
                    filter = ScheduledExecutionFilter.ALL,
                    expectedDates = listOf(firstOccurrence, secondOccurrence, thirdOccurrence),
                )
            }
        }
    }

    @Test
    fun `should allow moving next scheduled installment execution to a date after previous generated execution`() {
        val today = LocalDate.of(2026, 2, 10)
        val firstOccurrence = today
        val secondOccurrence = today.plusMonths(1)
        val thirdOccurrence = today.plusMonths(2)
        val allowedPastDate = firstOccurrence.plusDays(1)
        val inspectionDate = secondOccurrence.plusDays(10)
        lateinit var bankAccountId: UUID

        walletScenario(initialDate = today) {
            given {
                user(defaultCurrency = "BRL")
                bankAccountId = bankAccount(balance = BigDecimal("1000.00"), currency = "BRL")
            }

            `when` {
                createEntry(
                    NewEntryRequest(
                        type = WalletEntryType.EXPENSE,
                        originId = bankAccountId,
                        date = firstOccurrence,
                        value = BigDecimal("100.00"),
                        confirmed = true,
                        paymentType = PaymentType.INSTALLMENTS,
                        installments = 3,
                        periodicity = RecurrenceType.MONTHLY,
                    ),
                )
                advanceTime(inspectionDate)
                editScheduled(
                    occurrenceDate = secondOccurrence,
                    scope = ScheduledEditScope.ONLY_THIS,
                    newEntryRequest =
                        NewEntryRequest(
                            type = WalletEntryType.EXPENSE,
                            originId = bankAccountId,
                            date = allowedPastDate,
                            value = BigDecimal("100.00"),
                            confirmed = true,
                            paymentType = PaymentType.INSTALLMENTS,
                            installments = 3,
                            periodicity = RecurrenceType.MONTHLY,
                        ),
                )
            }

            then {
                balanceShouldBe(BigDecimal("800.00"), bankAccountId)
                recurrenceExecutionCountShouldBe(1)
                scheduledManagerCountShouldBe(ScheduledExecutionFilter.ALREADY_GENERATED, 2)
                scheduledManagerCountShouldBe(ScheduledExecutionFilter.FUTURE, 1)
                scheduledManagerDatesShouldBe(
                    filter = ScheduledExecutionFilter.ALL,
                    expectedDates = listOf(firstOccurrence, allowedPastDate, thirdOccurrence),
                )
                scheduledInstallmentsShouldBe(
                    filter = ScheduledExecutionFilter.FUTURE,
                    expectedInstallments = listOf(3),
                    expectedTotal = 3,
                )
            }
        }
    }

    @Test
    fun `should reject moving next scheduled installment execution to the same date as previous generated execution`() {
        val today = LocalDate.of(2026, 1, 8)
        val firstOccurrence = today
        val secondOccurrence = today.plusMonths(1)
        val thirdOccurrence = today.plusMonths(2)
        val forbiddenDate = firstOccurrence
        lateinit var bankAccountId: UUID

        val error =
            assertThrows<InvalidPastScheduledExecutionEditException> {
                walletScenario(initialDate = today) {
                    given {
                        user(defaultCurrency = "BRL")
                        bankAccountId = bankAccount(balance = BigDecimal("1000.00"), currency = "BRL")
                    }

                    `when` {
                        createEntry(
                            NewEntryRequest(
                                type = WalletEntryType.EXPENSE,
                                originId = bankAccountId,
                                date = firstOccurrence,
                                value = BigDecimal("100.00"),
                                confirmed = true,
                                paymentType = PaymentType.INSTALLMENTS,
                                installments = 5,
                                periodicity = RecurrenceType.MONTHLY,
                            ),
                        )
                        advanceTime(secondOccurrence)
                        runRecurrence()
                        editScheduled(
                            occurrenceDate = thirdOccurrence,
                            scope = ScheduledEditScope.ONLY_THIS,
                            newEntryRequest =
                                NewEntryRequest(
                                    type = WalletEntryType.EXPENSE,
                                    originId = bankAccountId,
                                    date = forbiddenDate,
                                    value = BigDecimal("100.00"),
                                    confirmed = true,
                                    paymentType = PaymentType.INSTALLMENTS,
                                    installments = 5,
                                    periodicity = RecurrenceType.MONTHLY,
                                ),
                        )
                    }
                }
            }

        assertThat(error.message)
            .isEqualTo("The next scheduled execution cannot be moved to the same day as or before the previous generated execution.")

        walletScenario(initialDate = today) {
            given {
                user(defaultCurrency = "BRL")
                bankAccountId = bankAccount(balance = BigDecimal("1000.00"), currency = "BRL")
            }

            `when` {
                createEntry(
                    NewEntryRequest(
                        type = WalletEntryType.EXPENSE,
                        originId = bankAccountId,
                        date = firstOccurrence,
                        value = BigDecimal("100.00"),
                        confirmed = true,
                        paymentType = PaymentType.INSTALLMENTS,
                        installments = 5,
                        periodicity = RecurrenceType.MONTHLY,
                    ),
                )
                advanceTime(secondOccurrence)
                runRecurrence()
            }

            then {
                balanceShouldBe(BigDecimal("800.00"), bankAccountId)
                recurrenceExecutionCountShouldBe(2)
                recurrenceNextExecutionShouldBe(thirdOccurrence)
                scheduledManagerDatesShouldBe(
                    filter = ScheduledExecutionFilter.ALL,
                    expectedDates = listOf(firstOccurrence, secondOccurrence, thirdOccurrence),
                )
                scheduledInstallmentsShouldBe(
                    filter = ScheduledExecutionFilter.FUTURE,
                    expectedInstallments = listOf(3),
                    expectedTotal = 5,
                )
            }
        }
    }

    @Test
    fun `should keep global installment numbering on THIS_AND_FUTURE split`() {
        val today = LocalDate.of(2026, 1, 8)
        val secondOccurrence = today.plusMonths(1)
        lateinit var bankAccountId: UUID

        walletScenario(initialDate = today) {
            given {
                user(defaultCurrency = "BRL")
                bankAccountId = bankAccount(balance = BigDecimal("1000.00"), currency = "BRL")
            }

            `when` {
                createEntry(
                    NewEntryRequest(
                        type = WalletEntryType.EXPENSE,
                        originId = bankAccountId,
                        date = today,
                        value = BigDecimal("100.00"),
                        confirmed = true,
                        paymentType = PaymentType.INSTALLMENTS,
                        installments = 3,
                        periodicity = RecurrenceType.MONTHLY,
                    ),
                )

                editScheduled(
                    occurrenceDate = secondOccurrence,
                    scope = ScheduledEditScope.THIS_AND_FUTURE,
                    newEntryRequest =
                        NewEntryRequest(
                            type = WalletEntryType.EXPENSE,
                            originId = bankAccountId,
                            date = secondOccurrence,
                            value = BigDecimal("80.00"),
                            confirmed = true,
                            paymentType = PaymentType.INSTALLMENTS,
                            installments = 3,
                            periodicity = RecurrenceType.MONTHLY,
                        ),
                )
            }

            then {
                scheduledInstallmentsShouldBe(
                    filter = ScheduledExecutionFilter.FUTURE,
                    expectedInstallments = listOf(2),
                    expectedTotal = 3,
                )
            }

            `when` {
                advanceTimeToNextRecurrenceExecution()
                runRecurrence()
            }

            then {
                scheduledInstallmentsShouldBe(
                    filter = ScheduledExecutionFilter.FUTURE,
                    expectedInstallments = listOf(3),
                    expectedTotal = 3,
                )
            }
        }
    }

    @Test
    fun `should keep global installment numbering on ONLY_THIS split`() {
        val today = LocalDate.of(2026, 1, 8)
        val secondOccurrence = today.plusMonths(1)
        lateinit var bankAccountId: UUID

        walletScenario(initialDate = today) {
            given {
                user(defaultCurrency = "BRL")
                bankAccountId = bankAccount(balance = BigDecimal("1000.00"), currency = "BRL")
            }

            `when` {
                createEntry(
                    NewEntryRequest(
                        type = WalletEntryType.EXPENSE,
                        originId = bankAccountId,
                        date = today,
                        value = BigDecimal("100.00"),
                        confirmed = true,
                        paymentType = PaymentType.INSTALLMENTS,
                        installments = 3,
                        periodicity = RecurrenceType.MONTHLY,
                    ),
                )

                editScheduled(
                    occurrenceDate = secondOccurrence,
                    scope = ScheduledEditScope.ONLY_THIS,
                    newEntryRequest =
                        NewEntryRequest(
                            type = WalletEntryType.EXPENSE,
                            originId = bankAccountId,
                            date = secondOccurrence,
                            value = BigDecimal("80.00"),
                            confirmed = true,
                            paymentType = PaymentType.INSTALLMENTS,
                            installments = 3,
                            periodicity = RecurrenceType.MONTHLY,
                        ),
                )
            }

            then {
                scheduledInstallmentsShouldBe(
                    filter = ScheduledExecutionFilter.FUTURE,
                    expectedInstallments = listOf(2, 3),
                    expectedTotal = 3,
                )
            }
        }
    }

    @Test
    fun `should update installment total when editing THIS_AND_FUTURE from scheduler manager`() {
        val today = LocalDate.of(2026, 1, 8)
        val firstOccurrence = today.plusDays(1)
        lateinit var bankAccountId: UUID

        walletScenario(initialDate = today) {
            given {
                user(defaultCurrency = "BRL")
                bankAccountId = bankAccount(balance = BigDecimal("1000.00"), currency = "BRL")
            }

            `when` {
                createEntry(
                    NewEntryRequest(
                        type = WalletEntryType.EXPENSE,
                        originId = bankAccountId,
                        date = firstOccurrence,
                        value = BigDecimal("100.00"),
                        confirmed = true,
                        paymentType = PaymentType.INSTALLMENTS,
                        installments = 5,
                        periodicity = RecurrenceType.MONTHLY,
                    ),
                )

                editScheduled(
                    occurrenceDate = firstOccurrence,
                    scope = ScheduledEditScope.THIS_AND_FUTURE,
                    newEntryRequest =
                        NewEntryRequest(
                            type = WalletEntryType.EXPENSE,
                            originId = bankAccountId,
                            date = firstOccurrence,
                            value = BigDecimal("100.00"),
                            confirmed = true,
                            paymentType = PaymentType.INSTALLMENTS,
                            installments = 3,
                            periodicity = RecurrenceType.MONTHLY,
                        ),
                )
            }

            then {
                recurrenceLimitShouldBe(3)
                recurrenceSeriesQtyTotalShouldBe(3)
                recurrenceSegmentsShouldBe(1)
                scheduledInstallmentsShouldBe(
                    filter = ScheduledExecutionFilter.FUTURE,
                    expectedInstallments = listOf(1),
                    expectedTotal = 3,
                )
            }
        }
    }

    @Test
    fun `should reunify split series when editing earlier occurrence with THIS_AND_FUTURE`() {
        val today = LocalDate.of(2026, 1, 8)
        val firstOccurrence = today.plusDays(1)
        val secondOccurrence = firstOccurrence.plusMonths(1)
        lateinit var bankAccountId: UUID

        walletScenario(initialDate = today) {
            given {
                user(defaultCurrency = "BRL")
                bankAccountId = bankAccount(balance = BigDecimal("1000.00"), currency = "BRL")
            }

            `when` {
                createEntry(
                    NewEntryRequest(
                        type = WalletEntryType.EXPENSE,
                        originId = bankAccountId,
                        date = firstOccurrence,
                        value = BigDecimal("15.00"),
                        confirmed = true,
                        paymentType = PaymentType.INSTALLMENTS,
                        installments = 5,
                        periodicity = RecurrenceType.MONTHLY,
                    ),
                )

                editScheduled(
                    occurrenceDate = secondOccurrence,
                    scope = ScheduledEditScope.THIS_AND_FUTURE,
                    newEntryRequest =
                        NewEntryRequest(
                            type = WalletEntryType.EXPENSE,
                            originId = bankAccountId,
                            date = secondOccurrence,
                            value = BigDecimal("20.00"),
                            confirmed = true,
                            paymentType = PaymentType.INSTALLMENTS,
                            installments = 5,
                            periodicity = RecurrenceType.MONTHLY,
                        ),
                )
            }

            then {
                recurrenceSegmentsShouldBe(2)
            }

            `when` {
                editScheduled(
                    occurrenceDate = firstOccurrence,
                    scope = ScheduledEditScope.THIS_AND_FUTURE,
                    newEntryRequest =
                        NewEntryRequest(
                            type = WalletEntryType.EXPENSE,
                            originId = bankAccountId,
                            date = firstOccurrence,
                            value = BigDecimal("23.00"),
                            confirmed = true,
                            paymentType = PaymentType.INSTALLMENTS,
                            installments = 5,
                            periodicity = RecurrenceType.MONTHLY,
                        ),
                )
            }

            then {
                recurrenceSegmentsShouldBe(1)
            }

            `when` {
                repeat(5) {
                    advanceTimeToNextRecurrenceExecution()
                    runRecurrence()
                }
            }

            then {
                balanceShouldBe(BigDecimal("885.00"), bankAccountId)
            }
        }
    }

    @Test
    fun `should rewrite generated occurrences inside THIS_AND_FUTURE affected range`() {
        val today = LocalDate.of(2026, 1, 8)
        val firstOccurrence = today
        lateinit var bankAccountId: UUID

        walletScenario(initialDate = today) {
            given {
                user(defaultCurrency = "BRL")
                bankAccountId = bankAccount(balance = BigDecimal("1000.00"), currency = "BRL")
            }

            `when` {
                createEntry(
                    NewEntryRequest(
                        type = WalletEntryType.EXPENSE,
                        originId = bankAccountId,
                        date = firstOccurrence,
                        value = BigDecimal("15.00"),
                        confirmed = true,
                        paymentType = PaymentType.INSTALLMENTS,
                        installments = 5,
                        periodicity = RecurrenceType.MONTHLY,
                    ),
                )

                advanceTimeToNextRecurrenceExecution()
                runRecurrence()
            }

            then {
                balanceShouldBe(BigDecimal("970.00"), bankAccountId)
            }

            `when` {
                editScheduled(
                    occurrenceDate = firstOccurrence,
                    scope = ScheduledEditScope.THIS_AND_FUTURE,
                    newEntryRequest =
                        NewEntryRequest(
                            type = WalletEntryType.EXPENSE,
                            originId = bankAccountId,
                            date = firstOccurrence,
                            value = BigDecimal("23.00"),
                            confirmed = true,
                            paymentType = PaymentType.INSTALLMENTS,
                            installments = 5,
                            periodicity = RecurrenceType.MONTHLY,
                        ),
                )
            }

            then {
                recurrenceSegmentsShouldBe(1)
                balanceShouldBe(BigDecimal("954.00"), bankAccountId)
            }

            `when` {
                repeat(3) {
                    advanceTimeToNextRecurrenceExecution()
                    runRecurrence()
                }
            }

            then {
                balanceShouldBe(BigDecimal("885.00"), bankAccountId)
            }
        }
    }

    @Test
    fun `should keep recurrence tags null after THIS_AND_FUTURE unification`() {
        val today = LocalDate.of(2026, 1, 8)
        val firstOccurrence = today.plusDays(1)
        val secondOccurrence = firstOccurrence.plusMonths(1)
        lateinit var bankAccountId: UUID

        walletScenario(initialDate = today) {
            given {
                user(defaultCurrency = "BRL")
                bankAccountId = bankAccount(balance = BigDecimal("1000.00"), currency = "BRL")
            }

            `when` {
                createEntry(
                    NewEntryRequest(
                        type = WalletEntryType.EXPENSE,
                        originId = bankAccountId,
                        date = firstOccurrence,
                        value = BigDecimal("100.00"),
                        confirmed = true,
                        paymentType = PaymentType.RECURRING,
                        periodicity = RecurrenceType.MONTHLY,
                        periodicityQtyLimit = 3,
                        tags = null,
                    ),
                )

                editScheduled(
                    occurrenceDate = secondOccurrence,
                    scope = ScheduledEditScope.ONLY_THIS,
                    newEntryRequest =
                        NewEntryRequest(
                            type = WalletEntryType.EXPENSE,
                            originId = bankAccountId,
                            date = secondOccurrence,
                            value = BigDecimal("80.00"),
                            confirmed = true,
                            paymentType = PaymentType.RECURRING,
                            periodicity = RecurrenceType.MONTHLY,
                            periodicityQtyLimit = 3,
                            tags = null,
                        ),
                )
            }

            then {
                recurrenceSegmentsShouldBe(3)
                recurrenceSeriesTagsShouldBeNull()
            }

            `when` {
                editScheduled(
                    occurrenceDate = firstOccurrence,
                    scope = ScheduledEditScope.THIS_AND_FUTURE,
                    newEntryRequest =
                        NewEntryRequest(
                            type = WalletEntryType.EXPENSE,
                            originId = bankAccountId,
                            date = firstOccurrence,
                            value = BigDecimal("70.00"),
                            confirmed = true,
                            paymentType = PaymentType.RECURRING,
                            periodicity = RecurrenceType.MONTHLY,
                            periodicityQtyLimit = 3,
                            tags = null,
                        ),
                )
            }

            then {
                recurrenceSegmentsShouldBe(1)
                recurrenceSeriesTagsShouldBeNull()
            }
        }
    }

    @Test
    fun `should keep recurrence tags null after ONLY_THIS split`() {
        val today = LocalDate.of(2026, 1, 8)
        val firstOccurrence = today.plusDays(1)
        val secondOccurrence = firstOccurrence.plusMonths(1)
        lateinit var bankAccountId: UUID

        walletScenario(initialDate = today) {
            given {
                user(defaultCurrency = "BRL")
                bankAccountId = bankAccount(balance = BigDecimal("1000.00"), currency = "BRL")
            }

            `when` {
                createEntry(
                    NewEntryRequest(
                        type = WalletEntryType.EXPENSE,
                        originId = bankAccountId,
                        date = firstOccurrence,
                        value = BigDecimal("100.00"),
                        confirmed = true,
                        paymentType = PaymentType.RECURRING,
                        periodicity = RecurrenceType.MONTHLY,
                        periodicityQtyLimit = 3,
                        tags = null,
                    ),
                )

                editScheduled(
                    occurrenceDate = secondOccurrence,
                    scope = ScheduledEditScope.ONLY_THIS,
                    newEntryRequest =
                        NewEntryRequest(
                            type = WalletEntryType.EXPENSE,
                            originId = bankAccountId,
                            date = secondOccurrence,
                            value = BigDecimal("80.00"),
                            confirmed = true,
                            paymentType = PaymentType.RECURRING,
                            periodicity = RecurrenceType.MONTHLY,
                            periodicityQtyLimit = 3,
                            tags = null,
                        ),
                )
            }

            then {
                recurrenceSegmentsShouldBe(3)
                recurrenceSeriesTagsShouldBeNull()
            }
        }
    }

    @Test
    fun `should not split when editing next occurrence and only reducing installment qty`() {
        val today = LocalDate.of(2026, 1, 8)
        val secondOccurrence = today.plusMonths(1)
        lateinit var bankAccountId: UUID

        walletScenario(initialDate = today) {
            given {
                user(defaultCurrency = "BRL")
                bankAccountId = bankAccount(balance = BigDecimal("1000.00"), currency = "BRL")
            }

            `when` {
                createEntry(
                    NewEntryRequest(
                        type = WalletEntryType.EXPENSE,
                        originId = bankAccountId,
                        date = today,
                        value = BigDecimal("100.00"),
                        confirmed = true,
                        paymentType = PaymentType.INSTALLMENTS,
                        installments = 5,
                        periodicity = RecurrenceType.MONTHLY,
                    ),
                )

                editScheduled(
                    occurrenceDate = secondOccurrence,
                    scope = ScheduledEditScope.THIS_AND_FUTURE,
                    newEntryRequest =
                        NewEntryRequest(
                            type = WalletEntryType.EXPENSE,
                            originId = bankAccountId,
                            date = secondOccurrence,
                            value = BigDecimal("100.00"),
                            confirmed = true,
                            paymentType = PaymentType.INSTALLMENTS,
                            installments = 3,
                            periodicity = RecurrenceType.MONTHLY,
                        ),
                )
            }

            then {
                recurrenceSegmentsShouldBe(1)
                recurrenceLimitShouldBe(3)
                recurrenceSeriesQtyTotalShouldBe(3)
                scheduledInstallmentsShouldBe(
                    filter = ScheduledExecutionFilter.FUTURE,
                    expectedInstallments = listOf(2),
                    expectedTotal = 3,
                )
            }
        }
    }

    @Test
    fun `should close recurrence when new limit equals already executed on THIS_AND_FUTURE edit`() {
        val today = LocalDate.of(2026, 1, 8)
        val thirdOccurrence = today.plusMonths(2)
        lateinit var bankAccountId: UUID

        walletScenario(initialDate = today) {
            given {
                user(defaultCurrency = "BRL")
                bankAccountId = bankAccount(balance = BigDecimal("1000.00"), currency = "BRL")
            }

            `when` {
                createEntry(
                    NewEntryRequest(
                        type = WalletEntryType.EXPENSE,
                        originId = bankAccountId,
                        date = today,
                        value = BigDecimal("100.00"),
                        confirmed = true,
                        paymentType = PaymentType.INSTALLMENTS,
                        installments = 5,
                        periodicity = RecurrenceType.MONTHLY,
                    ),
                )

                advanceTimeToNextRecurrenceExecution()
                runRecurrence()

                editScheduled(
                    occurrenceDate = thirdOccurrence,
                    scope = ScheduledEditScope.THIS_AND_FUTURE,
                    newEntryRequest =
                        NewEntryRequest(
                            type = WalletEntryType.EXPENSE,
                            originId = bankAccountId,
                            date = thirdOccurrence,
                            value = BigDecimal("100.00"),
                            confirmed = true,
                            paymentType = PaymentType.INSTALLMENTS,
                            installments = 2,
                            periodicity = RecurrenceType.MONTHLY,
                        ),
                )
            }

            then {
                recurrenceLimitShouldBe(2)
                recurrenceNextExecutionShouldBe(null)
                recurrenceSeriesQtyTotalShouldBe(2)
                scheduledManagerCountShouldBe(ScheduledExecutionFilter.FUTURE, 0)
            }
        }
    }

    @Test
    fun `should reject THIS_AND_FUTURE edit when new limit is lower than already executed`() {
        val today = LocalDate.of(2026, 1, 8)
        val thirdOccurrence = today.plusMonths(2)

        assertThrows<InvalidRecurrenceQtyLimitException> {
            walletScenario(initialDate = today) {
                lateinit var bankAccountId: UUID

                given {
                    user(defaultCurrency = "BRL")
                    bankAccountId = bankAccount(balance = BigDecimal("1000.00"), currency = "BRL")
                }

                `when` {
                    createEntry(
                        NewEntryRequest(
                            type = WalletEntryType.EXPENSE,
                            originId = bankAccountId,
                            date = today,
                            value = BigDecimal("100.00"),
                            confirmed = true,
                            paymentType = PaymentType.INSTALLMENTS,
                            installments = 5,
                            periodicity = RecurrenceType.MONTHLY,
                        ),
                    )

                    advanceTimeToNextRecurrenceExecution()
                    runRecurrence()

                    editScheduled(
                        occurrenceDate = thirdOccurrence,
                        scope = ScheduledEditScope.THIS_AND_FUTURE,
                        newEntryRequest =
                            NewEntryRequest(
                                type = WalletEntryType.EXPENSE,
                                originId = bankAccountId,
                                date = thirdOccurrence,
                                value = BigDecimal("100.00"),
                                confirmed = true,
                                paymentType = PaymentType.INSTALLMENTS,
                                installments = 1,
                                periodicity = RecurrenceType.MONTHLY,
                            ),
                    )
                }
            }
        }
    }

    @Test
    fun `should delete all linked recurrence segments on ALL_SERIES from derived segment`() {
        val today = LocalDate.of(2026, 1, 8)
        val secondOccurrence = today.plusMonths(1)
        val thirdOccurrence = today.plusMonths(2)
        lateinit var bankAccountId: UUID

        walletScenario(initialDate = today) {
            given {
                user(defaultCurrency = "BRL")
                bankAccountId = bankAccount(balance = BigDecimal("1000.00"), currency = "BRL")
            }

            `when` {
                createEntry(
                    NewEntryRequest(
                        type = WalletEntryType.EXPENSE,
                        originId = bankAccountId,
                        date = today,
                        value = BigDecimal("100.00"),
                        confirmed = true,
                        paymentType = PaymentType.INSTALLMENTS,
                        installments = 3,
                        periodicity = RecurrenceType.MONTHLY,
                    ),
                )

                editScheduled(
                    occurrenceDate = secondOccurrence,
                    scope = ScheduledEditScope.THIS_AND_FUTURE,
                    newEntryRequest =
                        NewEntryRequest(
                            type = WalletEntryType.EXPENSE,
                            originId = bankAccountId,
                            date = secondOccurrence,
                            value = BigDecimal("80.00"),
                            confirmed = true,
                            paymentType = PaymentType.INSTALLMENTS,
                            installments = 3,
                            periodicity = RecurrenceType.MONTHLY,
                        ),
                )

                editScheduled(
                    occurrenceDate = thirdOccurrence,
                    scope = ScheduledEditScope.ONLY_THIS,
                    newEntryRequest =
                        NewEntryRequest(
                            type = WalletEntryType.EXPENSE,
                            originId = bankAccountId,
                            date = thirdOccurrence,
                            value = BigDecimal("70.00"),
                            confirmed = true,
                            paymentType = PaymentType.INSTALLMENTS,
                            installments = 3,
                            periodicity = RecurrenceType.MONTHLY,
                        ),
                )

                clearPublishedEvents()
                deleteScheduled(
                    occurrenceDate = secondOccurrence,
                    scope = ScheduledEditScope.ALL_SERIES,
                )
            }

            then {
                scheduledManagerCountShouldBe(ScheduledExecutionFilter.FUTURE, 0)
                scheduledManagerCountShouldBe(ScheduledExecutionFilter.ALREADY_GENERATED, 0)
                scheduledManagerCountShouldBe(ScheduledExecutionFilter.ALL, 0)
                balanceShouldBe(BigDecimal("1000.00"), bankAccountId)
                publishedWalletEventsShouldBe(ActionEventType.DELETE, 1)
            }
        }
    }

    @Test
    fun `should increment recurrence series qty_total on unlimited recurring executions`() {
        val today = LocalDate.of(2026, 1, 8)
        lateinit var bankAccountId: UUID

        walletScenario(initialDate = today) {
            given {
                user(defaultCurrency = "BRL")
                bankAccountId = bankAccount(balance = BigDecimal("1000.00"), currency = "BRL")
            }

            `when` {
                createEntry(
                    NewEntryRequest(
                        type = WalletEntryType.EXPENSE,
                        originId = bankAccountId,
                        date = today,
                        value = BigDecimal("50.00"),
                        confirmed = true,
                        paymentType = PaymentType.RECURRING,
                        periodicity = RecurrenceType.MONTHLY,
                        periodicityQtyLimit = null,
                    ),
                )
            }

            then {
                recurrenceSeriesQtyTotalShouldBe(1)
            }

            `when` {
                advanceTimeToNextRecurrenceExecution()
                runRecurrence()
            }

            then {
                recurrenceSeriesQtyTotalShouldBe(2)
            }

            `when` {
                advanceTimeToNextRecurrenceExecution()
                runRecurrence()
            }

            then {
                recurrenceSeriesQtyTotalShouldBe(3)
            }
        }
    }

    @Test
    fun `should recompute generated scheduled occurrence without posting for non-generated edits`() {
        val today = LocalDate.of(2026, 1, 8)
        val executionDate = today.plusDays(1)
        lateinit var bankAccountId: UUID

        walletScenario(initialDate = today) {
            given {
                user(defaultCurrency = "BRL")
                bankAccountId = bankAccount(balance = BigDecimal("1000.00"), currency = "BRL")
            }

            `when` {
                createEntry(
                    NewEntryRequest(
                        type = WalletEntryType.EXPENSE,
                        originId = bankAccountId,
                        date = executionDate,
                        value = BigDecimal("100.00"),
                        confirmed = true,
                        paymentType = PaymentType.UNIQUE,
                    ),
                )
            }

            then {
                balanceShouldBe(BigDecimal("1000.00"), bankAccountId)
            }

            `when` {
                advanceTimeToNextRecurrenceExecution()
                runRecurrence()
            }

            then {
                balanceShouldBe(BigDecimal("900.00"), bankAccountId)
            }

            `when` {
                clearPublishedEvents()
                editScheduled(
                    occurrenceDate = executionDate,
                    scope = ScheduledEditScope.ONLY_THIS,
                    newEntryRequest =
                        NewEntryRequest(
                            type = WalletEntryType.EXPENSE,
                            originId = bankAccountId,
                            date = executionDate,
                            value = BigDecimal("40.00"),
                            confirmed = true,
                            paymentType = PaymentType.UNIQUE,
                        ),
                )
            }

            then {
                balanceShouldBe(BigDecimal("960.00"), bankAccountId)
                publishedWalletEventsShouldBe(ActionEventType.UPDATE, 1)
            }
        }
    }

    @Test
    fun `should update installment reservation delta immediately while keeping invoice posting on generation`() {
        val today = LocalDate.of(2026, 1, 8)
        val totalLimit = BigDecimal("3000.00")
        val dueDay = 20
        val daysBetweenDueAndClosing = 10
        val dueOnNextBusinessDay = false
        val expectedCardModel =
            CreditCard(
                name = "Expected Card",
                enabled = true,
                userId = UUID.randomUUID(),
                currency = "BRL",
                totalLimit = totalLimit,
                balance = totalLimit,
                dueDay = dueDay,
                daysBetweenDueAndClosing = daysBetweenDueAndClosing,
                dueOnNextBusinessDay = dueOnNextBusinessDay,
            )
        val billDate1 = expectedCardModel.getBestBill(today)
        val billDate2 = billDate1.plusMonths(1)
        val secondOccurrence = today.plusMonths(1)

        lateinit var creditCardId: UUID

        walletScenario(initialDate = today) {
            given {
                user(defaultCurrency = "BRL")
                creditCardId =
                    creditCard(
                        limit = totalLimit,
                        currency = "BRL",
                        dueDay = dueDay,
                        daysBetweenDueAndClosing = daysBetweenDueAndClosing,
                        dueOnNextBusinessDay = dueOnNextBusinessDay,
                    )
            }

            `when` {
                installmentPurchase(
                    total = BigDecimal("300.00"),
                    installments = 3,
                    originId = creditCardId,
                    date = today,
                )
            }

            then {
                availableLimitShouldBe(BigDecimal("2700.00"), creditCardId)
                billValueShouldBe(BigDecimal("-100.00"), billDate = billDate1, creditCardId = creditCardId)
            }

            `when` {
                clearPublishedEvents()
                editScheduled(
                    occurrenceDate = secondOccurrence,
                    scope = ScheduledEditScope.ONLY_THIS,
                    newEntryRequest =
                        NewEntryRequest(
                            type = WalletEntryType.EXPENSE,
                            originId = creditCardId,
                            date = secondOccurrence,
                            value = BigDecimal("150.00"),
                            confirmed = true,
                            paymentType = PaymentType.INSTALLMENTS,
                            installments = 3,
                            periodicity = RecurrenceType.MONTHLY,
                            originBillDate = billDate2,
                        ),
                )
            }

            then {
                availableLimitShouldBe(BigDecimal("2650.00"), creditCardId)
                billValueShouldBe(BigDecimal("-100.00"), billDate = billDate1, creditCardId = creditCardId)
                publishedWalletEventsShouldBe(ActionEventType.UPDATE, 1)
            }

            `when` {
                advanceTimeToNextRecurrenceExecution()
                runRecurrence()
            }

            then {
                billValueShouldBe(BigDecimal("-150.00"), billDate = billDate2, creditCardId = creditCardId)
            }
        }
    }

    @Test
    fun `should reject scheduled recurrence edit without bill date for credit card`() {
        val today = LocalDate.of(2026, 1, 8)
        val dueDay = 20
        val daysBetweenDueAndClosing = 10
        val dueOnNextBusinessDay = false
        val expectedCardModel =
            CreditCard(
                name = "Expected Card",
                enabled = true,
                userId = UUID.randomUUID(),
                currency = "BRL",
                totalLimit = BigDecimal("3000.00"),
                balance = BigDecimal("3000.00"),
                dueDay = dueDay,
                daysBetweenDueAndClosing = daysBetweenDueAndClosing,
                dueOnNextBusinessDay = dueOnNextBusinessDay,
            )

        val firstOccurrence = today.plusDays(1)
        val secondOccurrence = firstOccurrence.plusMonths(1)
        val firstBillDate = expectedCardModel.getBestBill(firstOccurrence)

        assertThrows<InvalidWalletSourceSplitException> {
            walletScenario(initialDate = today) {
                lateinit var creditCardId: UUID

                given {
                    user(defaultCurrency = "BRL")
                    creditCardId =
                        creditCard(
                            limit = BigDecimal("3000.00"),
                            currency = "BRL",
                            dueDay = dueDay,
                            daysBetweenDueAndClosing = daysBetweenDueAndClosing,
                            dueOnNextBusinessDay = dueOnNextBusinessDay,
                        )
                }

                `when` {
                    createEntry(
                        NewEntryRequest(
                            type = WalletEntryType.EXPENSE,
                            originId = creditCardId,
                            date = firstOccurrence,
                            value = BigDecimal("100.00"),
                            confirmed = true,
                            paymentType = PaymentType.RECURRING,
                            periodicity = RecurrenceType.MONTHLY,
                            periodicityQtyLimit = 2,
                            originBillDate = firstBillDate,
                        ),
                    )

                    editScheduled(
                        occurrenceDate = secondOccurrence,
                        scope = ScheduledEditScope.THIS_AND_FUTURE,
                        newEntryRequest =
                            NewEntryRequest(
                                type = WalletEntryType.EXPENSE,
                                originId = creditCardId,
                                date = secondOccurrence,
                                value = BigDecimal("80.00"),
                                confirmed = true,
                                paymentType = PaymentType.RECURRING,
                                periodicity = RecurrenceType.MONTHLY,
                                periodicityQtyLimit = 2,
                                originBillDate = null,
                            ),
                    )
                }
            }
        }
    }

    @Test
    fun `should respect manual bill during generation when scheduled recurrence edit provides bill explicitly`() {
        val today = LocalDate.of(2026, 1, 8)
        val dueDay = 20
        val daysBetweenDueAndClosing = 10
        val dueOnNextBusinessDay = false
        val expectedCardModel =
            CreditCard(
                name = "Expected Card",
                enabled = true,
                userId = UUID.randomUUID(),
                currency = "BRL",
                totalLimit = BigDecimal("3000.00"),
                balance = BigDecimal("3000.00"),
                dueDay = dueDay,
                daysBetweenDueAndClosing = daysBetweenDueAndClosing,
                dueOnNextBusinessDay = dueOnNextBusinessDay,
            )

        val firstOccurrence = today.plusDays(1)
        val secondOccurrence = firstOccurrence.plusMonths(1)
        val firstBillDate = expectedCardModel.getBestBill(firstOccurrence)
        val secondAutoBillDate = expectedCardModel.getBestBill(secondOccurrence)
        val secondManualBillDate = secondAutoBillDate.plusMonths(1)

        lateinit var creditCardId: UUID

        walletScenario(initialDate = today) {
            given {
                user(defaultCurrency = "BRL")
                creditCardId =
                    creditCard(
                        limit = BigDecimal("3000.00"),
                        currency = "BRL",
                        dueDay = dueDay,
                        daysBetweenDueAndClosing = daysBetweenDueAndClosing,
                        dueOnNextBusinessDay = dueOnNextBusinessDay,
                    )
            }

            `when` {
                createEntry(
                    NewEntryRequest(
                        type = WalletEntryType.EXPENSE,
                        originId = creditCardId,
                        date = firstOccurrence,
                        value = BigDecimal("100.00"),
                        confirmed = true,
                        paymentType = PaymentType.RECURRING,
                        periodicity = RecurrenceType.MONTHLY,
                        periodicityQtyLimit = 2,
                        originBillDate = firstBillDate,
                    ),
                )

                editScheduled(
                    occurrenceDate = secondOccurrence,
                    scope = ScheduledEditScope.THIS_AND_FUTURE,
                    newEntryRequest =
                        NewEntryRequest(
                            type = WalletEntryType.EXPENSE,
                            originId = creditCardId,
                            date = secondOccurrence,
                            value = BigDecimal("80.00"),
                            confirmed = true,
                            paymentType = PaymentType.RECURRING,
                            periodicity = RecurrenceType.MONTHLY,
                            periodicityQtyLimit = 2,
                            originBillDate = secondManualBillDate,
                        ),
                )
            }

            `when` {
                advanceTimeToNextRecurrenceExecution()
                runRecurrence()
            }

            then {
                billValueShouldBe(BigDecimal("-100.00"), billDate = firstBillDate, creditCardId = creditCardId)
            }

            `when` {
                advanceTimeToNextRecurrenceExecution()
                runRecurrence()
            }

            then {
                billShouldNotExist(billDate = secondAutoBillDate, creditCardId = creditCardId)
                billValueShouldBe(BigDecimal("-80.00"), billDate = secondManualBillDate, creditCardId = creditCardId)
            }
        }
    }

    @Test
    fun `should recalculate future installment dates and bills on THIS_AND_FUTURE edit`() {
        val today = LocalDate.of(2026, 3, 15)
        val dueDay = 20
        val daysBetweenDueAndClosing = 10
        val dueOnNextBusinessDay = false
        val totalLimit = BigDecimal("3000.00")
        val expectedCardModel =
            CreditCard(
                name = "Expected Card",
                enabled = true,
                userId = UUID.randomUUID(),
                currency = "BRL",
                totalLimit = totalLimit,
                balance = totalLimit,
                dueDay = dueDay,
                daysBetweenDueAndClosing = daysBetweenDueAndClosing,
                dueOnNextBusinessDay = dueOnNextBusinessDay,
            )

        val firstOccurrence = LocalDate.of(2026, 5, 14)
        val movedOccurrence = LocalDate.of(2026, 4, 20)
        val secondRecalculatedOccurrence = movedOccurrence.plusMonths(1)
        val thirdRecalculatedOccurrence = movedOccurrence.plusMonths(2)
        val movedBillDate1 = expectedCardModel.getBestBill(movedOccurrence)
        val movedBillDate2 = expectedCardModel.getBestBill(secondRecalculatedOccurrence)
        val movedBillDate3 = expectedCardModel.getBestBill(thirdRecalculatedOccurrence)
        val originalBillDate1 = expectedCardModel.getBestBill(firstOccurrence)

        lateinit var creditCardId: UUID

        walletScenario(initialDate = today) {
            given {
                user(defaultCurrency = "BRL")
                creditCardId =
                    creditCard(
                        limit = totalLimit,
                        currency = "BRL",
                        dueDay = dueDay,
                        daysBetweenDueAndClosing = daysBetweenDueAndClosing,
                        dueOnNextBusinessDay = dueOnNextBusinessDay,
                    )
            }

            `when` {
                installmentPurchase(
                    total = BigDecimal("400.00"),
                    installments = 4,
                    originId = creditCardId,
                    date = firstOccurrence,
                )

                editScheduled(
                    occurrenceDate = firstOccurrence,
                    scope = ScheduledEditScope.THIS_AND_FUTURE,
                    newEntryRequest =
                        NewEntryRequest(
                            type = WalletEntryType.EXPENSE,
                            originId = creditCardId,
                            date = movedOccurrence,
                            value = BigDecimal("100.00"),
                            confirmed = true,
                            paymentType = PaymentType.INSTALLMENTS,
                            installments = 4,
                            periodicity = RecurrenceType.MONTHLY,
                            originBillDate = movedBillDate1,
                        ),
                )
            }

            then {
                recurrenceNextExecutionShouldBe(movedOccurrence)
            }

            `when` {
                advanceTime(movedOccurrence)
                runRecurrence()
            }

            then {
                recurrenceNextExecutionShouldBe(secondRecalculatedOccurrence)
                billValueShouldBe(BigDecimal("-100.00"), billDate = movedBillDate1, creditCardId = creditCardId)
            }

            `when` {
                advanceTime(secondRecalculatedOccurrence)
                runRecurrence()
            }

            then {
                recurrenceNextExecutionShouldBe(thirdRecalculatedOccurrence)
                billValueShouldBe(BigDecimal("-100.00"), billDate = movedBillDate2, creditCardId = creditCardId)
            }

            `when` {
                advanceTime(thirdRecalculatedOccurrence)
                runRecurrence()
            }

            then {
                billValueShouldBe(BigDecimal("-100.00"), billDate = movedBillDate3, creditCardId = creditCardId)
            }
        }
    }

    @Test
    fun `should keep intermediate credit card installment bill when moving next scheduled installment to past with THIS_AND_FUTURE`() {
        val creationDate = LocalDate.of(2026, 4, 10)
        val today = LocalDate.of(2026, 5, 14)
        val dueDay = 20
        val daysBetweenDueAndClosing = 10
        val dueOnNextBusinessDay = false
        val totalLimit = BigDecimal("3000.00")
        val expectedCardModel =
            CreditCard(
                name = "Expected Card",
                enabled = true,
                userId = UUID.randomUUID(),
                currency = "BRL",
                totalLimit = totalLimit,
                balance = totalLimit,
                dueDay = dueDay,
                daysBetweenDueAndClosing = daysBetweenDueAndClosing,
                dueOnNextBusinessDay = dueOnNextBusinessDay,
            )

        val originalOccurrence = LocalDate.of(2026, 5, 14)
        val movedOccurrence = LocalDate.of(2026, 4, 14)
        val nextRecalculatedOccurrence = LocalDate.of(2026, 5, 14)
        val followingOccurrence = LocalDate.of(2026, 6, 14)
        val movedBillDate = expectedCardModel.getBestBill(movedOccurrence)
        val nextBillDate = expectedCardModel.getBestBill(nextRecalculatedOccurrence)
        val installmentValue = BigDecimal("160.00")

        lateinit var creditCardId: UUID

        walletScenario(initialDate = creationDate) {
            given {
                user(defaultCurrency = "BRL")
                creditCardId =
                    creditCard(
                        limit = totalLimit,
                        currency = "BRL",
                        dueDay = dueDay,
                        daysBetweenDueAndClosing = daysBetweenDueAndClosing,
                        dueOnNextBusinessDay = dueOnNextBusinessDay,
                    )
            }

            `when` {
                installmentPurchase(
                    total = BigDecimal("800.00"),
                    installments = 5,
                    originId = creditCardId,
                    date = originalOccurrence,
                )
                advanceTime(today)
                editScheduled(
                    occurrenceDate = originalOccurrence,
                    scope = ScheduledEditScope.THIS_AND_FUTURE,
                    newEntryRequest =
                        NewEntryRequest(
                            type = WalletEntryType.EXPENSE,
                            originId = creditCardId,
                            date = movedOccurrence,
                            value = installmentValue,
                            confirmed = true,
                            paymentType = PaymentType.INSTALLMENTS,
                            installments = 5,
                            periodicity = RecurrenceType.MONTHLY,
                            originBillDate = movedBillDate,
                        ),
                )
            }

            then {
                billValueShouldBe(installmentValue.unaryMinus(), billDate = movedBillDate, creditCardId = creditCardId)
                recurrenceNextExecutionShouldBe(nextRecalculatedOccurrence)
            }

            `when` {
                runRecurrence()
            }

            then {
                billValueShouldBe(installmentValue.unaryMinus(), billDate = nextBillDate, creditCardId = creditCardId)
                recurrenceNextExecutionShouldBe(followingOccurrence)
            }
        }
    }

    @Test
    fun `should reanchor generated installment and preserve limit on past THIS_AND_FUTURE edit`() {
        val today = LocalDate.of(2026, 5, 14)
        val dueDay = 1
        val daysBetweenDueAndClosing = 10
        val dueOnNextBusinessDay = false
        val totalLimit = BigDecimal("3000.00")
        val expectedCardModel =
            CreditCard(
                name = "Expected Card",
                enabled = true,
                userId = UUID.randomUUID(),
                currency = "BRL",
                totalLimit = totalLimit,
                balance = totalLimit,
                dueDay = dueDay,
                daysBetweenDueAndClosing = daysBetweenDueAndClosing,
                dueOnNextBusinessDay = dueOnNextBusinessDay,
            )

        val originalOccurrence = today
        val movedOccurrence = LocalDate.of(2026, 4, 22)
        val nextRecalculatedOccurrence = LocalDate.of(2026, 5, 22)
        val followingOccurrence = LocalDate.of(2026, 6, 22)
        val movedBillDate = expectedCardModel.getBestBill(movedOccurrence)
        val juneBillDate = expectedCardModel.getBestBill(nextRecalculatedOccurrence)
        val installmentValue = BigDecimal("160.00")

        lateinit var creditCardId: UUID

        walletScenario(initialDate = today) {
            given {
                user(defaultCurrency = "BRL")
                creditCardId =
                    creditCard(
                        limit = totalLimit,
                        currency = "BRL",
                        dueDay = dueDay,
                        daysBetweenDueAndClosing = daysBetweenDueAndClosing,
                        dueOnNextBusinessDay = dueOnNextBusinessDay,
                    )
            }

            `when` {
                installmentPurchase(
                    total = BigDecimal("800.00"),
                    installments = 5,
                    originId = creditCardId,
                    date = originalOccurrence,
                )
                editScheduled(
                    occurrenceDate = originalOccurrence,
                    scope = ScheduledEditScope.THIS_AND_FUTURE,
                    newEntryRequest =
                        NewEntryRequest(
                            type = WalletEntryType.EXPENSE,
                            originId = creditCardId,
                            date = movedOccurrence,
                            value = installmentValue,
                            confirmed = true,
                            paymentType = PaymentType.INSTALLMENTS,
                            installments = 5,
                            periodicity = RecurrenceType.MONTHLY,
                            originBillDate = movedBillDate,
                        ),
                )
            }

            then {
                availableLimitShouldBe(BigDecimal("2200.00"), creditCardId)
                billValueShouldBe(installmentValue.unaryMinus(), billDate = movedBillDate, creditCardId = creditCardId)
                recurrenceNextExecutionShouldBe(nextRecalculatedOccurrence)
                scheduledManagerDatesShouldBe(
                    filter = ScheduledExecutionFilter.ALL,
                    expectedDates = listOf(movedOccurrence, nextRecalculatedOccurrence),
                )
            }

            `when` {
                advanceTime(nextRecalculatedOccurrence)
                runRecurrence()
            }

            then {
                availableLimitShouldBe(BigDecimal("2200.00"), creditCardId)
                billValueShouldBe(installmentValue.unaryMinus(), billDate = juneBillDate, creditCardId = creditCardId)
                recurrenceNextExecutionShouldBe(followingOccurrence)
            }
        }
    }

    @Test
    fun `should keep limit and monthly sequence when moving generated first credit card installment with THIS_AND_FUTURE`() {
        val today = LocalDate.of(2026, 5, 15)
        val dueDay = 1
        val daysBetweenDueAndClosing = 2
        val dueOnNextBusinessDay = false
        val totalLimit = BigDecimal("1000.00")
        val expectedCardModel =
            CreditCard(
                name = "Expected Card",
                enabled = true,
                userId = UUID.randomUUID(),
                currency = "BRL",
                totalLimit = totalLimit,
                balance = totalLimit,
                dueDay = dueDay,
                daysBetweenDueAndClosing = daysBetweenDueAndClosing,
                dueOnNextBusinessDay = dueOnNextBusinessDay,
            )

        val originalOccurrence = today
        val movedOccurrence = LocalDate.of(2026, 4, 22)
        val recalculatedSecondOccurrence = LocalDate.of(2026, 5, 22)
        val recalculatedThirdOccurrence = LocalDate.of(2026, 6, 22)
        val originalBillDate = expectedCardModel.getBestBill(originalOccurrence)
        val movedBillDate = expectedCardModel.getBestBill(movedOccurrence)
        val secondOccurrenceBillDate = expectedCardModel.getBestBill(recalculatedSecondOccurrence)
        val installmentValue = BigDecimal("100.00")

        lateinit var creditCardId: UUID

        walletScenario(initialDate = today) {
            given {
                user(defaultCurrency = "BRL")
                creditCardId =
                    creditCard(
                        limit = totalLimit,
                        currency = "BRL",
                        dueDay = dueDay,
                        daysBetweenDueAndClosing = daysBetweenDueAndClosing,
                        dueOnNextBusinessDay = dueOnNextBusinessDay,
                    )
            }

            `when` {
                createEntry(
                    NewEntryRequest(
                        type = WalletEntryType.EXPENSE,
                        date = originalOccurrence,
                        value = installmentValue,
                        confirmed = true,
                        paymentType = PaymentType.INSTALLMENTS,
                        installments = 5,
                        periodicity = RecurrenceType.MONTHLY,
                        sources =
                            listOf(
                                NewWalletSourceLeg(
                                    walletItemId = creditCardId,
                                    contributionPercent = BigDecimal("100.00"),
                                    billDate = originalBillDate,
                                ),
                            ),
                    ),
                )
                editScheduled(
                    occurrenceDate = originalOccurrence,
                    scope = ScheduledEditScope.THIS_AND_FUTURE,
                    newEntryRequest =
                        NewEntryRequest(
                            type = WalletEntryType.EXPENSE,
                            date = movedOccurrence,
                            value = installmentValue,
                            confirmed = true,
                            paymentType = PaymentType.INSTALLMENTS,
                            installments = 5,
                            periodicity = RecurrenceType.MONTHLY,
                            sources =
                                listOf(
                                    NewWalletSourceLeg(
                                        walletItemId = creditCardId,
                                        contributionPercent = BigDecimal("100.00"),
                                        billDate = movedBillDate,
                                    ),
                                ),
                        ),
                )
                fetchFirstScheduledExecution(ScheduledExecutionFilter.FUTURE)
            }

            then {
                availableLimitShouldBe(BigDecimal("500.00"), creditCardId)
                billValueShouldBe(installmentValue.unaryMinus(), billDate = movedBillDate, creditCardId = creditCardId)
                recurrenceNextExecutionShouldBe(recalculatedSecondOccurrence)
                scheduledManagerDatesShouldBe(
                    filter = ScheduledExecutionFilter.ALL,
                    expectedDates = listOf(movedOccurrence, recalculatedSecondOccurrence),
                )
                fetchedScheduledWalletEventShouldExist()
                fetchedScheduledWalletEventDateShouldBe(recalculatedSecondOccurrence)
                fetchedScheduledWalletEventInstallmentShouldBe(2)
                fetchedScheduledWalletEventEntryBillDateShouldBe(secondOccurrenceBillDate)
            }

            `when` {
                advanceTime(recalculatedSecondOccurrence)
                runRecurrence()
            }

            then {
                availableLimitShouldBe(BigDecimal("500.00"), creditCardId)
                recurrenceNextExecutionShouldBe(recalculatedThirdOccurrence)
                billValueShouldBe(installmentValue.unaryMinus(), billDate = secondOccurrenceBillDate, creditCardId = creditCardId)
                scheduledManagerDatesShouldBe(
                    filter = ScheduledExecutionFilter.ALL,
                    expectedDates = listOf(movedOccurrence, recalculatedSecondOccurrence, recalculatedThirdOccurrence),
                )
            }
        }
    }

    @Test
    fun `should keep limit and monthly sequence when moving future first credit card installment with THIS_AND_FUTURE`() {
        val today = LocalDate.of(2026, 5, 15)
        val originalOccurrence = LocalDate.of(2026, 5, 16)
        val movedOccurrence = LocalDate.of(2026, 4, 22)
        val recalculatedSecondOccurrence = LocalDate.of(2026, 5, 22)
        val recalculatedThirdOccurrence = LocalDate.of(2026, 6, 22)
        val dueDay = 1
        val daysBetweenDueAndClosing = 2
        val dueOnNextBusinessDay = false
        val totalLimit = BigDecimal("1000.00")
        val installmentValue = BigDecimal("100.00")
        val expectedCardModel =
            CreditCard(
                name = "Expected Card",
                enabled = true,
                userId = UUID.randomUUID(),
                currency = "BRL",
                totalLimit = totalLimit,
                balance = totalLimit,
                dueDay = dueDay,
                daysBetweenDueAndClosing = daysBetweenDueAndClosing,
                dueOnNextBusinessDay = dueOnNextBusinessDay,
            )
        val originalBillDate = expectedCardModel.getBestBill(originalOccurrence)
        val movedBillDate = expectedCardModel.getBestBill(movedOccurrence)
        val secondOccurrenceBillDate = expectedCardModel.getBestBill(recalculatedSecondOccurrence)

        lateinit var creditCardId: UUID

        walletScenario(initialDate = today) {
            given {
                user(defaultCurrency = "BRL")
                creditCardId =
                    creditCard(
                        limit = totalLimit,
                        name = "BBB",
                        currency = "BRL",
                        dueDay = dueDay,
                        daysBetweenDueAndClosing = daysBetweenDueAndClosing,
                        dueOnNextBusinessDay = dueOnNextBusinessDay,
                    )
            }

            `when` {
                createEntry(
                    NewEntryRequest(
                        type = WalletEntryType.EXPENSE,
                        date = originalOccurrence,
                        value = installmentValue,
                        name = "AAA",
                        confirmed = false,
                        paymentType = PaymentType.INSTALLMENTS,
                        installments = 5,
                        periodicity = RecurrenceType.MONTHLY,
                        tags = emptyList(),
                        sources =
                            listOf(
                                NewWalletSourceLeg(
                                    walletItemId = creditCardId,
                                    contributionPercent = BigDecimal("100.00"),
                                    billDate = originalBillDate,
                                ),
                            ),
                    ),
                )
                editScheduled(
                    occurrenceDate = originalOccurrence,
                    scope = ScheduledEditScope.THIS_AND_FUTURE,
                    newEntryRequest =
                        NewEntryRequest(
                            type = WalletEntryType.EXPENSE,
                            date = movedOccurrence,
                            value = installmentValue,
                            name = "AAA",
                            confirmed = false,
                            paymentType = PaymentType.INSTALLMENTS,
                            installments = 5,
                            periodicity = RecurrenceType.MONTHLY,
                            tags = emptyList(),
                            sources =
                                listOf(
                                    NewWalletSourceLeg(
                                        walletItemId = creditCardId,
                                        contributionPercent = BigDecimal("100.00"),
                                        billDate = movedBillDate,
                                    ),
                                ),
                        ),
                )
                fetchFirstScheduledExecution(ScheduledExecutionFilter.FUTURE)
            }

            then {
                availableLimitShouldBe(BigDecimal("500.00"), creditCardId)
                billValueShouldBe(installmentValue.unaryMinus(), billDate = movedBillDate, creditCardId = creditCardId)
                recurrenceNextExecutionShouldBe(recalculatedSecondOccurrence)
                scheduledManagerDatesShouldBe(
                    filter = ScheduledExecutionFilter.ALL,
                    expectedDates = listOf(movedOccurrence, recalculatedSecondOccurrence),
                )
                fetchedScheduledWalletEventShouldExist()
                fetchedScheduledWalletEventDateShouldBe(recalculatedSecondOccurrence)
                fetchedScheduledWalletEventInstallmentShouldBe(2)
                fetchedScheduledWalletEventEntryBillDateShouldBe(secondOccurrenceBillDate)
            }

            `when` {
                advanceTime(recalculatedSecondOccurrence)
                runRecurrence()
            }

            then {
                availableLimitShouldBe(BigDecimal("500.00"), creditCardId)
                recurrenceNextExecutionShouldBe(recalculatedThirdOccurrence)
                billValueShouldBe(installmentValue.unaryMinus(), billDate = secondOccurrenceBillDate, creditCardId = creditCardId)
                scheduledManagerDatesShouldBe(
                    filter = ScheduledExecutionFilter.ALL,
                    expectedDates = listOf(movedOccurrence, recalculatedSecondOccurrence, recalculatedThirdOccurrence),
                )
            }
        }
    }

    @Test
    fun `should delete one-off transaction and publish delete event`() {
        val today = LocalDate.of(2026, 1, 8)
        lateinit var bankAccountId: UUID
        lateinit var walletEventId: UUID

        walletScenario(initialDate = today) {
            given {
                user(defaultCurrency = "BRL")
                bankAccountId = bankAccount(balance = BigDecimal("1000.00"), currency = "BRL")
            }

            `when` {
                expense(
                    value = BigDecimal("100.00"),
                    originId = bankAccountId,
                    date = today,
                    confirmed = true,
                )
                walletEventId = lastWalletEventId()
                clearPublishedEvents()
                deleteOneOff(walletEventId)
                fetchWalletEventById(walletEventId)
            }

            then {
                balanceShouldBe(BigDecimal("1000.00"), bankAccountId)
                fetchedWalletEventShouldNotExist()
                publishedWalletEventsShouldBe(ActionEventType.DELETE, 1)
            }
        }
    }

    @Test
    fun `should delete scheduled unique occurrence without explicit scope`() {
        val today = LocalDate.of(2026, 1, 8)
        val occurrenceDate = today.plusDays(1)
        lateinit var bankAccountId: UUID

        walletScenario(initialDate = today) {
            given {
                user(defaultCurrency = "BRL")
                bankAccountId = bankAccount(balance = BigDecimal("1000.00"), currency = "BRL")
            }

            `when` {
                createEntry(
                    NewEntryRequest(
                        type = WalletEntryType.EXPENSE,
                        originId = bankAccountId,
                        date = occurrenceDate,
                        value = BigDecimal("80.00"),
                        confirmed = true,
                        paymentType = PaymentType.UNIQUE,
                    ),
                )

                clearPublishedEvents()
                deleteScheduled(
                    occurrenceDate = occurrenceDate,
                    scope = null,
                )
            }

            then {
                scheduledManagerCountShouldBe(ScheduledExecutionFilter.FUTURE, 0)
                publishedWalletEventsShouldBe(ActionEventType.DELETE, 1)
            }
        }
    }

    @Test
    fun `should delete this and future scheduled occurrences`() {
        val today = LocalDate.of(2026, 1, 8)
        val firstOccurrence = today.plusDays(1)
        val secondOccurrence = firstOccurrence.plusMonths(1)
        lateinit var bankAccountId: UUID

        walletScenario(initialDate = today) {
            given {
                user(defaultCurrency = "BRL")
                bankAccountId = bankAccount(balance = BigDecimal("1000.00"), currency = "BRL")
            }

            `when` {
                createEntry(
                    NewEntryRequest(
                        type = WalletEntryType.EXPENSE,
                        originId = bankAccountId,
                        date = firstOccurrence,
                        value = BigDecimal("100.00"),
                        confirmed = true,
                        paymentType = PaymentType.RECURRING,
                        periodicity = RecurrenceType.MONTHLY,
                        periodicityQtyLimit = 3,
                    ),
                )

                clearPublishedEvents()
                deleteScheduled(
                    occurrenceDate = secondOccurrence,
                    scope = ScheduledEditScope.THIS_AND_FUTURE,
                )
            }

            then {
                scheduledManagerCountShouldBe(ScheduledExecutionFilter.FUTURE, 1)
                publishedWalletEventsShouldBe(ActionEventType.DELETE, 1)
            }
        }
    }

    @Test
    fun `should map available limit across one-off and installment edits with refund and series deletion`() {
        val today = LocalDate.of(2026, 1, 8)
        val secondOccurrence = today.plusMonths(1)
        val totalLimit = BigDecimal("1000.00")
        val expectedCardModel =
            CreditCard(
                name = "Expected Card",
                enabled = true,
                userId = UUID.randomUUID(),
                currency = "BRL",
                totalLimit = totalLimit,
                balance = totalLimit,
                dueDay = 20,
                daysBetweenDueAndClosing = 10,
                dueOnNextBusinessDay = false,
            )
        val billDate1 = expectedCardModel.getBestBill(today)
        val billDate2 = expectedCardModel.getBestBill(secondOccurrence)

        lateinit var creditCardId: UUID
        lateinit var oneOffPurchaseId: UUID
        lateinit var installmentRecurrenceConfigId: UUID

        walletScenario(initialDate = today) {
            given {
                user(defaultCurrency = "BRL")
                creditCardId =
                    creditCard(
                        limit = totalLimit,
                        currency = "BRL",
                        dueDay = 20,
                        daysBetweenDueAndClosing = 10,
                        dueOnNextBusinessDay = false,
                    )
            }

            `when` {
                expense(
                    value = BigDecimal("300.00"),
                    originId = creditCardId,
                    date = today,
                    confirmed = true,
                    billDate = billDate1,
                )
                oneOffPurchaseId = lastWalletEventId()
            }

            then {
                availableLimitShouldBe(BigDecimal("700.00"), creditCardId)
            }

            `when` {
                editOneOff(
                    walletEventId = oneOffPurchaseId,
                    newEntryRequest =
                        NewEntryRequest(
                            type = WalletEntryType.EXPENSE,
                            originId = creditCardId,
                            date = today,
                            value = BigDecimal("200.00"),
                            confirmed = true,
                            paymentType = PaymentType.UNIQUE,
                            originBillDate = billDate1,
                        ),
                )
            }

            then {
                availableLimitShouldBe(BigDecimal("800.00"), creditCardId)
            }

            `when` {
                installmentPurchase(
                    total = BigDecimal("300.00"),
                    installments = 3,
                    originId = creditCardId,
                    date = today,
                )
            }

            then {
                availableLimitShouldBe(BigDecimal("500.00"), creditCardId)
            }

            `when` {
                editScheduled(
                    occurrenceDate = secondOccurrence,
                    scope = ScheduledEditScope.THIS_AND_FUTURE,
                    newEntryRequest =
                        NewEntryRequest(
                            type = WalletEntryType.EXPENSE,
                            originId = creditCardId,
                            date = secondOccurrence,
                            value = BigDecimal("100.00"),
                            confirmed = true,
                            paymentType = PaymentType.INSTALLMENTS,
                            installments = 4,
                            periodicity = RecurrenceType.MONTHLY,
                            originBillDate = billDate2,
                        ),
                )
            }

            then {
                availableLimitShouldBe(BigDecimal("400.00"), creditCardId)
            }

            `when` {
                editScheduled(
                    occurrenceDate = secondOccurrence,
                    scope = ScheduledEditScope.THIS_AND_FUTURE,
                    newEntryRequest =
                        NewEntryRequest(
                            type = WalletEntryType.EXPENSE,
                            originId = creditCardId,
                            date = secondOccurrence,
                            value = BigDecimal("100.00"),
                            confirmed = true,
                            paymentType = PaymentType.INSTALLMENTS,
                            installments = 2,
                            periodicity = RecurrenceType.MONTHLY,
                            originBillDate = billDate2,
                        ),
                )
                installmentRecurrenceConfigId = lastRecurrenceConfigId()
            }

            then {
                availableLimitShouldBe(BigDecimal("600.00"), creditCardId)
            }

            `when` {
                revenue(
                    value = BigDecimal("50.00"),
                    originId = creditCardId,
                    date = today,
                    confirmed = true,
                )
            }

            then {
                availableLimitShouldBe(BigDecimal("650.00"), creditCardId)
            }

            `when` {
                deleteScheduled(
                    recurrenceConfigId = installmentRecurrenceConfigId,
                    occurrenceDate = secondOccurrence,
                    scope = ScheduledEditScope.ALL_SERIES,
                )
            }

            then {
                availableLimitShouldBe(BigDecimal("850.00"), creditCardId)
            }
        }
    }

    @Test
    fun `should map available limit from 400 to 850 when shrinking installments refunding and deleting series`() {
        val today = LocalDate.of(2026, 1, 8)
        val secondOccurrence = today.plusMonths(1)
        val totalLimit = BigDecimal("1000.00")
        val expectedCardModel =
            CreditCard(
                name = "Expected Card",
                enabled = true,
                userId = UUID.randomUUID(),
                currency = "BRL",
                totalLimit = totalLimit,
                balance = totalLimit,
                dueDay = 20,
                daysBetweenDueAndClosing = 10,
                dueOnNextBusinessDay = false,
            )
        val billDate1 = expectedCardModel.getBestBill(today)
        val billDate2 = expectedCardModel.getBestBill(secondOccurrence)

        lateinit var creditCardId: UUID
        lateinit var installmentRecurrenceConfigId: UUID

        walletScenario(initialDate = today) {
            given {
                user(defaultCurrency = "BRL")
                creditCardId =
                    creditCard(
                        limit = totalLimit,
                        currency = "BRL",
                        dueDay = 20,
                        daysBetweenDueAndClosing = 10,
                        dueOnNextBusinessDay = false,
                    )
            }

            `when` {
                expense(
                    value = BigDecimal("200.00"),
                    originId = creditCardId,
                    date = today,
                    confirmed = true,
                    billDate = billDate1,
                )
                installmentPurchase(
                    total = BigDecimal("400.00"),
                    installments = 4,
                    originId = creditCardId,
                    date = today,
                )
            }

            then {
                availableLimitShouldBe(BigDecimal("400.00"), creditCardId)
            }

            `when` {
                editScheduled(
                    occurrenceDate = secondOccurrence,
                    scope = ScheduledEditScope.THIS_AND_FUTURE,
                    newEntryRequest =
                        NewEntryRequest(
                            type = WalletEntryType.EXPENSE,
                            originId = creditCardId,
                            date = secondOccurrence,
                            value = BigDecimal("100.00"),
                            confirmed = true,
                            paymentType = PaymentType.INSTALLMENTS,
                            installments = 2,
                            periodicity = RecurrenceType.MONTHLY,
                            originBillDate = billDate2,
                        ),
                )
                installmentRecurrenceConfigId = lastRecurrenceConfigId()
            }

            then {
                availableLimitShouldBe(BigDecimal("600.00"), creditCardId)
            }

            `when` {
                revenue(
                    value = BigDecimal("50.00"),
                    originId = creditCardId,
                    date = today,
                    confirmed = true,
                )
            }

            then {
                availableLimitShouldBe(BigDecimal("650.00"), creditCardId)
            }

            `when` {
                deleteScheduled(
                    recurrenceConfigId = installmentRecurrenceConfigId,
                    occurrenceDate = secondOccurrence,
                    scope = ScheduledEditScope.ALL_SERIES,
                )
            }

            then {
                availableLimitShouldBe(BigDecimal("850.00"), creditCardId)
            }
        }
    }

    @Test
    fun `should move installment purchase from credit card XX to credit card YY`() {
        val today = LocalDate.of(2026, 1, 8)
        val secondOccurrence = today.plusMonths(1)
        val xxLimit = BigDecimal("1000.00")
        val yyLimit = BigDecimal("400.00")
        val dueDay = 20
        val daysBetweenDueAndClosing = 10
        val dueOnNextBusinessDay = false
        val expectedYyCardModel =
            CreditCard(
                name = "YY",
                enabled = true,
                userId = UUID.randomUUID(),
                currency = "BRL",
                totalLimit = yyLimit,
                balance = yyLimit,
                dueDay = dueDay,
                daysBetweenDueAndClosing = daysBetweenDueAndClosing,
                dueOnNextBusinessDay = dueOnNextBusinessDay,
            )
        val yyBillDateOnSecondOccurrence = expectedYyCardModel.getBestBill(secondOccurrence)

        lateinit var cardXxId: UUID
        lateinit var cardYyId: UUID

        walletScenario(initialDate = today) {
            given {
                user(defaultCurrency = "BRL")
                cardXxId =
                    creditCard(
                        name = "XX",
                        limit = xxLimit,
                        currency = "BRL",
                        dueDay = dueDay,
                        daysBetweenDueAndClosing = daysBetweenDueAndClosing,
                        dueOnNextBusinessDay = dueOnNextBusinessDay,
                    )
                cardYyId =
                    creditCard(
                        name = "YY",
                        limit = yyLimit,
                        currency = "BRL",
                        dueDay = dueDay,
                        daysBetweenDueAndClosing = daysBetweenDueAndClosing,
                        dueOnNextBusinessDay = dueOnNextBusinessDay,
                    )
            }

            `when` {
                installmentPurchase(
                    total = BigDecimal("300.00"),
                    installments = 3,
                    originId = cardXxId,
                    date = today,
                )
            }

            then {
                availableLimitShouldBe(BigDecimal("700.00"), cardXxId)
            }

            `when` {
                editScheduled(
                    occurrenceDate = secondOccurrence,
                    scope = ScheduledEditScope.THIS_AND_FUTURE,
                    newEntryRequest =
                        NewEntryRequest(
                            type = WalletEntryType.EXPENSE,
                            originId = cardYyId,
                            date = secondOccurrence,
                            value = BigDecimal("100.00"),
                            confirmed = true,
                            paymentType = PaymentType.INSTALLMENTS,
                            installments = 3,
                            periodicity = RecurrenceType.MONTHLY,
                            originBillDate = yyBillDateOnSecondOccurrence,
                        ),
                )
            }

            then {
                availableLimitShouldBe(BigDecimal("1000.00"), cardXxId)
                availableLimitShouldBe(BigDecimal("100.00"), cardYyId)
            }
        }
    }

    @Test
    fun `should move installment purchase from credit card XX to bank account ZZ`() {
        val today = LocalDate.of(2026, 1, 8)
        val secondOccurrence = today.plusMonths(1)
        val xxLimit = BigDecimal("1000.00")

        lateinit var cardXxId: UUID
        lateinit var bankZzId: UUID

        walletScenario(initialDate = today) {
            given {
                user(defaultCurrency = "BRL")
                cardXxId =
                    creditCard(
                        name = "XX",
                        limit = xxLimit,
                        currency = "BRL",
                        dueDay = 20,
                        daysBetweenDueAndClosing = 10,
                        dueOnNextBusinessDay = false,
                    )
                bankZzId =
                    bankAccount(
                        name = "ZZ",
                        balance = BigDecimal("2000.00"),
                        currency = "BRL",
                    )
            }

            `when` {
                installmentPurchase(
                    total = BigDecimal("300.00"),
                    installments = 3,
                    originId = cardXxId,
                    date = today,
                )
            }

            then {
                availableLimitShouldBe(BigDecimal("700.00"), cardXxId)
            }

            `when` {
                editScheduled(
                    occurrenceDate = secondOccurrence,
                    scope = ScheduledEditScope.THIS_AND_FUTURE,
                    newEntryRequest =
                        NewEntryRequest(
                            type = WalletEntryType.EXPENSE,
                            originId = bankZzId,
                            date = secondOccurrence,
                            value = BigDecimal("100.00"),
                            confirmed = true,
                            paymentType = PaymentType.INSTALLMENTS,
                            installments = 3,
                            periodicity = RecurrenceType.MONTHLY,
                        ),
                )
            }

            then {
                availableLimitShouldBe(BigDecimal("1000.00"), cardXxId)
                balanceShouldBe(BigDecimal("900.00"), bankZzId)
            }
        }
    }
}

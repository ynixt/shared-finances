package com.ynixt.sharedfinances.scenarios.wallet

import com.ynixt.sharedfinances.domain.exceptions.http.GroupNotFoundException
import com.ynixt.sharedfinances.scenarios.support.ScenarioGroupService
import com.ynixt.sharedfinances.scenarios.wallet.support.walletScenario
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

class GroupTransferScenarioDslTest {
    @Test
    fun `should throw permission error when transferring to bank account outside group scope`() {
        val today = LocalDate.of(2026, 1, 8)
        val transferValue = BigDecimal("100.00")
        val groupService = ScenarioGroupService()
        val groupId = groupService.createGroup(name = "Shared Group")
        lateinit var groupMemberUserId: UUID
        lateinit var groupMemberBankAccountId: UUID
        lateinit var outsideBankAccountId: UUID

        val scenario =
            walletScenario(initialDate = today, groupService = groupService) {
                given {
                    user(
                        email = "outside-user@example.com",
                        defaultCurrency = "BRL",
                    )
                    outsideBankAccountId =
                        bankAccount(
                            name = "Outside User Account",
                            balance = BigDecimal("500.00"),
                            currency = "BRL",
                        )

                    groupMemberUserId =
                        user(
                            email = "group-member@example.com",
                            defaultCurrency = "BRL",
                        )
                    groupMemberBankAccountId =
                        bankAccount(
                            name = "Group Member Account",
                            balance = BigDecimal("1000.00"),
                            currency = "BRL",
                        )
                }
            }

        groupService.upsertMemberScope(
            groupId = groupId,
            userId = groupMemberUserId,
            associatedItemIds = setOf(groupMemberBankAccountId),
        )

        assertThatThrownBy {
            runBlocking {
                scenario.whenActions.transfer(
                    value = transferValue,
                    date = today,
                    groupId = groupId,
                    originId = groupMemberBankAccountId,
                    targetId = outsideBankAccountId,
                    name = "Unauthorized Group Transfer",
                    confirmed = true,
                )
            }
        }.isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("insufficient permissions")
    }

    @Test
    fun `should throw permission error when user outside group tries transfer to bank account inside group`() {
        val today = LocalDate.of(2026, 1, 8)
        val transferValue = BigDecimal("100.00")
        val groupService = ScenarioGroupService()
        val groupId = groupService.createGroup(name = "Shared Group")

        lateinit var outsideUserBankAccountId: UUID
        lateinit var groupMemberUserId: UUID
        lateinit var groupMemberBankAccountId: UUID

        val scenario =
            walletScenario(initialDate = today, groupService = groupService) {
                given {
                    groupMemberUserId =
                        user(
                            email = "group-member-inside@example.com",
                            defaultCurrency = "BRL",
                        )
                    groupMemberBankAccountId =
                        bankAccount(
                            name = "Group Member Inside Account",
                            balance = BigDecimal("700.00"),
                            currency = "BRL",
                        )

                    user(
                        email = "outside-user-transfer@example.com",
                        defaultCurrency = "BRL",
                    )
                    outsideUserBankAccountId =
                        bankAccount(
                            name = "Outside User Transfer Account",
                            balance = BigDecimal("900.00"),
                            currency = "BRL",
                        )
                }
            }

        groupService.upsertMemberScope(
            groupId = groupId,
            userId = groupMemberUserId,
            associatedItemIds = setOf(groupMemberBankAccountId),
        )

        assertThatThrownBy {
            runBlocking {
                scenario.whenActions.transfer(
                    value = transferValue,
                    date = today,
                    groupId = groupId,
                    originId = outsideUserBankAccountId,
                    targetId = groupMemberBankAccountId,
                    name = "Outside User To Group Account Transfer",
                    confirmed = true,
                )
            }
        }.isInstanceOf(GroupNotFoundException::class.java)
    }

    @Test
    fun `should transfer between users in same group and update balances of both accounts`() {
        val today = LocalDate.of(2026, 1, 8)
        val transferValue = BigDecimal("150.00")
        val senderInitialBalance = BigDecimal("1000.00")
        val receiverInitialBalance = BigDecimal("300.00")
        val expectedSenderBalance = senderInitialBalance.subtract(transferValue)
        val expectedReceiverBalance = receiverInitialBalance.add(transferValue)
        val groupService = ScenarioGroupService()
        val groupId = groupService.createGroup(name = "Shared Group")

        lateinit var senderUserId: UUID
        lateinit var receiverUserId: UUID
        lateinit var senderBankAccountId: UUID
        lateinit var receiverBankAccountId: UUID

        walletScenario(initialDate = today, groupService = groupService) {
            given {
                receiverUserId =
                    user(
                        email = "receiver@example.com",
                        defaultCurrency = "BRL",
                    )
                receiverBankAccountId =
                    bankAccount(
                        name = "Receiver Account",
                        balance = receiverInitialBalance,
                        currency = "BRL",
                    )

                senderUserId =
                    user(
                        email = "sender@example.com",
                        defaultCurrency = "BRL",
                    )
                senderBankAccountId =
                    bankAccount(
                        name = "Sender Account",
                        balance = senderInitialBalance,
                        currency = "BRL",
                    )
            }

            `when` {
                groupService.upsertMemberScope(
                    groupId = groupId,
                    userId = senderUserId,
                    associatedItemIds = setOf(senderBankAccountId, receiverBankAccountId),
                )
                groupService.upsertMemberScope(
                    groupId = groupId,
                    userId = receiverUserId,
                    associatedItemIds = setOf(senderBankAccountId, receiverBankAccountId),
                )

                transfer(
                    value = transferValue,
                    date = today,
                    groupId = groupId,
                    originId = senderBankAccountId,
                    targetId = receiverBankAccountId,
                    name = "Group Bank Transfer",
                    confirmed = true,
                )
            }

            then {
                balanceShouldBe(
                    expected = expectedSenderBalance,
                    bankAccountId = senderBankAccountId,
                )
                balanceShouldBe(
                    expected = expectedReceiverBalance,
                    bankAccountId = receiverBankAccountId,
                )
            }
        }
    }
}

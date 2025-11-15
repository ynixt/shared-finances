package com.ynixt.sharedfinances.domain.services.groups

import com.ynixt.sharedfinances.domain.models.bankaccount.BankAccount
import reactor.core.publisher.Mono
import java.util.UUID

interface GroupBankAssociationService {
    fun findAllAllowedBanksToAssociate(
        userId: UUID,
        groupId: UUID,
    ): Mono<List<BankAccount>>

    fun findAllAssociatedBanks(
        userId: UUID,
        groupId: UUID,
    ): Mono<List<BankAccount>>

    fun associateBank(
        userId: UUID,
        groupId: UUID,
        bankAccountId: UUID,
    ): Mono<Unit>

    fun unassociateBank(
        userId: UUID,
        groupId: UUID,
        bankAccountId: UUID,
    ): Mono<Unit>
}

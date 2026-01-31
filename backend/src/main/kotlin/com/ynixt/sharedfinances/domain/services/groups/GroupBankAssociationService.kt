package com.ynixt.sharedfinances.domain.services.groups

import com.ynixt.sharedfinances.domain.models.bankaccount.BankAccount
import java.util.UUID

interface GroupBankAssociationService {
    suspend fun findAllAllowedBanksToAssociate(
        userId: UUID,
        groupId: UUID,
    ): List<BankAccount>

    suspend fun findAllAssociatedBanks(
        userId: UUID,
        groupId: UUID,
    ): List<BankAccount>

    suspend fun associateBank(
        userId: UUID,
        groupId: UUID,
        bankAccountId: UUID,
    ): Boolean

    suspend fun unassociateBank(
        userId: UUID,
        groupId: UUID,
        bankAccountId: UUID,
    ): Boolean
}

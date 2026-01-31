package com.ynixt.sharedfinances.domain.services.actionevents

import com.ynixt.sharedfinances.domain.entities.groups.GroupEntity
import com.ynixt.sharedfinances.domain.entities.groups.GroupWalletItemEntity
import com.ynixt.sharedfinances.domain.models.groups.GroupWithRole
import java.util.UUID

interface GroupActionEventService {
    suspend fun sendInsertedGroup(
        userId: UUID,
        group: GroupEntity,
    )

    suspend fun sendUpdatedGroup(
        userId: UUID,
        group: GroupWithRole,
    )

    suspend fun sendDeletedGroup(
        userId: UUID,
        id: UUID,
        membersId: List<UUID>,
    )

    suspend fun sendBankAssociated(
        userId: UUID,
        groupBankAccount: GroupWalletItemEntity,
    )

    suspend fun sendBankUnassociated(
        userId: UUID,
        groupId: UUID,
        bankAccountId: UUID,
    )

    suspend fun sendCreditCardAssociated(
        userId: UUID,
        groupCreditCard: GroupWalletItemEntity,
    )

    suspend fun sendCreditCardUnassociated(
        userId: UUID,
        groupId: UUID,
        creditCardId: UUID,
    )
}

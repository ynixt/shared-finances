package com.ynixt.sharedfinances.domain.services.actionevents

import com.ynixt.sharedfinances.domain.entities.groups.GroupEntity
import com.ynixt.sharedfinances.domain.entities.groups.GroupWalletItemEntity
import java.util.UUID

interface GroupActionEventService {
    suspend fun sendInsertedGroup(
        userId: UUID,
        group: GroupEntity,
    )

    suspend fun sendUpdatedGroup(
        userId: UUID,
        groupId: UUID,
        name: String,
    )

    suspend fun sendOwnershipChanged(
        userId: UUID,
        groupId: UUID,
        previousOwnerUserId: UUID,
        newOwnerUserId: UUID,
    )

    suspend fun sendMemberLeft(
        userId: UUID,
        groupId: UUID,
        departedUserId: UUID,
        membersId: List<UUID>,
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

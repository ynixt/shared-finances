package com.ynixt.sharedfinances.domain.services.actionevents

import com.ynixt.sharedfinances.domain.entities.groups.GroupEntity
import com.ynixt.sharedfinances.domain.entities.groups.GroupWalletItemEntity
import com.ynixt.sharedfinances.domain.models.groups.GroupWithRole
import reactor.core.publisher.Mono
import java.util.UUID

interface GroupActionEventService {
    fun sendInsertedGroup(
        userId: UUID,
        group: GroupEntity,
    ): Mono<Long>

    fun sendUpdatedGroup(
        userId: UUID,
        group: GroupWithRole,
    ): Mono<Long>

    fun sendDeletedGroup(
        userId: UUID,
        id: UUID,
        membersId: List<UUID>,
    ): Mono<Long>

    fun sendBankAssociated(
        userId: UUID,
        groupBankAccount: GroupWalletItemEntity,
    ): Mono<Long>

    fun sendBankUnassociated(
        userId: UUID,
        groupId: UUID,
        bankAccountId: UUID,
    ): Mono<Long>

    fun sendCreditCardAssociated(
        userId: UUID,
        groupCreditCard: GroupWalletItemEntity,
    ): Mono<Long>

    fun sendCreditCardUnassociated(
        userId: UUID,
        groupId: UUID,
        creditCardId: UUID,
    ): Mono<Long>
}

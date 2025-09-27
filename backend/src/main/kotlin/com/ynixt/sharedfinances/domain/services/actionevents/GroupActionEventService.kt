package com.ynixt.sharedfinances.domain.services.actionevents

import com.ynixt.sharedfinances.domain.entities.groups.Group
import com.ynixt.sharedfinances.domain.entities.groups.GroupBankAccount
import com.ynixt.sharedfinances.domain.models.groups.GroupWithRole
import reactor.core.publisher.Mono
import java.util.UUID

interface GroupActionEventService {
    fun sendInsertedGroup(
        userId: UUID,
        group: Group,
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
        groupBankAccount: GroupBankAccount,
    ): Mono<Long>

    fun sendBankUnassociated(
        userId: UUID,
        groupId: UUID,
        bankAccountId: UUID,
    ): Mono<Long>
}

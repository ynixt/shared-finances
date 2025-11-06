package com.ynixt.sharedfinances.domain.repositories

import com.ynixt.sharedfinances.domain.entities.groups.GroupCreditCard
import reactor.core.publisher.Mono
import java.util.UUID

interface GroupCreditCardRepository {
    fun save(groupUser: GroupCreditCard): Mono<GroupCreditCard>

    fun deleteByGroupIdAndCreditCardId(
        groupId: UUID,
        creditCardId: UUID,
    ): Mono<Long>
}

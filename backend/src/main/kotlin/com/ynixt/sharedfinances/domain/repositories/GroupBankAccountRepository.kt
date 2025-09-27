package com.ynixt.sharedfinances.domain.repositories

import com.ynixt.sharedfinances.domain.entities.groups.GroupBankAccount
import reactor.core.publisher.Mono
import java.util.UUID

interface GroupBankAccountRepository {
    fun save(groupUser: GroupBankAccount): Mono<GroupBankAccount>

    fun deleteByGroupIdAndBankAccountId(
        groupId: UUID,
        bankAccountId: UUID,
    ): Mono<Long>
}

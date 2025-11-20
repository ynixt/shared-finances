package com.ynixt.sharedfinances.resources.repositories.springdata

import com.ynixt.sharedfinances.domain.entities.groups.GroupWalletItemEntity
import org.springframework.data.repository.Repository
import reactor.core.publisher.Mono
import java.util.UUID

interface GroupWalletItemSpringDataRepository : Repository<GroupWalletItemEntity, String> {
    fun countByGroupId(
        groupId: UUID,
        enabled: Boolean,
    ): Mono<Long>

    fun save(groupUser: GroupWalletItemEntity): Mono<GroupWalletItemEntity>

    fun deleteByGroupIdAndWalletItemId(
        groupId: UUID,
        walletItemId: UUID,
    ): Mono<Long>
}

package com.ynixt.sharedfinances.resources.repositories.r2dbc.springdata

import com.ynixt.sharedfinances.domain.entities.groups.GroupWalletItemEntity
import org.springframework.data.r2dbc.repository.R2dbcRepository
import reactor.core.publisher.Mono
import java.util.UUID

interface GroupWalletItemSpringDataRepository : R2dbcRepository<GroupWalletItemEntity, String> {
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

package com.ynixt.sharedfinances.resources.repositories.springdata

import com.ynixt.sharedfinances.domain.entities.groups.GroupWalletItem
import org.springframework.data.repository.Repository
import reactor.core.publisher.Mono
import java.util.UUID

interface GroupWalletItemSpringDataRepository : Repository<GroupWalletItem, String> {
    fun countByGroupId(
        groupId: UUID,
        enabled: Boolean,
    ): Mono<Long>

    fun save(groupUser: GroupWalletItem): Mono<GroupWalletItem>

    fun deleteByGroupIdAndWalletItemId(
        groupId: UUID,
        walletItemId: UUID,
    ): Mono<Long>
}

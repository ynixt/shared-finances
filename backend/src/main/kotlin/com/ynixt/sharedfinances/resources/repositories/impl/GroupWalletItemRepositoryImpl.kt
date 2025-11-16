package com.ynixt.sharedfinances.resources.repositories.impl

import com.ynixt.sharedfinances.domain.entities.groups.GroupWalletItem
import com.ynixt.sharedfinances.domain.entities.wallet.WalletItemEntity
import com.ynixt.sharedfinances.domain.enums.WalletItemType
import com.ynixt.sharedfinances.domain.repositories.GroupWalletItemRepository
import com.ynixt.sharedfinances.resources.repositories.r2dbc.GroupWalletItemR2DBCRepository
import com.ynixt.sharedfinances.resources.repositories.springdata.GroupWalletItemSpringDataRepository
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.UUID

@Repository
class GroupWalletItemRepositoryImpl(
    private val springDataRepository: GroupWalletItemSpringDataRepository,
    private val r2DBCRepository: GroupWalletItemR2DBCRepository,
) : GroupWalletItemRepository {
    override fun findAllByGroupIdAndEnabled(
        groupId: UUID,
        enabled: Boolean,
        pageable: Pageable,
    ): Flux<WalletItemEntity> = r2DBCRepository.findAllByGroupIdAndEnabled(groupId, enabled, pageable)

    override fun countByGroupId(
        groupId: UUID,
        enabled: Boolean,
    ): Mono<Long> = springDataRepository.countByGroupId(groupId, enabled)

    override fun save(groupUser: GroupWalletItem): Mono<GroupWalletItem> = springDataRepository.save(groupUser)

    override fun deleteByGroupIdAndWalletItemId(
        groupId: UUID,
        walletItemId: UUID,
    ): Mono<Long> = springDataRepository.deleteByGroupIdAndWalletItemId(groupId, walletItemId)

    override fun findAllAllowedForGroup(
        groupId: UUID,
        type: WalletItemType,
    ): Flux<WalletItemEntity> = r2DBCRepository.findAllAllowedForGroup(groupId, type)

    override fun findAllAssociatedToGroup(
        groupId: UUID,
        type: WalletItemType,
    ): Flux<WalletItemEntity> = r2DBCRepository.findAllAssociatedToGroup(groupId, type)
}

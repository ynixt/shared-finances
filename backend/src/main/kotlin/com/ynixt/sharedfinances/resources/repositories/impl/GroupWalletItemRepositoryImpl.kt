package com.ynixt.sharedfinances.resources.repositories.impl

import com.ynixt.sharedfinances.domain.entities.groups.GroupWalletItemEntity
import com.ynixt.sharedfinances.domain.entities.wallet.WalletItemEntity
import com.ynixt.sharedfinances.domain.enums.WalletItemType
import com.ynixt.sharedfinances.domain.repositories.GroupWalletItemRepository
import com.ynixt.sharedfinances.resources.repositories.r2dbc.databaseclient.GroupWalletItemDatabaseClientRepository
import com.ynixt.sharedfinances.resources.repositories.r2dbc.springdata.GroupWalletItemSpringDataRepository
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.UUID

@Repository
class GroupWalletItemRepositoryImpl(
    private val springDataRepository: GroupWalletItemSpringDataRepository,
    private val dcRepository: GroupWalletItemDatabaseClientRepository,
) : GroupWalletItemRepository {
    override fun findAllByGroupIdAndEnabled(
        groupId: UUID,
        enabled: Boolean,
        pageable: Pageable,
        walletItemType: WalletItemType?,
    ): Flux<WalletItemEntity> = dcRepository.findAllByGroupIdAndEnabled(groupId, enabled, pageable, walletItemType)

    override fun countByGroupIdAndEnabled(
        groupId: UUID,
        enabled: Boolean,
        walletItemType: WalletItemType?,
    ): Mono<Long> = dcRepository.countByGroupIdAndEnabled(groupId, enabled, walletItemType)

    override fun save(groupUser: GroupWalletItemEntity): Mono<GroupWalletItemEntity> = springDataRepository.save(groupUser)

    override fun deleteByGroupIdAndWalletItemId(
        groupId: UUID,
        walletItemId: UUID,
    ): Mono<Long> = springDataRepository.deleteByGroupIdAndWalletItemId(groupId, walletItemId)

    override fun findAllAllowedForGroup(
        groupId: UUID,
        type: WalletItemType,
    ): Flux<WalletItemEntity> = dcRepository.findAllAllowedForGroup(groupId, type)

    override fun findAllAssociatedToGroup(
        groupId: UUID,
        type: WalletItemType,
    ): Flux<WalletItemEntity> = dcRepository.findAllAssociatedToGroup(groupId, type)

    override fun countByGroupIdAndWalletItemId(
        groupId: UUID,
        walletItemId: UUID,
    ): Mono<Long> = springDataRepository.countByGroupIdAndWalletItemId(groupId, walletItemId)
}

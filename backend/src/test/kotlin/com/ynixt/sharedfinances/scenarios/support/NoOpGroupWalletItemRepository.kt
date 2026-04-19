package com.ynixt.sharedfinances.scenarios.support

import com.ynixt.sharedfinances.domain.entities.groups.GroupWalletItemEntity
import com.ynixt.sharedfinances.domain.entities.wallet.WalletItemEntity
import com.ynixt.sharedfinances.domain.enums.WalletItemType
import com.ynixt.sharedfinances.domain.repositories.GroupWalletItemRepository
import org.springframework.data.domain.Pageable
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.UUID

/** Test stand-in when group–wallet associations are irrelevant to the scenario. */
internal class NoOpGroupWalletItemRepository : GroupWalletItemRepository {
    override fun findAllByGroupIdAndEnabled(
        groupId: UUID,
        enabled: Boolean,
        pageable: Pageable,
        walletItemType: WalletItemType?,
    ): Flux<WalletItemEntity> = Flux.empty()

    override fun countByGroupIdAndEnabled(
        groupId: UUID,
        enabled: Boolean,
        walletItemType: WalletItemType?,
    ): Mono<Long> = Mono.just(0L)

    override fun save(groupUser: GroupWalletItemEntity): Mono<GroupWalletItemEntity> = Mono.just(groupUser)

    override fun deleteByGroupIdAndWalletItemId(
        groupId: UUID,
        walletItemId: UUID,
    ): Mono<Long> = Mono.just(0L)

    override fun deleteAllForWalletItemsOwnedByUser(userId: UUID): Mono<Long> = Mono.just(0L)

    override fun findAllAllowedForGroup(
        userId: UUID,
        groupId: UUID,
        type: WalletItemType,
    ): Flux<WalletItemEntity> = Flux.empty()

    override fun findAllAssociatedToGroup(
        groupId: UUID,
        type: WalletItemType,
    ): Flux<WalletItemEntity> = Flux.empty()

    override fun countByGroupIdAndWalletItemId(
        groupId: UUID,
        walletItemId: UUID,
    ): Mono<Long> = Mono.just(0L)
}

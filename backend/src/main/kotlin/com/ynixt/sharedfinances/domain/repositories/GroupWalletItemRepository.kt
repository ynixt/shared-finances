package com.ynixt.sharedfinances.domain.repositories

import com.ynixt.sharedfinances.domain.entities.groups.GroupWalletItemEntity
import com.ynixt.sharedfinances.domain.entities.wallet.WalletItemEntity
import com.ynixt.sharedfinances.domain.enums.WalletItemType
import org.springframework.data.domain.Pageable
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.UUID

interface GroupWalletItemRepository {
    fun findAllByGroupIdAndEnabled(
        groupId: UUID,
        enabled: Boolean,
        pageable: Pageable,
        walletItemType: WalletItemType? = null,
    ): Flux<WalletItemEntity>

    fun countByGroupIdAndEnabled(
        groupId: UUID,
        enabled: Boolean,
        walletItemType: WalletItemType? = null,
    ): Mono<Long>

    fun save(groupUser: GroupWalletItemEntity): Mono<GroupWalletItemEntity>

    fun deleteByGroupIdAndWalletItemId(
        groupId: UUID,
        walletItemId: UUID,
    ): Mono<Long>

    /** Removes every group association for wallet items owned by the user (required before deleting the user / CASCADE on wallet_item). */
    fun deleteAllForWalletItemsOwnedByUser(userId: UUID): Mono<Long>

    fun findAllAllowedForGroup(
        groupId: UUID,
        type: WalletItemType,
    ): Flux<WalletItemEntity>

    fun findAllAssociatedToGroup(
        groupId: UUID,
        type: WalletItemType,
    ): Flux<WalletItemEntity>

    fun countByGroupIdAndWalletItemId(
        groupId: UUID,
        walletItemId: UUID,
    ): Mono<Long>
}

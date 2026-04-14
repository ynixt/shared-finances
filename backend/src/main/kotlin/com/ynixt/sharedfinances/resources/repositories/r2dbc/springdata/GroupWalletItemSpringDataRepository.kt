package com.ynixt.sharedfinances.resources.repositories.r2dbc.springdata

import com.ynixt.sharedfinances.domain.entities.groups.GroupWalletItemEntity
import org.springframework.data.r2dbc.repository.Modifying
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.r2dbc.repository.R2dbcRepository
import reactor.core.publisher.Mono
import java.util.UUID

interface GroupWalletItemSpringDataRepository : R2dbcRepository<GroupWalletItemEntity, String> {
    fun save(groupUser: GroupWalletItemEntity): Mono<GroupWalletItemEntity>

    fun deleteByGroupIdAndWalletItemId(
        groupId: UUID,
        walletItemId: UUID,
    ): Mono<Long>

    fun countByGroupIdAndWalletItemId(
        groupId: UUID,
        walletItemId: UUID,
    ): Mono<Long>

    @Modifying
    @Query(
        """
        DELETE FROM group_wallet_item gwi
        WHERE gwi.wallet_item_id IN (SELECT wi.id FROM wallet_item wi WHERE wi.user_id = :userId)
        """,
    )
    fun deleteAllForWalletItemsOwnedByUser(userId: UUID): Mono<Long>
}

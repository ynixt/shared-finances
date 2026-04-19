package com.ynixt.sharedfinances.resources.repositories.r2dbc.springdata

import com.ynixt.sharedfinances.domain.entities.groups.GroupMemberDebtMovementEntity
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.r2dbc.repository.R2dbcRepository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.UUID

interface GroupMemberDebtMovementSpringDataRepository : R2dbcRepository<GroupMemberDebtMovementEntity, String> {
    @Query(
        """
        SELECT m.*
        FROM group_member_debt_movement m
        WHERE m.source_wallet_event_id = :walletEventId
          AND NOT EXISTS (
              SELECT 1
              FROM group_member_debt_movement reversal
              WHERE reversal.source_movement_id = m.id
          )
        ORDER BY m.created_at ASC, m.id ASC
        """,
    )
    fun findActiveBySourceWalletEventId(walletEventId: UUID): Flux<GroupMemberDebtMovementEntity>

    fun findByIdAndGroupId(
        id: UUID,
        groupId: UUID,
    ): Mono<GroupMemberDebtMovementEntity>

    @Query(
        """
        SELECT *
        FROM group_member_debt_movement
        WHERE id = :rootMovementId OR source_movement_id = :rootMovementId
        ORDER BY created_at ASC, id ASC
        """,
    )
    fun findAdjustmentChain(rootMovementId: UUID): Flux<GroupMemberDebtMovementEntity>
}

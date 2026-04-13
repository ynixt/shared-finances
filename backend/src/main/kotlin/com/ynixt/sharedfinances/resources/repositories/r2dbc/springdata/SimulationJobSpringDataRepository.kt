package com.ynixt.sharedfinances.resources.repositories.r2dbc.springdata

import com.ynixt.sharedfinances.domain.entities.simulation.SimulationJobEntity
import org.springframework.data.domain.Pageable
import org.springframework.data.r2dbc.repository.Modifying
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.r2dbc.repository.R2dbcRepository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.OffsetDateTime
import java.util.UUID

interface SimulationJobSpringDataRepository : R2dbcRepository<SimulationJobEntity, String> {
    fun findByIdAndOwnerUserId(
        id: UUID,
        ownerUserId: UUID,
    ): Mono<SimulationJobEntity>

    fun findByIdAndOwnerGroupId(
        id: UUID,
        ownerGroupId: UUID,
    ): Mono<SimulationJobEntity>

    fun findAllByOwnerUserIdOrderByCreatedAtDescIdDesc(
        ownerUserId: UUID,
        pageable: Pageable,
    ): Flux<SimulationJobEntity>

    fun findAllByOwnerGroupIdOrderByCreatedAtDescIdDesc(
        ownerGroupId: UUID,
        pageable: Pageable,
    ): Flux<SimulationJobEntity>

    fun countByOwnerUserId(ownerUserId: UUID): Mono<Long>

    fun countByOwnerGroupId(ownerGroupId: UUID): Mono<Long>

    @Modifying
    @Query(
        """
        UPDATE simulation_job
        SET
            status = 'CANCELLED',
            cancelled_at = NOW(),
            lease_expires_at = NULL,
            worker_id = NULL,
            finished_at = NOW(),
            updated_at = NOW()
        WHERE
            id = :id
            AND owner_user_id = :ownerUserId
            AND status IN ('QUEUED', 'RUNNING')
        """,
    )
    fun cancelIfOwnedPending(
        id: UUID,
        ownerUserId: UUID,
    ): Mono<Long>

    @Modifying
    @Query(
        """
        UPDATE simulation_job
        SET
            status = 'CANCELLED',
            cancelled_at = NOW(),
            lease_expires_at = NULL,
            worker_id = NULL,
            finished_at = NOW(),
            updated_at = NOW()
        WHERE
            id = :id
            AND owner_group_id = :ownerGroupId
            AND status IN ('QUEUED', 'RUNNING')
        """,
    )
    fun cancelIfOwnedByGroupPending(
        id: UUID,
        ownerGroupId: UUID,
    ): Mono<Long>

    @Modifying
    @Query(
        """
        UPDATE simulation_job
        SET
            status = 'QUEUED',
            lease_expires_at = NULL,
            worker_id = NULL,
            started_at = NULL,
            updated_at = NOW()
        WHERE
            status = 'RUNNING'
            AND lease_expires_at IS NOT NULL
            AND lease_expires_at < :now
        """,
    )
    fun reconcileExpiredLeases(now: OffsetDateTime): Mono<Long>

    @Modifying
    @Query("DELETE FROM simulation_job WHERE created_at < :threshold")
    fun deleteAllByCreatedAtBefore(threshold: OffsetDateTime): Mono<Long>
}

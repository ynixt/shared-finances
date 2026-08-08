package com.ynixt.sharedfinances.resources.repositories.r2dbc.databaseclient

import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.OffsetDateTime
import java.util.UUID

interface ImportBatchDispatchRepository {
    fun queueUndo(
        batchId: UUID,
        userId: UUID,
    ): Mono<UUID>

    fun claimOldestQueuedForUser(
        userId: UUID,
        workerId: String,
        now: OffsetDateTime,
        leaseExpiresAt: OffsetDateTime,
    ): Mono<UUID>

    fun renewLease(
        batchId: UUID,
        workerId: String,
        leaseExpiresAt: OffsetDateTime,
    ): Mono<Long>

    fun markCompleted(
        batchId: UUID,
        workerId: String,
        finishedAt: OffsetDateTime,
    ): Mono<Long>

    fun deleteClaimedUndoBatch(
        batchId: UUID,
        workerId: String,
    ): Mono<Long>

    fun markQueuedForRetry(
        batchId: UUID,
        workerId: String,
        errorMessage: String,
    ): Mono<Long>

    fun markFailed(
        batchId: UUID,
        workerId: String,
        errorMessage: String,
        finishedAt: OffsetDateTime,
    ): Mono<Long>

    fun recoverExpiredLeases(
        now: OffsetDateTime,
        maxRetries: Int,
    ): Flux<UUID>

    fun findUsersReadyForDispatch(limit: Int): Flux<UUID>

    fun findOldestQueuedBatchId(userId: UUID): Mono<UUID>
}

@Repository
class ImportBatchDatabaseClientRepository(
    private val dbClient: DatabaseClient,
) : ImportBatchDispatchRepository {
    override fun queueUndo(
        batchId: UUID,
        userId: UUID,
    ): Mono<UUID> =
        dbClient
            .sql(
                """
                UPDATE import_batch
                SET
                    status = 'UNDO_QUEUED',
                    error_message = NULL,
                    lease_expires_at = NULL,
                    worker_id = NULL,
                    started_at = NULL,
                    finished_at = NULL,
                    retries = 0,
                    updated_at = NOW()
                WHERE id = :batchId
                  AND user_id = :userId
                  AND status IN ('COMPLETED', 'UNDO_FAILED')
                RETURNING id
                """,
            ).bind("batchId", batchId)
            .bind("userId", userId)
            .map { row, _ -> row.get("id", UUID::class.java)!! }
            .one()

    override fun claimOldestQueuedForUser(
        userId: UUID,
        workerId: String,
        now: OffsetDateTime,
        leaseExpiresAt: OffsetDateTime,
    ): Mono<UUID> =
        dbClient
            .sql(
                """
                WITH user_lock AS (
                    SELECT pg_try_advisory_xact_lock(hashtextextended(CAST(:userLockKey AS TEXT), 0)) AS locked
                ),
                next_batch AS (
                    SELECT id
                    FROM import_batch
                    WHERE user_id = :userId AND status IN ('QUEUED', 'UNDO_QUEUED')
                    ORDER BY created_at ASC, id ASC
                    LIMIT 1
                )
                UPDATE import_batch ib
                SET
                    status = CASE
                        WHEN ib.status = 'QUEUED' THEN 'RUNNING'
                        ELSE 'UNDO_RUNNING'
                    END,
                    started_at = COALESCE(started_at, :now),
                    lease_expires_at = :leaseExpiresAt,
                    worker_id = :workerId,
                    retries = retries + 1,
                    error_message = NULL,
                    updated_at = NOW()
                WHERE
                    (SELECT locked FROM user_lock)
                    AND ib.id = (SELECT id FROM next_batch)
                    AND ib.status IN ('QUEUED', 'UNDO_QUEUED')
                    AND NOT EXISTS (
                        SELECT 1 FROM import_batch running
                        WHERE running.user_id = :userId AND running.status IN ('RUNNING', 'UNDO_RUNNING')
                    )
                RETURNING ib.id
                """,
            ).bind("userId", userId)
            .bind("userLockKey", "import-user:$userId")
            .bind("workerId", workerId)
            .bind("now", now)
            .bind("leaseExpiresAt", leaseExpiresAt)
            .map { row, _ -> row.get("id", UUID::class.java)!! }
            .one()

    override fun renewLease(
        batchId: UUID,
        workerId: String,
        leaseExpiresAt: OffsetDateTime,
    ): Mono<Long> =
        dbClient
            .sql(
                """
                UPDATE import_batch
                SET lease_expires_at = :leaseExpiresAt, updated_at = NOW()
                WHERE id = :batchId AND status IN ('RUNNING', 'UNDO_RUNNING') AND worker_id = :workerId
                """,
            ).bind("batchId", batchId)
            .bind("workerId", workerId)
            .bind("leaseExpiresAt", leaseExpiresAt)
            .fetch()
            .rowsUpdated()

    override fun deleteClaimedUndoBatch(
        batchId: UUID,
        workerId: String,
    ): Mono<Long> =
        dbClient
            .sql(
                """
                DELETE FROM import_batch
                WHERE id = :batchId AND status = 'UNDO_RUNNING' AND worker_id = :workerId
                """,
            ).bind("batchId", batchId)
            .bind("workerId", workerId)
            .fetch()
            .rowsUpdated()

    override fun markCompleted(
        batchId: UUID,
        workerId: String,
        finishedAt: OffsetDateTime,
    ): Mono<Long> =
        dbClient
            .sql(
                """
                UPDATE import_batch
                SET
                    status = 'COMPLETED',
                    request_payload = NULL,
                    error_message = NULL,
                    lease_expires_at = NULL,
                    worker_id = NULL,
                    finished_at = :finishedAt,
                    updated_at = NOW()
                WHERE id = :batchId AND status = 'RUNNING' AND worker_id = :workerId
                """,
            ).bind("batchId", batchId)
            .bind("workerId", workerId)
            .bind("finishedAt", finishedAt)
            .fetch()
            .rowsUpdated()

    override fun markQueuedForRetry(
        batchId: UUID,
        workerId: String,
        errorMessage: String,
    ): Mono<Long> =
        dbClient
            .sql(
                """
                UPDATE import_batch
                SET
                    status = CASE
                        WHEN status = 'RUNNING' THEN 'QUEUED'
                        ELSE 'UNDO_QUEUED'
                    END,
                    error_message = :errorMessage,
                    lease_expires_at = NULL,
                    worker_id = NULL,
                    updated_at = NOW()
                WHERE id = :batchId AND status IN ('RUNNING', 'UNDO_RUNNING') AND worker_id = :workerId
                """,
            ).bind("batchId", batchId)
            .bind("workerId", workerId)
            .bind("errorMessage", errorMessage)
            .fetch()
            .rowsUpdated()

    override fun markFailed(
        batchId: UUID,
        workerId: String,
        errorMessage: String,
        finishedAt: OffsetDateTime,
    ): Mono<Long> =
        dbClient
            .sql(
                """
                UPDATE import_batch
                SET
                    status = CASE
                        WHEN status = 'RUNNING' THEN 'FAILED'
                        ELSE 'UNDO_FAILED'
                    END,
                    request_payload = CASE WHEN status = 'RUNNING' THEN NULL ELSE request_payload END,
                    error_message = :errorMessage,
                    lease_expires_at = NULL,
                    worker_id = NULL,
                    finished_at = :finishedAt,
                    updated_at = NOW()
                WHERE id = :batchId AND status IN ('RUNNING', 'UNDO_RUNNING') AND worker_id = :workerId
                """,
            ).bind("batchId", batchId)
            .bind("workerId", workerId)
            .bind("errorMessage", errorMessage)
            .bind("finishedAt", finishedAt)
            .fetch()
            .rowsUpdated()

    override fun recoverExpiredLeases(
        now: OffsetDateTime,
        maxRetries: Int,
    ): Flux<UUID> =
        dbClient
            .sql(
                """
                UPDATE import_batch
                SET
                    status = CASE
                        WHEN status = 'RUNNING' AND retries >= :maxRetries THEN 'FAILED'
                        WHEN status = 'UNDO_RUNNING' AND retries >= :maxRetries THEN 'UNDO_FAILED'
                        WHEN status = 'RUNNING' THEN 'QUEUED'
                        ELSE 'UNDO_QUEUED'
                    END,
                    error_message = CASE
                        WHEN status = 'UNDO_RUNNING' AND retries >= :maxRetries THEN 'A importação não pôde ser desfeita após várias tentativas.'
                        WHEN status = 'UNDO_RUNNING' THEN 'O desfazer anterior foi interrompido e será tentado novamente.'
                        WHEN retries >= :maxRetries THEN 'A importação não pôde ser concluída após várias tentativas.'
                        ELSE 'A execução anterior foi interrompida e será tentada novamente.'
                    END,
                    request_payload = CASE
                        WHEN status = 'RUNNING' AND retries >= :maxRetries THEN NULL
                        ELSE request_payload
                    END,
                    lease_expires_at = NULL,
                    worker_id = NULL,
                    finished_at = CASE WHEN retries >= :maxRetries THEN :now ELSE NULL END,
                    updated_at = NOW()
                WHERE status IN ('RUNNING', 'UNDO_RUNNING') AND lease_expires_at IS NOT NULL AND lease_expires_at < :now
                RETURNING id
                """,
            ).bind("now", now)
            .bind("maxRetries", maxRetries)
            .map { row, _ -> row.get("id", UUID::class.java)!! }
            .all()

    override fun findUsersReadyForDispatch(limit: Int): Flux<UUID> =
        dbClient
            .sql(
                """
                SELECT DISTINCT queued.user_id
                FROM import_batch queued
                WHERE queued.status IN ('QUEUED', 'UNDO_QUEUED')
                  AND NOT EXISTS (
                      SELECT 1 FROM import_batch running
                      WHERE running.user_id = queued.user_id AND running.status IN ('RUNNING', 'UNDO_RUNNING')
                  )
                ORDER BY queued.user_id
                LIMIT :limit
                """,
            ).bind("limit", limit)
            .map { row, _ -> row.get("user_id", UUID::class.java)!! }
            .all()

    override fun findOldestQueuedBatchId(userId: UUID): Mono<UUID> =
        dbClient
            .sql(
                """
                SELECT id FROM import_batch
                WHERE user_id = :userId AND status IN ('QUEUED', 'UNDO_QUEUED')
                ORDER BY created_at ASC, id ASC
                LIMIT 1
                """,
            ).bind("userId", userId)
            .map { row, _ -> row.get("id", UUID::class.java)!! }
            .one()
}

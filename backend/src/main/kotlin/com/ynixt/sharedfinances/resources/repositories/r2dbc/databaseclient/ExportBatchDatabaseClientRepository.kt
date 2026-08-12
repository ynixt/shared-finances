package com.ynixt.sharedfinances.resources.repositories.r2dbc.databaseclient

import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.OffsetDateTime
import java.util.UUID

interface ExportBatchDispatchRepository {
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
        rowCount: Int,
        fileKey: String,
        finishedAt: OffsetDateTime,
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

    fun recordFirstDownload(
        batchId: UUID,
        userId: UUID,
        downloadedAt: OffsetDateTime,
    ): Mono<OffsetDateTime>

    fun deleteCompleted(
        batchId: UUID,
        userId: UUID,
    ): Mono<Long>
}

@Repository
class ExportBatchDatabaseClientRepository(
    private val dbClient: DatabaseClient,
) : ExportBatchDispatchRepository {
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
                    SELECT pg_try_advisory_xact_lock(hashtextextended(CAST(:lockKey AS TEXT), 0)) AS locked
                ), next_batch AS (
                    SELECT id FROM export_batch
                    WHERE user_id = :userId AND status = 'QUEUED'
                    ORDER BY created_at ASC, id ASC LIMIT 1
                )
                UPDATE export_batch batch
                SET status = 'RUNNING', started_at = COALESCE(started_at, :now),
                    lease_expires_at = :leaseExpiresAt, worker_id = :workerId,
                    retries = retries + 1, error_message = NULL, updated_at = NOW()
                WHERE (SELECT locked FROM user_lock)
                  AND batch.id = (SELECT id FROM next_batch)
                  AND batch.status = 'QUEUED'
                  AND NOT EXISTS (
                      SELECT 1 FROM export_batch running
                      WHERE running.user_id = :userId AND running.status = 'RUNNING'
                  )
                RETURNING batch.id
                """.trimIndent(),
            ).bind("lockKey", "export-user:$userId")
            .bind("userId", userId)
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
                "UPDATE export_batch SET lease_expires_at = :leaseExpiresAt, updated_at = NOW() " +
                    "WHERE id = :batchId AND status = 'RUNNING' AND worker_id = :workerId",
            ).bind("batchId", batchId)
            .bind("workerId", workerId)
            .bind("leaseExpiresAt", leaseExpiresAt)
            .fetch()
            .rowsUpdated()

    override fun markCompleted(
        batchId: UUID,
        workerId: String,
        rowCount: Int,
        fileKey: String,
        finishedAt: OffsetDateTime,
    ): Mono<Long> =
        dbClient
            .sql(
                """
                WITH completed AS (
                    UPDATE export_batch
                    SET status = 'COMPLETED', row_count = :rowCount, file_key = :fileKey,
                        finished_at = :finishedAt, counted_at = COALESCE(counted_at, :finishedAt),
                        lease_expires_at = NULL, worker_id = NULL, error_message = NULL, updated_at = NOW()
                    WHERE id = :batchId AND status = 'RUNNING' AND worker_id = :workerId
                    RETURNING user_id
                )
                INSERT INTO plan_quota_monthly_usage (user_id, quota, month_start, usage)
                SELECT user_id, 'EXPORTS_PER_MONTH',
                       (date_trunc('month', :finishedAt AT TIME ZONE 'UTC'))::date, 1
                FROM completed
                ON CONFLICT (user_id, quota)
                DO UPDATE SET
                    month_start = EXCLUDED.month_start,
                    usage = CASE
                        WHEN plan_quota_monthly_usage.month_start = EXCLUDED.month_start
                            THEN plan_quota_monthly_usage.usage + 1
                        ELSE 1
                    END,
                    updated_at = NOW()
                RETURNING 1 AS updated
                """.trimIndent(),
            ).bind("batchId", batchId)
            .bind("workerId", workerId)
            .bind("rowCount", rowCount)
            .bind("fileKey", fileKey)
            .bind("finishedAt", finishedAt)
            .map { row, _ -> row.get("updated", Int::class.javaObjectType)!!.toLong() }
            .one()
            .defaultIfEmpty(0L)

    override fun markQueuedForRetry(
        batchId: UUID,
        workerId: String,
        errorMessage: String,
    ): Mono<Long> =
        dbClient
            .sql(
                """
                UPDATE export_batch SET status = 'QUEUED', error_message = :errorMessage,
                    lease_expires_at = NULL, worker_id = NULL, updated_at = NOW()
                WHERE id = :batchId AND status = 'RUNNING' AND worker_id = :workerId
                """.trimIndent(),
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
                UPDATE export_batch SET status = 'FAILED', error_message = :errorMessage,
                    lease_expires_at = NULL, worker_id = NULL, finished_at = :finishedAt, updated_at = NOW()
                WHERE id = :batchId AND status = 'RUNNING' AND worker_id = :workerId
                """.trimIndent(),
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
                UPDATE export_batch
                SET status = CASE WHEN retries >= :maxRetries THEN 'FAILED' ELSE 'QUEUED' END,
                    error_message = CASE WHEN retries >= :maxRetries
                        THEN 'A exportação não pôde ser concluída após várias tentativas.'
                        ELSE 'A execução anterior foi interrompida e será tentada novamente.' END,
                    lease_expires_at = NULL, worker_id = NULL,
                    finished_at = CASE WHEN retries >= :maxRetries THEN :now ELSE NULL END,
                    updated_at = NOW()
                WHERE status = 'RUNNING' AND lease_expires_at IS NOT NULL AND lease_expires_at < :now
                RETURNING id
                """.trimIndent(),
            ).bind("now", now)
            .bind("maxRetries", maxRetries)
            .map { row, _ -> row.get("id", UUID::class.java)!! }
            .all()

    override fun findUsersReadyForDispatch(limit: Int): Flux<UUID> =
        dbClient
            .sql(
                """
                SELECT DISTINCT queued.user_id FROM export_batch queued
                WHERE queued.status = 'QUEUED' AND NOT EXISTS (
                    SELECT 1 FROM export_batch running
                    WHERE running.user_id = queued.user_id AND running.status = 'RUNNING'
                ) ORDER BY queued.user_id LIMIT :limit
                """.trimIndent(),
            ).bind("limit", limit)
            .map { row, _ -> row.get("user_id", UUID::class.java)!! }
            .all()

    override fun findOldestQueuedBatchId(userId: UUID): Mono<UUID> =
        dbClient
            .sql(
                "SELECT id FROM export_batch WHERE user_id = :userId AND status = 'QUEUED' " +
                    "ORDER BY created_at ASC, id ASC LIMIT 1",
            ).bind("userId", userId)
            .map { row, _ -> row.get("id", UUID::class.java)!! }
            .one()

    override fun recordFirstDownload(
        batchId: UUID,
        userId: UUID,
        downloadedAt: OffsetDateTime,
    ): Mono<OffsetDateTime> =
        dbClient
            .sql(
                "UPDATE export_batch SET first_downloaded_at = COALESCE(first_downloaded_at, :downloadedAt), updated_at = NOW() " +
                    "WHERE id = :batchId AND user_id = :userId AND status = 'COMPLETED' AND file_deleted_at IS NULL " +
                    "RETURNING first_downloaded_at",
            ).bind("batchId", batchId)
            .bind("userId", userId)
            .bind("downloadedAt", downloadedAt)
            .map { row, _ -> row.get("first_downloaded_at", OffsetDateTime::class.java)!! }
            .one()

    override fun deleteCompleted(
        batchId: UUID,
        userId: UUID,
    ): Mono<Long> =
        dbClient
            .sql(
                "DELETE FROM export_batch WHERE id = :batchId AND user_id = :userId AND status = 'COMPLETED'",
            ).bind("batchId", batchId)
            .bind("userId", userId)
            .fetch()
            .rowsUpdated()
}

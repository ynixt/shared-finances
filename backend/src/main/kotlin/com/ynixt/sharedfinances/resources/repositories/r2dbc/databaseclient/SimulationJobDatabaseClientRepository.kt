package com.ynixt.sharedfinances.resources.repositories.r2dbc.databaseclient

import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.OffsetDateTime
import java.util.UUID

data class UserLinkedSimulationJobDispatchScope(
    val ownerUserId: UUID?,
    val ownerGroupId: UUID?,
)

@Repository
class SimulationJobDatabaseClientRepository(
    private val dbClient: DatabaseClient,
) {
    fun findDispatchScopesForPendingJobsLinkedToUser(userId: UUID): Flux<UserLinkedSimulationJobDispatchScope> =
        dbClient
            .sql(
                """
                SELECT DISTINCT owner_user_id, owner_group_id
                FROM simulation_job
                WHERE
                    (owner_user_id = :userId OR requested_by_user_id = :userId)
                    AND status IN ('QUEUED', 'RUNNING')
                """,
            ).bind("userId", userId)
            .map { row, _ ->
                UserLinkedSimulationJobDispatchScope(
                    ownerUserId = row.get("owner_user_id", UUID::class.java),
                    ownerGroupId = row.get("owner_group_id", UUID::class.java),
                )
            }.all()

    fun promoteOldestQueuedUserJobToRunning(
        ownerUserId: UUID,
        workerId: String,
        now: OffsetDateTime,
        leaseExpiresAt: OffsetDateTime,
    ): Mono<UUID> =
        dbClient
            .sql(
                """
                WITH owner_lock AS (
                    SELECT pg_try_advisory_xact_lock(hashtextextended(CAST(:ownerLockKey AS TEXT), 0)) AS locked
                ),
                next_job AS (
                    SELECT id
                    FROM simulation_job
                    WHERE owner_user_id = :ownerUserId
                    AND owner_group_id IS NULL
                    AND status = 'QUEUED'
                    ORDER BY created_at ASC, id ASC
                    LIMIT 1
                ),
                running_exists AS (
                    SELECT 1
                    FROM simulation_job
                    WHERE owner_user_id = :ownerUserId
                      AND owner_group_id IS NULL
                      AND status = 'RUNNING'
                    LIMIT 1
                )
                UPDATE simulation_job sj
                SET
                    status = 'RUNNING',
                    started_at = COALESCE(started_at, :now),
                    lease_expires_at = :leaseExpiresAt,
                    worker_id = :workerId,
                    retries = retries + 1,
                    updated_at = NOW()
                WHERE
                    (SELECT locked FROM owner_lock)
                    AND
                    sj.id = (SELECT id FROM next_job)
                    AND sj.status = 'QUEUED'
                    AND NOT EXISTS (SELECT 1 FROM running_exists)
                RETURNING sj.id
                """,
            ).bind("ownerUserId", ownerUserId)
            .bind("ownerLockKey", "user:$ownerUserId")
            .bind("workerId", workerId)
            .bind("now", now)
            .bind("leaseExpiresAt", leaseExpiresAt)
            .map { row, _ -> row.get("id", UUID::class.java)!! }
            .one()

    fun promoteOldestQueuedGroupJobToRunning(
        ownerGroupId: UUID,
        workerId: String,
        now: OffsetDateTime,
        leaseExpiresAt: OffsetDateTime,
    ): Mono<UUID> =
        dbClient
            .sql(
                """
                WITH owner_lock AS (
                    SELECT pg_try_advisory_xact_lock(hashtextextended(CAST(:ownerLockKey AS TEXT), 0)) AS locked
                ),
                next_job AS (
                    SELECT id
                    FROM simulation_job
                    WHERE owner_group_id = :ownerGroupId
                    AND owner_user_id IS NULL
                    AND status = 'QUEUED'
                    ORDER BY created_at ASC, id ASC
                    LIMIT 1
                ),
                running_exists AS (
                    SELECT 1
                    FROM simulation_job
                    WHERE owner_group_id = :ownerGroupId
                      AND owner_user_id IS NULL
                      AND status = 'RUNNING'
                    LIMIT 1
                )
                UPDATE simulation_job sj
                SET
                    status = 'RUNNING',
                    started_at = COALESCE(started_at, :now),
                    lease_expires_at = :leaseExpiresAt,
                    worker_id = :workerId,
                    retries = retries + 1,
                    updated_at = NOW()
                WHERE
                    (SELECT locked FROM owner_lock)
                    AND
                    sj.id = (SELECT id FROM next_job)
                    AND sj.status = 'QUEUED'
                    AND NOT EXISTS (SELECT 1 FROM running_exists)
                RETURNING sj.id
                """,
            ).bind("ownerGroupId", ownerGroupId)
            .bind("ownerLockKey", "group:$ownerGroupId")
            .bind("workerId", workerId)
            .bind("now", now)
            .bind("leaseExpiresAt", leaseExpiresAt)
            .map { row, _ -> row.get("id", UUID::class.java)!! }
            .one()

    fun findOldestQueuedUserJobId(ownerUserId: UUID): Mono<UUID> =
        dbClient
            .sql(
                """
                SELECT id
                FROM simulation_job
                WHERE owner_user_id = :ownerUserId
                  AND owner_group_id IS NULL
                  AND status = 'QUEUED'
                ORDER BY created_at ASC, id ASC
                LIMIT 1
                """,
            ).bind("ownerUserId", ownerUserId)
            .map { row, _ -> row.get("id", UUID::class.java)!! }
            .one()

    fun findOldestQueuedGroupJobId(ownerGroupId: UUID): Mono<UUID> =
        dbClient
            .sql(
                """
                SELECT id
                FROM simulation_job
                WHERE owner_group_id = :ownerGroupId
                  AND owner_user_id IS NULL
                  AND status = 'QUEUED'
                ORDER BY created_at ASC, id ASC
                LIMIT 1
                """,
            ).bind("ownerGroupId", ownerGroupId)
            .map { row, _ -> row.get("id", UUID::class.java)!! }
            .one()

    fun findUserOwnersReadyForDispatch(limit: Int): Flux<UUID> =
        dbClient
            .sql(
                """
                SELECT DISTINCT q.owner_user_id
                FROM simulation_job q
                WHERE
                    q.owner_user_id IS NOT NULL
                    AND q.owner_group_id IS NULL
                    AND
                    q.status = 'QUEUED'
                    AND NOT EXISTS (
                        SELECT 1
                        FROM simulation_job r
                        WHERE
                            r.owner_user_id = q.owner_user_id
                            AND r.owner_group_id IS NULL
                            AND r.status = 'RUNNING'
                    )
                ORDER BY q.owner_user_id
                LIMIT :limit
                """,
            ).bind("limit", limit)
            .map { row, _ -> row.get("owner_user_id", UUID::class.java)!! }
            .all()

    fun findGroupOwnersReadyForDispatch(limit: Int): Flux<UUID> =
        dbClient
            .sql(
                """
                SELECT DISTINCT q.owner_group_id
                FROM simulation_job q
                WHERE
                    q.owner_group_id IS NOT NULL
                    AND q.owner_user_id IS NULL
                    AND
                    q.status = 'QUEUED'
                    AND NOT EXISTS (
                        SELECT 1
                        FROM simulation_job r
                        WHERE
                            r.owner_group_id = q.owner_group_id
                            AND r.owner_user_id IS NULL
                            AND r.status = 'RUNNING'
                    )
                ORDER BY q.owner_group_id
                LIMIT :limit
                """,
            ).bind("limit", limit)
            .map { row, _ -> row.get("owner_group_id", UUID::class.java)!! }
            .all()

    fun renewLease(
        jobId: UUID,
        workerId: String,
        leaseExpiresAt: OffsetDateTime,
    ): Mono<Long> =
        dbClient
            .sql(
                """
                UPDATE simulation_job
                SET
                    lease_expires_at = :leaseExpiresAt,
                    updated_at = NOW()
                WHERE
                    id = :jobId
                    AND status = 'RUNNING'
                    AND worker_id = :workerId
                """,
            ).bind("jobId", jobId)
            .bind("workerId", workerId)
            .bind("leaseExpiresAt", leaseExpiresAt)
            .fetch()
            .rowsUpdated()

    fun markCompleted(
        jobId: UUID,
        workerId: String,
        resultPayload: String?,
        finishedAt: OffsetDateTime,
    ): Mono<Long> =
        run {
            var spec =
                dbClient
                    .sql(
                        """
                        UPDATE simulation_job
                        SET
                            status = 'COMPLETED',
                            result_payload = :resultPayload,
                            error_message = NULL,
                            lease_expires_at = NULL,
                            worker_id = NULL,
                            finished_at = :finishedAt,
                            updated_at = NOW()
                        WHERE
                            id = :jobId
                            AND status = 'RUNNING'
                            AND worker_id = :workerId
                        """,
                    ).bind("jobId", jobId)
                    .bind("workerId", workerId)
                    .bind("finishedAt", finishedAt)

            spec =
                if (resultPayload == null) {
                    spec.bindNull("resultPayload", String::class.java)
                } else {
                    spec.bind("resultPayload", resultPayload)
                }

            spec.fetch().rowsUpdated()
        }

    fun markFailed(
        jobId: UUID,
        workerId: String,
        errorMessage: String,
        finishedAt: OffsetDateTime,
    ): Mono<Long> =
        dbClient
            .sql(
                """
                UPDATE simulation_job
                SET
                    status = 'FAILED',
                    error_message = :errorMessage,
                    lease_expires_at = NULL,
                    worker_id = NULL,
                    finished_at = :finishedAt,
                    updated_at = NOW()
                WHERE
                    id = :jobId
                    AND status = 'RUNNING'
                    AND worker_id = :workerId
                """,
            ).bind("jobId", jobId)
            .bind("workerId", workerId)
            .bind("errorMessage", errorMessage)
            .bind("finishedAt", finishedAt)
            .fetch()
            .rowsUpdated()
}

package com.ynixt.sharedfinances.resources.repositories.r2dbc.databaseclient

import com.ynixt.sharedfinances.domain.models.exports.ExportPurgeCandidate
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import java.time.OffsetDateTime
import java.util.UUID

@Repository
class ExportPurgeDatabaseClientRepository(
    private val dbClient: DatabaseClient,
) : ExportPurgeRepository {
    override fun findDownloadedBefore(cutoff: OffsetDateTime): Flux<ExportPurgeCandidate> = findCandidates("first_downloaded_at", cutoff)

    override fun findCompletedBefore(cutoff: OffsetDateTime): Flux<ExportPurgeCandidate> = findCandidates("finished_at", cutoff)

    override fun deleteAll(ids: Collection<UUID>): Flux<UUID> {
        require(ids.isNotEmpty()) { "At least one export batch id is required" }
        return dbClient
            .sql(
                """
                DELETE FROM export_batch
                WHERE id = ANY(CAST(:ids AS UUID[])) AND status = 'COMPLETED'
                RETURNING id
                """.trimIndent(),
            ).bind(
                "ids",
                ids.toTypedArray(),
            ).map { row, _ -> row.get("id", UUID::class.java)!! }
            .all()
    }

    private fun findCandidates(
        column: String,
        cutoff: OffsetDateTime,
    ): Flux<ExportPurgeCandidate> =
        dbClient
            .sql(
                """
                SELECT id, user_id, file_key FROM export_batch
                WHERE status = 'COMPLETED' AND file_deleted_at IS NULL AND file_key IS NOT NULL
                  AND $column IS NOT NULL AND $column < :cutoff
                ORDER BY $column ASC
                LIMIT 500
                """.trimIndent(),
            ).bind(
                "cutoff",
                cutoff,
            ).map { row, _ ->
                ExportPurgeCandidate(
                    batchId = row.get("id", UUID::class.java)!!,
                    userId = row.get("user_id", UUID::class.java)!!,
                    fileKey = row.get("file_key", String::class.java)!!,
                )
            }.all()
}

interface ExportPurgeRepository {
    fun findDownloadedBefore(cutoff: OffsetDateTime): Flux<ExportPurgeCandidate>

    fun findCompletedBefore(cutoff: OffsetDateTime): Flux<ExportPurgeCandidate>

    fun deleteAll(ids: Collection<UUID>): Flux<UUID>
}

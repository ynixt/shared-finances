package com.ynixt.sharedfinances.resources.repositories

import com.ynixt.sharedfinances.domain.enums.PlanLimitKey
import com.ynixt.sharedfinances.resources.repositories.r2dbc.databaseclient.ExportBatchDatabaseClientRepository
import com.ynixt.sharedfinances.resources.repositories.r2dbc.databaseclient.PlanQuotaUsageDatabaseClientRepository
import com.ynixt.sharedfinances.support.IntegrationTestContainers
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.r2dbc.test.autoconfigure.DataR2dbcTest
import org.springframework.context.annotation.Import
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.test.context.ActiveProfiles
import java.time.Instant
import java.time.OffsetDateTime
import java.util.UUID

@DataR2dbcTest
@ActiveProfiles("test")
@Import(ExportBatchDatabaseClientRepository::class, PlanQuotaUsageDatabaseClientRepository::class)
class ExportBatchQuotaDataR2dbcTest : IntegrationTestContainers() {
    @Autowired
    private lateinit var dbClient: DatabaseClient

    @Autowired
    private lateinit var batchRepository: ExportBatchDatabaseClientRepository

    @Autowired
    private lateinit var usageRepository: PlanQuotaUsageDatabaseClientRepository

    @Test
    fun `batch deletion preserves aggregate usage and a new month resets the bounded counter`() =
        runBlocking {
            val userId = UUID.randomUUID()
            seedUser(userId)

            val augustBatch = seedRunningBatch(userId, "worker-august")
            complete(augustBatch, "worker-august", "2026-08-12T12:00:00Z")
            assertThat(aggregate()).isEqualTo("2026-08-01:1")
            assertThat(usage(userId, "2026-08-01T00:00:00Z")).isEqualTo(1)

            assertThat(batchRepository.deleteCompleted(augustBatch, userId).awaitSingle()).isEqualTo(1)
            assertThat(countBatches()).isZero()
            assertThat(usage(userId, "2026-08-01T00:00:00Z")).isEqualTo(1)

            val septemberBatch = seedRunningBatch(userId, "worker-september")
            complete(septemberBatch, "worker-september", "2026-09-02T12:00:00Z")
            assertThat(usage(userId, "2026-09-01T00:00:00Z")).isEqualTo(1)
            assertThat(countUsageRows()).isEqualTo(1)
            Unit
        }

    private suspend fun complete(
        batchId: UUID,
        workerId: String,
        finishedAt: String,
    ) {
        assertThat(
            batchRepository
                .markCompleted(batchId, workerId, 1, "exports/$batchId.csv", OffsetDateTime.parse(finishedAt))
                .awaitSingle(),
        ).isEqualTo(1)
    }

    private suspend fun usage(
        userId: UUID,
        monthStart: String,
    ): Long = usageRepository.countUsage(userId, PlanLimitKey.EXPORTS_PER_MONTH, Instant.parse(monthStart))

    private suspend fun seedRunningBatch(
        userId: UUID,
        workerId: String,
    ): UUID {
        val batchId = UUID.randomUUID()
        exec(
            """
            INSERT INTO export_batch(id, user_id, status, format, filter_payload, worker_id)
            VALUES ('$batchId', '$userId', 'RUNNING', 'CSV', '{}', '$workerId')
            """.trimIndent(),
        )
        return batchId
    }

    private suspend fun seedUser(userId: UUID) {
        exec(
            """
            INSERT INTO users(id, email, password_hash, first_name, last_name, lang, tmz, default_currency, email_verified, mfa_enabled, role)
            VALUES ('$userId', 'export-quota-$userId@example.com', 'hash', 'Test', 'User', 'en', 'UTC', 'BRL', true, false, 'PRO')
            """.trimIndent(),
        )
    }

    private suspend fun countBatches(): Long = scalar("SELECT COUNT(*) AS value FROM export_batch")

    private suspend fun countUsageRows(): Long = scalar("SELECT COUNT(*) AS value FROM plan_quota_monthly_usage")

    private suspend fun aggregate(): String =
        dbClient
            .sql("SELECT month_start, usage FROM plan_quota_monthly_usage")
            .map { row, _ -> "${row.get("month_start")}:${row.get("usage")}" }
            .one()
            .awaitSingle()

    private suspend fun scalar(sql: String): Long =
        dbClient
            .sql(sql)
            .map { row, _ -> row.get("value", Long::class.javaObjectType)!! }
            .one()
            .awaitSingle()

    private suspend fun exec(sql: String) {
        dbClient
            .sql(sql)
            .fetch()
            .rowsUpdated()
            .awaitSingle()
    }
}

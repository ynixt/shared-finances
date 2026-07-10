package com.ynixt.sharedfinances.resources.repositories

import com.ynixt.sharedfinances.support.IntegrationTestContainers
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.flywaydb.core.Flyway
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.r2dbc.test.autoconfigure.DataR2dbcTest
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.test.context.ActiveProfiles
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

@DataR2dbcTest
@ActiveProfiles("test")
class RebuildGroupDebtChargeMonthFromCreditCardBillMigrationIntegrationTest : IntegrationTestContainers() {
    @Autowired
    private lateinit var dbClient: DatabaseClient

    @Test
    fun `V52 should skip orphan settlement reversals that no longer reference original fragments`() {
        runBlocking {
            flyway.clean()
            flywayFor("51").migrate()

            val groupId = UUID.randomUUID()
            val payerId = UUID.randomUUID()
            val receiverId = UUID.randomUUID()
            val actorId = UUID.randomUUID()
            val orphanReversalId = UUID.randomUUID()

            seedUser(payerId)
            seedUser(receiverId)
            seedUser(actorId)
            seedGroup(groupId)

            exec(
                """
                INSERT INTO group_member_debt_movement(
                    id,
                    group_id,
                    payer_id,
                    receiver_id,
                    month,
                    currency,
                    delta_signed,
                    reason_kind,
                    created_by_user_id,
                    note,
                    source_wallet_event_id,
                    source_movement_id
                ) VALUES
                    ('$orphanReversalId', '$groupId', '$payerId', '$receiverId', DATE '2026-04-01', 'BRL', 0.03, 'DEBT_SETTLEMENT_REVERSAL', '$actorId', NULL, NULL, NULL)
                """.trimIndent(),
            )

            flywayFor("52").migrate()

            val movementCount =
                dbClient
                    .sql("SELECT COUNT(*) AS qty FROM group_member_debt_movement WHERE group_id = :groupId")
                    .bind("groupId", groupId)
                    .map { row, _ -> row.get("qty", java.lang.Long::class.java)?.toLong() ?: 0L }
                    .one()
                    .awaitSingle()

            assertThat(movementCount).isEqualTo(0L)

            val monthlyBalance =
                dbClient
                    .sql(
                        """
                        SELECT balance
                        FROM group_member_debt_monthly
                        WHERE
                            group_id = :groupId
                            AND payer_id = :payerId
                            AND receiver_id = :receiverId
                            AND month = :month
                            AND currency = 'BRL'
                        """.trimIndent(),
                    ).bind("groupId", groupId)
                    .bind("payerId", payerId)
                    .bind("receiverId", receiverId)
                    .bind("month", LocalDate.of(2026, 4, 1))
                    .map { row, _ -> row.get("balance", BigDecimal::class.java)!! }
                    .one()
                    .awaitSingleOrNull()

            assertThat(monthlyBalance).isNull()
        }
    }

    private fun flywayFor(targetVersion: String?): Flyway =
        Flyway
            .configure()
            .dataSource(postgres.jdbcUrl, postgres.username, postgres.password)
            .locations("classpath:db/migration")
            .apply {
                if (targetVersion != null) {
                    target(targetVersion)
                }
            }.cleanDisabled(false)
            .load()

    private suspend fun seedUser(userId: UUID) {
        exec(
            """
            INSERT INTO users(id, email, password_hash, first_name, last_name, lang, tmz, default_currency, email_verified, mfa_enabled)
            VALUES ('$userId', 'migration-$userId@example.com', 'hash', 'Test', 'User', 'en', 'UTC', 'BRL', true, false)
            """.trimIndent(),
        )
    }

    private suspend fun seedGroup(groupId: UUID) {
        exec(
            """
            INSERT INTO "group"(id, name)
            VALUES ('$groupId', 'Migration group')
            """.trimIndent(),
        )
    }

    private suspend fun exec(sql: String) {
        dbClient
            .sql(sql)
            .fetch()
            .rowsUpdated()
            .awaitSingle()
    }
}

package com.ynixt.sharedfinances.resources.repositories

import com.ynixt.sharedfinances.resources.repositories.r2dbc.databaseclient.GroupMemberDebtDatabaseClientRepository
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
import java.util.UUID

@DataR2dbcTest
@ActiveProfiles("test")
@Import(GroupMemberDebtDatabaseClientRepository::class)
class GroupMemberDebtDatabaseClientRepositoryDataR2dbcTest : IntegrationTestContainers() {
    @Autowired
    private lateinit var dbClient: DatabaseClient

    @Autowired
    private lateinit var repository: GroupMemberDebtDatabaseClientRepository

    @Test
    fun `should return settled month composition even when monthly snapshot row does not exist`() {
        runBlocking {
            val groupId = UUID.randomUUID()
            val payerId = UUID.randomUUID()
            val receiverId = UUID.randomUUID()
            val actorId = UUID.randomUUID()

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
                    ('${UUID.randomUUID()}', '$groupId', '$payerId', '$receiverId', DATE '2026-04-01', 'BRL', 80.00, 'BENEFICIARY_CHARGE', '$actorId', 'charge', NULL, NULL),
                    ('${UUID.randomUUID()}', '$groupId', '$payerId', '$receiverId', DATE '2026-04-01', 'BRL', -80.00, 'DEBT_SETTLEMENT', '$actorId', 'settlement', NULL, NULL)
                """.trimIndent(),
            )

            val rows = repository.listMonthlyComposition(groupId).collectList().awaitSingle()

            assertThat(rows).hasSize(1)
            val row = rows.single()
            assertThat(row.month.toString()).isEqualTo("2026-04")
            assertThat(row.netAmount).isEqualByComparingTo("0.00")
            assertThat(row.chargeDelta).isEqualByComparingTo("80.00")
            assertThat(row.settlementDelta).isEqualByComparingTo("-80.00")
            assertThat(row.manualAdjustmentDelta).isEqualByComparingTo("0.00")
        }
    }

    private suspend fun seedUser(userId: UUID) {
        exec(
            """
            INSERT INTO users(id, email, password_hash, first_name, last_name, lang, tmz, default_currency, email_verified, mfa_enabled)
            VALUES ('$userId', 'debt-repo-$userId@example.com', 'hash', 'Test', 'User', 'en', 'UTC', 'BRL', true, false)
            """.trimIndent(),
        )
    }

    private suspend fun seedGroup(groupId: UUID) {
        exec(
            """
            INSERT INTO "group"(id, name)
            VALUES ('$groupId', 'Debt repository test')
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

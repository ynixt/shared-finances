package com.ynixt.sharedfinances.resources.repositories

import com.ynixt.sharedfinances.support.IntegrationTestContainers
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.data.r2dbc.test.autoconfigure.DataR2dbcTest
import org.springframework.test.context.ActiveProfiles
import java.sql.Connection
import java.sql.DriverManager
import java.util.UUID

@DataR2dbcTest
@ActiveProfiles("test")
class GroupDebtLedgerCascadeDataJpaTest : IntegrationTestContainers() {
    @Test
    fun `should cascade delete debt rows when referenced user is removed`() {
        val payerId = UUID.randomUUID()
        val receiverId = UUID.randomUUID()
        val actorId = UUID.randomUUID()
        val groupId = UUID.randomUUID()
        val walletEventId = UUID.randomUUID()
        val monthlyId = UUID.randomUUID()
        val movementId = UUID.randomUUID()

        openConnection().use { connection ->
            seedDebtLedgerFixture(
                connection = connection,
                payerId = payerId,
                receiverId = receiverId,
                actorId = actorId,
                groupId = groupId,
                walletEventId = walletEventId,
                monthlyId = monthlyId,
                movementId = movementId,
            )

            connection.exec("DELETE FROM users WHERE id = '$receiverId'")

            assertThat(connection.count("SELECT COUNT(*) FROM group_member_debt_monthly WHERE id = '$monthlyId'")).isZero()
            assertThat(connection.count("SELECT COUNT(*) FROM group_member_debt_movement WHERE id = '$movementId'")).isZero()
        }
    }

    @Test
    fun `should cascade delete debt rows when group is removed`() {
        val payerId = UUID.randomUUID()
        val receiverId = UUID.randomUUID()
        val actorId = UUID.randomUUID()
        val groupId = UUID.randomUUID()
        val walletEventId = UUID.randomUUID()
        val monthlyId = UUID.randomUUID()
        val movementId = UUID.randomUUID()

        openConnection().use { connection ->
            seedDebtLedgerFixture(
                connection = connection,
                payerId = payerId,
                receiverId = receiverId,
                actorId = actorId,
                groupId = groupId,
                walletEventId = walletEventId,
                monthlyId = monthlyId,
                movementId = movementId,
            )

            connection.exec("""DELETE FROM "group" WHERE id = '$groupId'""")

            assertThat(connection.count("SELECT COUNT(*) FROM group_member_debt_monthly WHERE id = '$monthlyId'")).isZero()
            assertThat(connection.count("SELECT COUNT(*) FROM group_member_debt_movement WHERE id = '$movementId'")).isZero()
        }
    }

    private fun seedDebtLedgerFixture(
        connection: Connection,
        payerId: UUID,
        receiverId: UUID,
        actorId: UUID,
        groupId: UUID,
        walletEventId: UUID,
        monthlyId: UUID,
        movementId: UUID,
    ) {
        connection.autoCommit = false

        listOf(payerId, receiverId, actorId).forEach { userId ->
            connection.exec(
                """
                INSERT INTO users(id, email, password_hash, first_name, last_name, lang, tmz, default_currency, email_verified, mfa_enabled)
                VALUES ('$userId', 'cascade-$userId@example.com', 'hash', 'Test', 'User', 'en', 'UTC', 'BRL', true, false)
                """.trimIndent(),
            )
        }

        connection.exec(
            """
            INSERT INTO "group"(id, name)
            VALUES ('$groupId', 'Debt cascade test')
            """.trimIndent(),
        )

        connection.exec(
            """
            INSERT INTO wallet_event(id, type, payment_type, name, created_by_user_id, group_id, date, confirmed, installment, recurrence_event_id, initial_balance)
            VALUES ('$walletEventId', 'EXPENSE', 'UNIQUE', 'Debt source', '$actorId', '$groupId', DATE '2026-04-10', true, NULL, NULL, false)
            """.trimIndent(),
        )

        connection.exec(
            """
            INSERT INTO group_member_debt_monthly(id, group_id, payer_id, receiver_id, month, currency, balance)
            VALUES ('$monthlyId', '$groupId', '$payerId', '$receiverId', DATE '2026-04-01', 'BRL', 30.00)
            """.trimIndent(),
        )

        connection.exec(
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
                source_wallet_event_id,
                source_movement_id,
                note
            ) VALUES (
                '$movementId',
                '$groupId',
                '$payerId',
                '$receiverId',
                DATE '2026-04-01',
                'BRL',
                30.00,
                'BENEFICIARY_CHARGE',
                '$actorId',
                '$walletEventId',
                NULL,
                'seed'
            )
            """.trimIndent(),
        )

        connection.commit()
    }

    private fun openConnection(): Connection = DriverManager.getConnection(postgres.jdbcUrl, postgres.username, postgres.password)

    private fun Connection.exec(sql: String) {
        createStatement().use { statement ->
            statement.execute(sql)
        }
    }

    private fun Connection.count(sql: String): Long =
        createStatement().use { statement ->
            statement.executeQuery(sql).use { rs ->
                check(rs.next())
                rs.getLong(1)
            }
        }
}

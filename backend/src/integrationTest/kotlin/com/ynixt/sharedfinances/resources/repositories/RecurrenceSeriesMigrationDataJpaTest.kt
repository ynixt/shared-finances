package com.ynixt.sharedfinances.resources.repositories

import com.ynixt.sharedfinances.support.IntegrationTestContainers
import org.assertj.core.api.Assertions.assertThat
import org.flywaydb.core.Flyway
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.data.r2dbc.test.autoconfigure.DataR2dbcTest
import org.springframework.test.context.ActiveProfiles
import java.sql.Connection
import java.sql.DriverManager
import java.util.UUID

@DataR2dbcTest
@ActiveProfiles("test")
class RecurrenceSeriesMigrationDataJpaTest : IntegrationTestContainers() {
    @BeforeEach
    override fun beforeEach() {
        Flyway
            .configure()
            .cleanDisabled(false)
            .dataSource(postgres.jdbcUrl, postgres.username, postgres.password)
            .locations("classpath:db/migration")
            .target("21")
            .load()
            .run {
                clean()
                migrate()
            }
    }

    @Test
    fun `should backfill recurrence_series and cascade delete from series root`() {
        val userId = UUID.randomUUID()
        val walletItemId = UUID.randomUUID()
        val seriesId = UUID.randomUUID()
        val firstRecurrenceId = UUID.randomUUID()
        val secondRecurrenceId = UUID.randomUUID()
        val walletEventId = UUID.randomUUID()
        val walletEntryId = UUID.randomUUID()
        val firstRecurrenceEntryId = UUID.randomUUID()
        val secondRecurrenceEntryId = UUID.randomUUID()

        openConnection().use { connection ->
            connection.autoCommit = false

            connection.exec(
                """
                INSERT INTO users(id, email, password_hash, first_name, last_name, lang, tmz, default_currency, email_verified, mfa_enabled)
                VALUES ('$userId', 'migration-$userId@example.com', 'hash', 'Test', 'User', 'en', 'UTC', 'BRL', true, false)
                """.trimIndent(),
            )

            connection.exec(
                """
                INSERT INTO wallet_item(id, user_id, type, name, currency, enabled, balance, total_limit, due_day, days_between_due_and_closing, due_on_next_business_day)
                VALUES ('$walletItemId', '$userId', 'BANK_ACCOUNT', 'Main', 'BRL', true, 1000.00, NULL, NULL, NULL, NULL)
                """.trimIndent(),
            )

            connection.exec(
                """
                INSERT INTO recurrence_event(id, type, payment_type, periodicity, name, user_id, qty_executed, qty_limit, last_execution, next_execution, end_execution, series_id, series_installment_total, series_installment_offset)
                VALUES
                    ('$firstRecurrenceId', 'EXPENSE', 'INSTALLMENTS', 'MONTHLY', 'Series root 1', '$userId', 1, 1, DATE '2026-01-08', DATE '2026-02-08', DATE '2026-01-08', '$seriesId', 3, 0),
                    ('$secondRecurrenceId', 'EXPENSE', 'INSTALLMENTS', 'MONTHLY', 'Series root 2', '$userId', 0, 2, NULL, DATE '2026-02-08', DATE '2026-03-08', '$seriesId', 3, 1)
                """.trimIndent(),
            )

            connection.exec(
                """
                INSERT INTO recurrence_entry(id, wallet_event_id, wallet_item_id, value, next_bill_date, last_bill_date)
                VALUES
                    ('$firstRecurrenceEntryId', '$firstRecurrenceId', '$walletItemId', -100.00, NULL, NULL),
                    ('$secondRecurrenceEntryId', '$secondRecurrenceId', '$walletItemId', -100.00, NULL, NULL)
                """.trimIndent(),
            )

            connection.exec(
                """
                INSERT INTO wallet_event(id, type, payment_type, name, user_id, date, confirmed, installment, recurrence_event_id)
                VALUES ('$walletEventId', 'EXPENSE', 'INSTALLMENTS', 'Generated', '$userId', DATE '2026-01-08', true, 1, '$firstRecurrenceId')
                """.trimIndent(),
            )

            connection.exec(
                """
                INSERT INTO wallet_entry(id, wallet_event_id, wallet_item_id, value, bill_id)
                VALUES ('$walletEntryId', '$walletEventId', '$walletItemId', -100.00, NULL)
                """.trimIndent(),
            )

            connection.commit()
        }

        Flyway
            .configure()
            .cleanDisabled(false)
            .dataSource(postgres.jdbcUrl, postgres.username, postgres.password)
            .locations("classpath:db/migration")
            .target("22")
            .load()
            .migrate()

        openConnection().use { connection ->
            val qtyTotal =
                connection.queryInt(
                    "SELECT qty_total FROM recurrence_series WHERE id = '$seriesId'",
                )
            val firstOffset =
                connection.queryInt(
                    "SELECT series_offset FROM recurrence_event WHERE id = '$firstRecurrenceId'",
                )
            val secondOffset =
                connection.queryInt(
                    "SELECT series_offset FROM recurrence_event WHERE id = '$secondRecurrenceId'",
                )

            assertThat(qtyTotal).isEqualTo(3)
            assertThat(firstOffset).isEqualTo(0)
            assertThat(secondOffset).isEqualTo(1)

            connection.exec("DELETE FROM recurrence_series WHERE id = '$seriesId'")

            assertThat(connection.count("SELECT COUNT(*) FROM recurrence_event WHERE series_id = '$seriesId'")).isZero()
            assertThat(
                connection.count(
                    "SELECT COUNT(*) FROM recurrence_entry WHERE wallet_event_id IN ('$firstRecurrenceId', '$secondRecurrenceId')",
                ),
            ).isZero()
            assertThat(
                connection.count(
                    "SELECT COUNT(*) FROM wallet_event WHERE recurrence_event_id IN ('$firstRecurrenceId', '$secondRecurrenceId')",
                ),
            ).isZero()
            assertThat(connection.count("SELECT COUNT(*) FROM wallet_entry WHERE wallet_event_id = '$walletEventId'"))
                .isZero()
        }
    }

    private fun openConnection(): Connection = DriverManager.getConnection(postgres.jdbcUrl, postgres.username, postgres.password)

    private fun Connection.exec(sql: String) {
        createStatement().use { statement ->
            statement.execute(sql)
        }
    }

    private fun Connection.queryInt(sql: String): Int =
        createStatement().use { statement ->
            statement.executeQuery(sql).use { rs ->
                check(rs.next())
                rs.getInt(1)
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

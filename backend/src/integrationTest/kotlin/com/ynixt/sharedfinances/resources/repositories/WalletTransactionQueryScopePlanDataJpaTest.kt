package com.ynixt.sharedfinances.resources.repositories

import com.ynixt.sharedfinances.application.config.SimpleEntityUuidBeforeConvertCallback
import com.ynixt.sharedfinances.resources.repositories.r2dbc.springdata.UserSpringDataRepository
import com.ynixt.sharedfinances.resources.repositories.r2dbc.springdata.WalletItemSpringDataRepository
import com.ynixt.sharedfinances.support.RepositoryDataR2dbcTestSupport
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
@Import(SimpleEntityUuidBeforeConvertCallback::class)
class WalletTransactionQueryScopePlanDataJpaTest : RepositoryDataR2dbcTestSupport() {
    @Autowired
    private lateinit var dbClient: DatabaseClient

    @Autowired
    private lateinit var userRepository: UserSpringDataRepository

    @Autowired
    private lateinit var walletItemRepository: WalletItemSpringDataRepository

    @Test
    fun `should use index-backed plans for main query-scope patterns`() {
        runBlocking {
            val owner = userRepository.save(newUser("owner-${UUID.randomUUID()}@example.com")).awaitSingle()
            val other = userRepository.save(newUser("other-${UUID.randomUUID()}@example.com")).awaitSingle()
            val ownerUserId = requireNotNull(owner.id)
            val otherUserId = requireNotNull(other.id)

            val ownerItem = walletItemRepository.save(newBankAccount(ownerUserId, "Owner item")).awaitSingle()
            val otherItem = walletItemRepository.save(newBankAccount(otherUserId, "Other item")).awaitSingle()
            val ownerItemId = requireNotNull(ownerItem.id)
            val otherItemId = requireNotNull(otherItem.id)
            val groupId = UUID.randomUUID()

            dbClient
                .sql("""INSERT INTO "group" (id, name) VALUES (:id, :name)""")
                .bind("id", groupId)
                .bind("name", "Query Scope Group")
                .fetch()
                .rowsUpdated()
                .awaitSingle()

            seedNoiseWalletItems(otherUserId)

            seedCommittedDataset(
                ownerUserId = ownerUserId,
                otherUserId = otherUserId,
                ownerItemId = ownerItemId,
                otherItemId = otherItemId,
                groupId = groupId,
            )
            seedRecurrenceDataset(
                ownerUserId = ownerUserId,
                otherUserId = otherUserId,
                ownerItemId = ownerItemId,
                otherItemId = otherItemId,
                groupId = groupId,
            )

            val committedUserPlan =
                explainQuery(
                    sql =
                        """
                        SELECT we.id
                        FROM wallet_entry wen
                        INNER JOIN wallet_item wi ON wi.id = wen.wallet_item_id
                        INNER JOIN wallet_event we ON we.id = wen.wallet_event_id
                        WHERE wi.user_id = :ownerUserId
                        GROUP BY we.id, we.date
                        ORDER BY we.date DESC, we.id DESC
                        LIMIT 50
                        """.trimIndent(),
                    bindings = mapOf("ownerUserId" to ownerUserId),
                )

            val committedGroupPlan =
                explainQuery(
                    sql =
                        """
                        SELECT we.id
                        FROM wallet_event we
                        WHERE we.group_id = :groupId
                        ORDER BY we.date DESC, we.id DESC
                        LIMIT 50
                        """.trimIndent(),
                    bindings = mapOf("groupId" to groupId),
                )

            val committedGroupUsersPlan =
                explainQuery(
                    sql =
                        """
                        SELECT we.id
                        FROM wallet_entry wen
                        INNER JOIN wallet_item wi ON wi.id = wen.wallet_item_id
                        INNER JOIN wallet_event we ON we.id = wen.wallet_event_id
                        WHERE wi.user_id = ANY(:ownerUserIds)
                          AND we.group_id = :groupId
                        GROUP BY we.id, we.date
                        ORDER BY we.date DESC, we.id DESC
                        LIMIT 50
                        """.trimIndent(),
                    bindings =
                        mapOf(
                            "ownerUserIds" to arrayOf(ownerUserId, otherUserId),
                            "groupId" to groupId,
                        ),
                )

            val recurrenceUserPlan =
                explainQuery(
                    sql =
                        """
                        SELECT re.id
                        FROM recurrence_entry ren
                        INNER JOIN wallet_item wi ON wi.id = ren.wallet_item_id
                        INNER JOIN recurrence_event re ON re.id = ren.wallet_event_id
                        WHERE wi.user_id = :ownerUserId
                        GROUP BY re.id, re.next_execution
                        ORDER BY re.next_execution DESC NULLS LAST, re.id DESC
                        LIMIT 50
                        """.trimIndent(),
                    bindings = mapOf("ownerUserId" to ownerUserId),
                )

            val recurrenceGroupPlan =
                explainQuery(
                    sql =
                        """
                        SELECT re.id
                        FROM recurrence_event re
                        WHERE re.group_id = :groupId
                        ORDER BY re.next_execution DESC NULLS LAST, re.id DESC
                        LIMIT 50
                        """.trimIndent(),
                    bindings = mapOf("groupId" to groupId),
                )

            assertThat(committedUserPlan)
                .containsAnyOf("idx_wallet_entry_wallet_item_wallet_event_id", "idx_wallet_entry_item_bill")
                .doesNotContain("Seq Scan on wallet_entry")
            assertThat(committedGroupPlan)
                .contains("idx_wallet_event_group_date_id")
                .doesNotContain("Seq Scan on wallet_event")
            assertThat(committedGroupUsersPlan)
                .contains("group_id =")
                .contains("user_id = ANY")

            assertThat(recurrenceUserPlan)
                .containsAnyOf("idx_recurrence_entry_wallet_item_wallet_event_id", "idx_recurrence_entry_event_wallet_item_id")
                .doesNotContain("Seq Scan on recurrence_entry")
            assertThat(recurrenceGroupPlan)
                .contains("idx_recurrence_event_group")
                .doesNotContain("Seq Scan on recurrence_event")
        }
    }

    private suspend fun seedCommittedDataset(
        ownerUserId: UUID,
        otherUserId: UUID,
        ownerItemId: UUID,
        otherItemId: UUID,
        groupId: UUID,
    ) {
        dbClient
            .sql("DELETE FROM wallet_entry")
            .fetch()
            .rowsUpdated()
            .awaitSingle()
        dbClient
            .sql("DELETE FROM wallet_event")
            .fetch()
            .rowsUpdated()
            .awaitSingle()

        dbClient
            .sql(
                """
                INSERT INTO wallet_event (
                    id,
                    type,
                    payment_type,
                    name,
                    category_id,
                    created_by_user_id,
                    group_id,
                    tags,
                    observations,
                    date,
                    confirmed,
                    installment,
                    recurrence_event_id,
                    initial_balance
                )
                SELECT
                    (
                        substr(md5('we' || i::text), 1, 8) || '-' ||
                        substr(md5('we' || i::text), 9, 4) || '-' ||
                        substr(md5('we' || i::text), 13, 4) || '-' ||
                        substr(md5('we' || i::text), 17, 4) || '-' ||
                        substr(md5('we' || i::text), 21, 12)
                    )::uuid,
                    'EXPENSE',
                    'UNIQUE',
                    'Committed event ' || i::text,
                    NULL,
                    CASE WHEN i % 2 = 0 THEN :ownerUserId ELSE :otherUserId END,
                    CASE WHEN i % 3 = 0 THEN :groupId ELSE NULL END,
                    NULL,
                    NULL,
                    DATE '2024-01-01' + (i % 720),
                    TRUE,
                    NULL,
                    NULL,
                    FALSE
                FROM generate_series(1, 6000) AS s(i)
                """.trimIndent(),
            ).bind("ownerUserId", ownerUserId)
            .bind("otherUserId", otherUserId)
            .bind("groupId", groupId)
            .fetch()
            .rowsUpdated()
            .awaitSingle()

        dbClient
            .sql(
                """
                INSERT INTO wallet_entry (
                    id,
                    value,
                    wallet_event_id,
                    wallet_item_id,
                    bill_id
                )
                SELECT
                    (
                        substr(md5('wen' || i::text), 1, 8) || '-' ||
                        substr(md5('wen' || i::text), 9, 4) || '-' ||
                        substr(md5('wen' || i::text), 13, 4) || '-' ||
                        substr(md5('wen' || i::text), 17, 4) || '-' ||
                        substr(md5('wen' || i::text), 21, 12)
                    )::uuid,
                    -10.00,
                    (
                        substr(md5('we' || i::text), 1, 8) || '-' ||
                        substr(md5('we' || i::text), 9, 4) || '-' ||
                        substr(md5('we' || i::text), 13, 4) || '-' ||
                        substr(md5('we' || i::text), 17, 4) || '-' ||
                        substr(md5('we' || i::text), 21, 12)
                    )::uuid,
                    CASE
                        WHEN i % 100 = 0 THEN :ownerItemId
                        ELSE (
                            substr(md5('noise' || ((i % 500) + 1)::text), 1, 8) || '-' ||
                            substr(md5('noise' || ((i % 500) + 1)::text), 9, 4) || '-' ||
                            substr(md5('noise' || ((i % 500) + 1)::text), 13, 4) || '-' ||
                            substr(md5('noise' || ((i % 500) + 1)::text), 17, 4) || '-' ||
                            substr(md5('noise' || ((i % 500) + 1)::text), 21, 12)
                        )::uuid
                    END,
                    NULL
                FROM generate_series(1, 6000) AS s(i)
                """.trimIndent(),
            ).bind("ownerItemId", ownerItemId)
            .fetch()
            .rowsUpdated()
            .awaitSingle()

        dbClient
            .sql("ANALYZE wallet_event")
            .fetch()
            .rowsUpdated()
            .awaitSingle()
        dbClient
            .sql("ANALYZE wallet_entry")
            .fetch()
            .rowsUpdated()
            .awaitSingle()
        dbClient
            .sql("ANALYZE wallet_item")
            .fetch()
            .rowsUpdated()
            .awaitSingle()
    }

    private suspend fun seedRecurrenceDataset(
        ownerUserId: UUID,
        otherUserId: UUID,
        ownerItemId: UUID,
        otherItemId: UUID,
        groupId: UUID,
    ) {
        dbClient
            .sql("DELETE FROM recurrence_entry")
            .fetch()
            .rowsUpdated()
            .awaitSingle()
        dbClient
            .sql("DELETE FROM recurrence_event")
            .fetch()
            .rowsUpdated()
            .awaitSingle()
        dbClient
            .sql("DELETE FROM recurrence_series")
            .fetch()
            .rowsUpdated()
            .awaitSingle()

        dbClient
            .sql(
                """
                INSERT INTO recurrence_series (id, qty_total)
                SELECT
                    (
                        substr(md5('rs' || i::text), 1, 8) || '-' ||
                        substr(md5('rs' || i::text), 9, 4) || '-' ||
                        substr(md5('rs' || i::text), 13, 4) || '-' ||
                        substr(md5('rs' || i::text), 17, 4) || '-' ||
                        substr(md5('rs' || i::text), 21, 12)
                    )::uuid,
                    NULL
                FROM generate_series(1, 6000) AS s(i)
                """.trimIndent(),
            ).fetch()
            .rowsUpdated()
            .awaitSingle()

        dbClient
            .sql(
                """
                INSERT INTO recurrence_event (
                    id,
                    type,
                    payment_type,
                    periodicity,
                    name,
                    category_id,
                    created_by_user_id,
                    group_id,
                    tags,
                    observations,
                    qty_executed,
                    qty_limit,
                    last_execution,
                    next_execution,
                    end_execution,
                    series_id,
                    series_offset,
                    initial_balance
                )
                SELECT
                    (
                        substr(md5('re' || i::text), 1, 8) || '-' ||
                        substr(md5('re' || i::text), 9, 4) || '-' ||
                        substr(md5('re' || i::text), 13, 4) || '-' ||
                        substr(md5('re' || i::text), 17, 4) || '-' ||
                        substr(md5('re' || i::text), 21, 12)
                    )::uuid,
                    'EXPENSE',
                    'RECURRING',
                    'MONTHLY',
                    'Recurrence ' || i::text,
                    NULL,
                    CASE WHEN i % 2 = 0 THEN :ownerUserId ELSE :otherUserId END,
                    CASE WHEN i % 3 = 0 THEN :groupId ELSE NULL END,
                    NULL,
                    NULL,
                    0,
                    NULL,
                    NULL,
                    DATE '2026-01-01' + (i % 180),
                    NULL,
                    (
                        substr(md5('rs' || i::text), 1, 8) || '-' ||
                        substr(md5('rs' || i::text), 9, 4) || '-' ||
                        substr(md5('rs' || i::text), 13, 4) || '-' ||
                        substr(md5('rs' || i::text), 17, 4) || '-' ||
                        substr(md5('rs' || i::text), 21, 12)
                    )::uuid,
                    0,
                    FALSE
                FROM generate_series(1, 6000) AS s(i)
                """.trimIndent(),
            ).bind("ownerUserId", ownerUserId)
            .bind("otherUserId", otherUserId)
            .bind("groupId", groupId)
            .fetch()
            .rowsUpdated()
            .awaitSingle()

        dbClient
            .sql(
                """
                INSERT INTO recurrence_entry (
                    id,
                    wallet_event_id,
                    wallet_item_id,
                    value,
                    next_bill_date,
                    last_bill_date
                )
                SELECT
                    (
                        substr(md5('ren' || i::text), 1, 8) || '-' ||
                        substr(md5('ren' || i::text), 9, 4) || '-' ||
                        substr(md5('ren' || i::text), 13, 4) || '-' ||
                        substr(md5('ren' || i::text), 17, 4) || '-' ||
                        substr(md5('ren' || i::text), 21, 12)
                    )::uuid,
                    (
                        substr(md5('re' || i::text), 1, 8) || '-' ||
                        substr(md5('re' || i::text), 9, 4) || '-' ||
                        substr(md5('re' || i::text), 13, 4) || '-' ||
                        substr(md5('re' || i::text), 17, 4) || '-' ||
                        substr(md5('re' || i::text), 21, 12)
                    )::uuid,
                    CASE
                        WHEN i % 100 = 0 THEN :ownerItemId
                        ELSE (
                            substr(md5('noise' || ((i % 500) + 1)::text), 1, 8) || '-' ||
                            substr(md5('noise' || ((i % 500) + 1)::text), 9, 4) || '-' ||
                            substr(md5('noise' || ((i % 500) + 1)::text), 13, 4) || '-' ||
                            substr(md5('noise' || ((i % 500) + 1)::text), 17, 4) || '-' ||
                            substr(md5('noise' || ((i % 500) + 1)::text), 21, 12)
                        )::uuid
                    END,
                    -15.00,
                    NULL,
                    NULL
                FROM generate_series(1, 6000) AS s(i)
                """.trimIndent(),
            ).bind("ownerItemId", ownerItemId)
            .fetch()
            .rowsUpdated()
            .awaitSingle()

        dbClient
            .sql("ANALYZE recurrence_event")
            .fetch()
            .rowsUpdated()
            .awaitSingle()
        dbClient
            .sql("ANALYZE recurrence_entry")
            .fetch()
            .rowsUpdated()
            .awaitSingle()
    }

    private suspend fun seedNoiseWalletItems(otherUserId: UUID) {
        dbClient
            .sql(
                """
                INSERT INTO wallet_item (
                    id,
                    user_id,
                    type,
                    name,
                    currency,
                    enabled,
                    balance,
                    total_limit,
                    due_day,
                    days_between_due_and_closing,
                    due_on_next_business_day,
                    show_on_dashboard
                )
                SELECT
                    (
                        substr(md5('noise' || i::text), 1, 8) || '-' ||
                        substr(md5('noise' || i::text), 9, 4) || '-' ||
                        substr(md5('noise' || i::text), 13, 4) || '-' ||
                        substr(md5('noise' || i::text), 17, 4) || '-' ||
                        substr(md5('noise' || i::text), 21, 12)
                    )::uuid,
                    :otherUserId,
                    'BANK_ACCOUNT',
                    'Noise item ' || i::text,
                    'BRL',
                    TRUE,
                    0,
                    NULL,
                    NULL,
                    NULL,
                    NULL,
                    FALSE
                FROM generate_series(1, 500) AS s(i)
                ON CONFLICT (id) DO NOTHING
                """.trimIndent(),
            ).bind("otherUserId", otherUserId)
            .fetch()
            .rowsUpdated()
            .awaitSingle()
        dbClient
            .sql("ANALYZE wallet_item")
            .fetch()
            .rowsUpdated()
            .awaitSingle()
    }

    private suspend fun explainQuery(
        sql: String,
        bindings: Map<String, Any>,
    ): String {
        var spec = dbClient.sql("EXPLAIN (COSTS OFF) $sql")
        for ((name, value) in bindings) {
            spec = spec.bind(name, value)
        }
        return spec
            .map { row, _ -> row.get(0, String::class.java)!! }
            .all()
            .collectList()
            .awaitSingle()
            .joinToString("\n")
    }
}

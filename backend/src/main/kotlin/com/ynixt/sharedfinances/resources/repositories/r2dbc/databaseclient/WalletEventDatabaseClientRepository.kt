package com.ynixt.sharedfinances.resources.repositories.r2dbc.databaseclient

import com.ynixt.sharedfinances.domain.entities.wallet.entries.WalletEventEntity
import com.ynixt.sharedfinances.domain.enums.WalletEntryType
import com.ynixt.sharedfinances.domain.repositories.WalletEventCursorFindAll
import com.ynixt.sharedfinances.domain.repositories.WalletTransactionQueryPath
import com.ynixt.sharedfinances.domain.repositories.WalletTransactionQueryScope
import com.ynixt.sharedfinances.resources.repositories.r2dbc.mapping.WalletEntryR2DBCMapping
import com.ynixt.sharedfinances.resources.repositories.r2dbc.mapping.WalletEventR2DBCMapping
import com.ynixt.sharedfinances.resources.repositories.r2dbc.mapping.WalletItemR2DBCMapping
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import java.time.LocalDate
import java.util.UUID

@Repository
class WalletEventDatabaseClientRepository(
    private val dbClient: DatabaseClient,
) : DatabaseClientRepository() {
    fun findAll(
        scope: WalletTransactionQueryScope,
        limit: Int,
        walletItemId: UUID?,
        walletItemIds: Set<UUID>,
        entryTypes: Set<WalletEntryType>,
        categoryConceptIds: Set<UUID>,
        includeUncategorized: Boolean,
        minimumDate: LocalDate?,
        maximumDate: LocalDate?,
        billId: UUID?,
        cursor: WalletEventCursorFindAll?,
    ): Flux<WalletEventEntity> {
        var eventIdsSql =
            when (scope.path) {
                WalletTransactionQueryPath.OWNERSHIP ->
                    buildOwnershipEventIdsSql(
                        scope = scope,
                        walletItemId = walletItemId,
                        walletItemIds = walletItemIds,
                        entryTypes = entryTypes,
                        categoryConceptIds = categoryConceptIds,
                        includeUncategorized = includeUncategorized,
                        minimumDate = minimumDate,
                        maximumDate = maximumDate,
                        billId = billId,
                        cursor = cursor,
                    )
                WalletTransactionQueryPath.GROUP_SCOPE ->
                    buildGroupScopeEventIdsSql(
                        scope = scope,
                        walletItemId = walletItemId,
                        walletItemIds = walletItemIds,
                        entryTypes = entryTypes,
                        categoryConceptIds = categoryConceptIds,
                        includeUncategorized = includeUncategorized,
                        minimumDate = minimumDate,
                        maximumDate = maximumDate,
                        billId = billId,
                        cursor = cursor,
                    )
            }

        eventIdsSql +=
            """
             ORDER BY we.date DESC, we.id DESC
             LIMIT :limit
            """

        var spec = dbClient.sql(eventIdsSql)

        if (scope.ownerUserIds.isNotEmpty()) spec = spec.bind("ownerUserIds", scope.ownerUserIds.toTypedArray())
        if (scope.groupIds.isNotEmpty()) spec = spec.bind("groupIds", scope.groupIds.toTypedArray())
        if (walletItemId != null) spec = spec.bind("walletItemId", walletItemId)
        if (walletItemIds.isNotEmpty()) spec = spec.bind("walletItemIds", walletItemIds.toTypedArray())
        if (entryTypes.isNotEmpty()) spec = spec.bind("entryTypes", entryTypes.map { it.name }.toTypedArray())
        if (categoryConceptIds.isNotEmpty()) spec = spec.bind("categoryConceptIds", categoryConceptIds.toTypedArray())
        if (billId != null) spec = spec.bind("billId", billId)
        if (minimumDate != null) spec = spec.bind("minimumDate", minimumDate)
        if (maximumDate != null) spec = spec.bind("maximumDate", maximumDate)
        if (cursor != null) spec = spec.bind("cursorDate", cursor.maximumDate).bind("cursorId", cursor.maximumId)
        spec = spec.bind("limit", limit)

        return spec
            .map { row, _ -> row.get("id", UUID::class.java)!! }
            .all()
            .collectList()
            .flatMapMany { eventIds ->
                if (eventIds.isEmpty()) return@flatMapMany Flux.empty<WalletEventEntity>()

                // Query 2: the real results, using the ids from query 1
                val fullSql = """
                SELECT
                    we.*,
                    ${WalletEntryR2DBCMapping.createSelectForWalletEntry("wen", "entry_")},
                    ${WalletItemR2DBCMapping.createSelectForWalletItem("wi", "entry_wi_")}
                FROM wallet_event we
                JOIN wallet_entry wen ON wen.wallet_event_id = we.id
                JOIN wallet_item wi ON wi.id = wen.wallet_item_id
                WHERE we.id = ANY(:eventIds)
                ORDER BY we.date DESC, we.id DESC, wen.value ASC
            """

                dbClient
                    .sql(fullSql)
                    .bind("eventIds", eventIds.toTypedArray())
                    .map { row, _ ->
                        val event = WalletEventR2DBCMapping.walletEventFromRow(row, "")
                        val entry = WalletEntryR2DBCMapping.walletEntryFromRow(row, "entry_")
                        entry.walletItem = WalletItemR2DBCMapping.walletItemFromRow(row, "entry_wi_")
                        Pair(event, entry)
                    }.all()
                    .bufferUntilChanged { it.first.id!! }
                    .map { pairs ->
                        val event = pairs.first().first

                        event.entries =
                            pairs.map { it.second }.also {
                                it.forEach { entry ->
                                    entry.event = event
                                }
                            }

                        event
                    }
            }
    }

    private fun buildOwnershipEventIdsSql(
        scope: WalletTransactionQueryScope,
        walletItemId: UUID?,
        walletItemIds: Set<UUID>,
        entryTypes: Set<WalletEntryType>,
        categoryConceptIds: Set<UUID>,
        includeUncategorized: Boolean,
        minimumDate: LocalDate?,
        maximumDate: LocalDate?,
        billId: UUID?,
        cursor: WalletEventCursorFindAll?,
    ): String {
        var sql =
            """
            SELECT we.id
            FROM wallet_entry wen
            INNER JOIN wallet_item wi ON wi.id = wen.wallet_item_id
            INNER JOIN wallet_event we ON we.id = wen.wallet_event_id
            WHERE wi.user_id = ANY(:ownerUserIds)
            """.trimIndent()

        if (scope.groupIds.isNotEmpty()) {
            sql += " AND we.group_id = ANY(:groupIds)"
        }

        if (walletItemId != null) {
            sql += " AND wen.wallet_item_id = :walletItemId"
        }
        if (walletItemIds.isNotEmpty()) {
            sql += " AND wen.wallet_item_id = ANY(:walletItemIds)"
        }
        if (billId != null) {
            sql += " AND wen.bill_id = :billId"
        }
        if (entryTypes.isNotEmpty()) {
            sql += " AND CAST(we.type AS TEXT) = ANY(:entryTypes)"
        }
        sql += buildCategoryPredicateSql(categoryConceptIds = categoryConceptIds, includeUncategorized = includeUncategorized)
        if (minimumDate != null) {
            sql += " AND we.date >= :minimumDate"
        }
        if (maximumDate != null) {
            sql += " AND we.date <= :maximumDate"
        }
        if (cursor != null) {
            sql += " AND (we.date, we.id) < (:cursorDate, :cursorId)"
        }

        sql += " GROUP BY we.id, we.date"
        return sql
    }

    private fun buildGroupScopeEventIdsSql(
        scope: WalletTransactionQueryScope,
        walletItemId: UUID?,
        walletItemIds: Set<UUID>,
        entryTypes: Set<WalletEntryType>,
        categoryConceptIds: Set<UUID>,
        includeUncategorized: Boolean,
        minimumDate: LocalDate?,
        maximumDate: LocalDate?,
        billId: UUID?,
        cursor: WalletEventCursorFindAll?,
    ): String {
        var sql =
            """
            SELECT we.id
            FROM wallet_event we
            WHERE we.group_id = ANY(:groupIds)
            """.trimIndent()

        if (walletItemId != null) {
            sql +=
                " AND EXISTS (SELECT 1 FROM wallet_entry wen WHERE wen.wallet_event_id = we.id AND wen.wallet_item_id = :walletItemId)"
        }
        if (walletItemIds.isNotEmpty()) {
            sql +=
                " AND EXISTS (SELECT 1 FROM wallet_entry wen WHERE wen.wallet_event_id = we.id AND wen.wallet_item_id = ANY(:walletItemIds))"
        }
        if (billId != null) {
            sql +=
                " AND EXISTS (SELECT 1 FROM wallet_entry wen WHERE wen.wallet_event_id = we.id AND wen.bill_id = :billId)"
        }
        if (entryTypes.isNotEmpty()) {
            sql += " AND CAST(we.type AS TEXT) = ANY(:entryTypes)"
        }
        sql += buildCategoryPredicateSql(categoryConceptIds = categoryConceptIds, includeUncategorized = includeUncategorized)
        if (minimumDate != null) {
            sql += " AND we.date >= :minimumDate"
        }
        if (maximumDate != null) {
            sql += " AND we.date <= :maximumDate"
        }
        if (cursor != null) {
            sql += " AND (we.date, we.id) < (:cursorDate, :cursorId)"
        }

        return sql
    }

    private fun buildCategoryPredicateSql(
        categoryConceptIds: Set<UUID>,
        includeUncategorized: Boolean,
    ): String =
        when {
            categoryConceptIds.isEmpty() && !includeUncategorized -> ""
            categoryConceptIds.isEmpty() && includeUncategorized -> " AND we.category_id IS NULL"
            categoryConceptIds.isNotEmpty() && includeUncategorized ->
                """
                AND (
                   we.category_id IS NULL
                   OR EXISTS (
                       SELECT 1
                       FROM wallet_entry_category cat
                       WHERE cat.id = we.category_id
                         AND cat.concept_id = ANY(:categoryConceptIds)
                   )
                )
                """.trimIndent()
            else ->
                """
                AND EXISTS (
                   SELECT 1
                   FROM wallet_entry_category cat
                   WHERE cat.id = we.category_id
                     AND cat.concept_id = ANY(:categoryConceptIds)
                )
                """.trimIndent()
        }
}

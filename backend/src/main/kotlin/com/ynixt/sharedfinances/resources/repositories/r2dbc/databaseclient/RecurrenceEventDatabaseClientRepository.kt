package com.ynixt.sharedfinances.resources.repositories.r2dbc.databaseclient

import com.ynixt.sharedfinances.domain.entities.wallet.entries.RecurrenceEventEntity
import com.ynixt.sharedfinances.domain.entities.wallet.entries.WalletEntryCategoryEntity
import com.ynixt.sharedfinances.domain.enums.WalletEntryType
import com.ynixt.sharedfinances.domain.repositories.WalletTransactionQueryPath
import com.ynixt.sharedfinances.domain.repositories.WalletTransactionQueryScope
import com.ynixt.sharedfinances.resources.repositories.r2dbc.mapping.RecurrenceEntryR2DBCMapping
import com.ynixt.sharedfinances.resources.repositories.r2dbc.mapping.RecurrenceEventR2DBCMapping
import com.ynixt.sharedfinances.resources.repositories.r2dbc.mapping.UserR2DBCMapping
import com.ynixt.sharedfinances.resources.repositories.r2dbc.mapping.WalletItemR2DBCMapping
import org.springframework.data.domain.Sort
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

@Repository
class RecurrenceEventDatabaseClientRepository(
    private val dbClient: DatabaseClient,
) : DatabaseClientRepository() {
    private val validSortColumns =
        mapOf(
            "nextExecution" to "re.next_execution",
            "id" to "re.id",
        )

    fun findAllEntries(
        scope: WalletTransactionQueryScope,
        minimumEndExecution: LocalDate?,
        maximumNextExecution: LocalDate?,
        billDate: LocalDate?,
        walletItemId: UUID?,
        walletItemIds: Set<UUID>,
        entryTypes: Set<WalletEntryType>,
        categoryConceptIds: Set<UUID>,
        includeUncategorized: Boolean,
        sort: Sort = Sort.unsorted(),
    ): Flux<RecurrenceEventEntity> {
        val orderClause = resolveOrderClause(sort)
        val eventIdsSql =
            when (scope.path) {
                WalletTransactionQueryPath.OWNERSHIP ->
                    buildOwnershipSql(
                        scope = scope,
                        minimumEndExecution = minimumEndExecution,
                        maximumNextExecution = maximumNextExecution,
                        billDate = billDate,
                        walletItemId = walletItemId,
                        walletItemIds = walletItemIds,
                        entryTypes = entryTypes,
                        categoryConceptIds = categoryConceptIds,
                        includeUncategorized = includeUncategorized,
                        orderClause = orderClause,
                    )
                WalletTransactionQueryPath.GROUP_SCOPE ->
                    buildGroupScopeSql(
                        scope = scope,
                        minimumEndExecution = minimumEndExecution,
                        maximumNextExecution = maximumNextExecution,
                        billDate = billDate,
                        walletItemId = walletItemId,
                        walletItemIds = walletItemIds,
                        entryTypes = entryTypes,
                        categoryConceptIds = categoryConceptIds,
                        includeUncategorized = includeUncategorized,
                        orderClause = orderClause,
                    )
            }

        var spec = dbClient.sql(eventIdsSql)

        if (scope.ownerUserIds.isNotEmpty()) {
            spec = spec.bind("ownerUserIds", scope.ownerUserIds.toTypedArray())
        }
        if (scope.groupIds.isNotEmpty()) {
            spec = spec.bind("groupIds", scope.groupIds.toTypedArray())
        }
        if (minimumEndExecution != null) {
            spec = spec.bind("minimumEndExecution", minimumEndExecution)
        }
        if (maximumNextExecution != null) {
            spec = spec.bind("maximumNextExecution", maximumNextExecution)
        }
        if (walletItemId != null) {
            spec = spec.bind("walletItemId", walletItemId)
        }
        if (billDate != null && walletItemId != null) {
            spec = spec.bind("billDate", billDate)
        }
        if (walletItemIds.isNotEmpty()) {
            spec = spec.bind("walletItemIds", walletItemIds.toTypedArray())
        }
        if (entryTypes.isNotEmpty()) {
            spec = spec.bind("entryTypes", entryTypes.map { it.name }.toTypedArray())
        }
        if (categoryConceptIds.isNotEmpty()) {
            spec = spec.bind("categoryConceptIds", categoryConceptIds.toTypedArray())
        }

        return spec
            .map { row, _ -> row.get("id", UUID::class.java)!! }
            .all()
            .collectList()
            .flatMapMany { eventIds ->
                if (eventIds.isEmpty()) {
                    return@flatMapMany Flux.empty<RecurrenceEventEntity>()
                }

                val fullSql =
                    """
                    SELECT
                        re.*,
                        rs.qty_total AS series_qty_total,
                        cat.id AS event_category_id,
                        cat.created_at AS event_category_created_at,
                        cat.updated_at AS event_category_updated_at,
                        cat.name AS event_category_name,
                        cat.color AS event_category_color,
                        cat.user_id AS event_category_user_id,
                        cat.group_id AS event_category_group_id,
                        cat.parent_id AS event_category_parent_id,
                        cat.concept_id AS event_category_concept_id,
                        grp.id AS event_group_id,
                        grp.created_at AS event_group_created_at,
                        grp.updated_at AS event_group_updated_at,
                        grp.name AS event_group_name,
                        ${UserR2DBCMapping.createSelectForUser("usr", "event_user_")},
                        ren.id AS entry_id,
                        ${RecurrenceEntryR2DBCMapping.createSelectForRecurrenceEntry("ren", "entry_")},
                        ${WalletItemR2DBCMapping.createSelectForWalletItem("wi", "entry_wi_")}
                    FROM recurrence_event re
                    LEFT JOIN recurrence_series rs ON rs.id = re.series_id
                    LEFT JOIN wallet_entry_category cat ON cat.id = re.category_id
                    LEFT JOIN "group" grp ON grp.id = re.group_id
                    LEFT JOIN users usr ON usr.id = re.created_by_user_id
                    JOIN recurrence_entry ren ON ren.wallet_event_id = re.id
                    JOIN wallet_item wi ON wi.id = ren.wallet_item_id
                    WHERE re.id = ANY(:eventIds)
                    ORDER BY $orderClause, ren.value ASC
                    """.trimIndent()

                dbClient
                    .sql(fullSql)
                    .bind("eventIds", eventIds.toTypedArray())
                    .map { row, _ ->
                        val event = RecurrenceEventR2DBCMapping.recurrenceEventFromRow(row, "")
                        event.seriesQtyTotal = row.get("series_qty_total", Int::class.javaObjectType)
                        event.hydratedCategory = categoryFromRowOrNull(row)
                        event.hydratedGroup = groupFromRowOrNull(row)
                        event.hydratedUser = userFromRowOrNull(row)
                        val entry = RecurrenceEntryR2DBCMapping.recurrenceEntryFromRow(row, "entry_")
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

    private fun buildOwnershipSql(
        scope: WalletTransactionQueryScope,
        minimumEndExecution: LocalDate?,
        maximumNextExecution: LocalDate?,
        billDate: LocalDate?,
        walletItemId: UUID?,
        walletItemIds: Set<UUID>,
        entryTypes: Set<WalletEntryType>,
        categoryConceptIds: Set<UUID>,
        includeUncategorized: Boolean,
        orderClause: String,
    ): String {
        var sql =
            """
            SELECT re.id
            FROM recurrence_entry ren
            INNER JOIN wallet_item wi ON wi.id = ren.wallet_item_id
            INNER JOIN recurrence_event re ON re.id = ren.wallet_event_id
            WHERE wi.user_id = ANY(:ownerUserIds)
            """.trimIndent()

        if (scope.groupIds.isNotEmpty()) {
            sql += " AND re.group_id = ANY(:groupIds)"
        }
        if (minimumEndExecution != null) {
            sql += " AND (re.end_execution >= :minimumEndExecution OR re.end_execution IS NULL)"
        }
        if (maximumNextExecution != null) {
            sql += " AND re.next_execution <= :maximumNextExecution"
        }
        if (walletItemId != null) {
            sql += " AND ren.wallet_item_id = :walletItemId"
            if (billDate != null) {
                sql += " AND ren.next_bill_date <= :billDate AND (ren.last_bill_date >= :billDate OR ren.last_bill_date IS NULL)"
            }
        }
        if (walletItemIds.isNotEmpty()) {
            sql += " AND ren.wallet_item_id = ANY(:walletItemIds)"
        }
        if (entryTypes.isNotEmpty()) {
            sql += " AND re.type = ANY(:entryTypes)"
        }
        val categoryPredicate =
            buildCategoryPredicateSql(
                categoryConceptIds = categoryConceptIds,
                includeUncategorized = includeUncategorized,
            )
        if (categoryPredicate.isNotBlank()) {
            sql += " $categoryPredicate"
        }

        sql += " GROUP BY re.id, re.next_execution"
        sql += " ORDER BY $orderClause"
        return sql
    }

    private fun buildGroupScopeSql(
        scope: WalletTransactionQueryScope,
        minimumEndExecution: LocalDate?,
        maximumNextExecution: LocalDate?,
        billDate: LocalDate?,
        walletItemId: UUID?,
        walletItemIds: Set<UUID>,
        entryTypes: Set<WalletEntryType>,
        categoryConceptIds: Set<UUID>,
        includeUncategorized: Boolean,
        orderClause: String,
    ): String {
        var sql =
            """
            SELECT re.id
            FROM recurrence_event re
            WHERE re.group_id = ANY(:groupIds)
            """.trimIndent()

        if (minimumEndExecution != null) {
            sql += " AND (re.end_execution >= :minimumEndExecution OR re.end_execution IS NULL)"
        }
        if (maximumNextExecution != null) {
            sql += " AND re.next_execution <= :maximumNextExecution"
        }
        if (walletItemId != null) {
            sql +=
                " AND EXISTS (SELECT 1 FROM recurrence_entry ren WHERE ren.wallet_event_id = re.id AND ren.wallet_item_id = :walletItemId"
            if (billDate != null) {
                sql += " AND ren.next_bill_date <= :billDate AND (ren.last_bill_date >= :billDate OR ren.last_bill_date IS NULL)"
            }
            sql += ")"
        }
        if (walletItemIds.isNotEmpty()) {
            sql +=
                " AND EXISTS (SELECT 1 FROM recurrence_entry ren WHERE ren.wallet_event_id = re.id AND ren.wallet_item_id = ANY(:walletItemIds))"
        }
        if (entryTypes.isNotEmpty()) {
            sql += " AND re.type = ANY(:entryTypes)"
        }
        val categoryPredicate =
            buildCategoryPredicateSql(
                categoryConceptIds = categoryConceptIds,
                includeUncategorized = includeUncategorized,
            )
        if (categoryPredicate.isNotBlank()) {
            sql += " $categoryPredicate"
        }

        sql += " ORDER BY $orderClause"
        return sql
    }

    private fun buildCategoryPredicateSql(
        categoryConceptIds: Set<UUID>,
        includeUncategorized: Boolean,
    ): String =
        when {
            categoryConceptIds.isEmpty() && !includeUncategorized -> ""
            categoryConceptIds.isEmpty() && includeUncategorized -> " AND re.category_id IS NULL"
            categoryConceptIds.isNotEmpty() && includeUncategorized ->
                """
                AND (
                   re.category_id IS NULL
                   OR EXISTS (
                       SELECT 1
                       FROM wallet_entry_category cat
                       WHERE cat.id = re.category_id
                         AND cat.concept_id = ANY(:categoryConceptIds)
                   )
                )
                """.trimIndent()
            else ->
                """
                AND EXISTS (
                   SELECT 1
                   FROM wallet_entry_category cat
                   WHERE cat.id = re.category_id
                     AND cat.concept_id = ANY(:categoryConceptIds)
                )
                """.trimIndent()
        }

    private fun resolveOrderClause(sort: Sort): String {
        if (!sort.isSorted) {
            return "re.next_execution DESC NULLS LAST, re.id DESC"
        }

        val clauses =
            sort
                .toList()
                .mapNotNull { order ->
                    val column = validSortColumns[order.property] ?: return@mapNotNull null
                    val direction = if (order.isAscending) "ASC" else "DESC"
                    "$column $direction"
                }

        return if (clauses.isEmpty()) {
            "re.next_execution DESC NULLS LAST, re.id DESC"
        } else {
            clauses.joinToString(", ")
        }
    }

    private fun categoryFromRowOrNull(row: io.r2dbc.spi.Row): WalletEntryCategoryEntity? {
        val categoryId = row.get("event_category_id", UUID::class.java) ?: return null

        return WalletEntryCategoryEntity(
            name = row.get("event_category_name", String::class.java)!!,
            color = row.get("event_category_color", String::class.java)!!,
            userId = row.get("event_category_user_id", UUID::class.java),
            groupId = row.get("event_category_group_id", UUID::class.java),
            parentId = row.get("event_category_parent_id", UUID::class.java),
            conceptId = row.get("event_category_concept_id", UUID::class.java)!!,
        ).also { category ->
            category.id = categoryId
            category.createdAt = row.get("event_category_created_at", OffsetDateTime::class.java)
            category.updatedAt = row.get("event_category_updated_at", OffsetDateTime::class.java)
        }
    }

    private fun groupFromRowOrNull(row: io.r2dbc.spi.Row): com.ynixt.sharedfinances.domain.entities.groups.GroupEntity? {
        val groupId = row.get("event_group_id", UUID::class.java) ?: return null

        return com.ynixt.sharedfinances.domain.entities.groups
            .GroupEntity(
                name = row.get("event_group_name", String::class.java)!!,
            ).also { group ->
                group.id = groupId
                group.createdAt = row.get("event_group_created_at", OffsetDateTime::class.java)
                group.updatedAt = row.get("event_group_updated_at", OffsetDateTime::class.java)
            }
    }

    private fun userFromRowOrNull(row: io.r2dbc.spi.Row): com.ynixt.sharedfinances.domain.entities.UserEntity? =
        row.get("event_user_id", UUID::class.java)?.let {
            UserR2DBCMapping.userFromRow(row, "event_user_")
        }
}

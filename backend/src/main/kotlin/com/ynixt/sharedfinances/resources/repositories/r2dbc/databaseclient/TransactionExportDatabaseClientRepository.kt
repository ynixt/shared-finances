package com.ynixt.sharedfinances.resources.repositories.r2dbc.databaseclient

import com.ynixt.sharedfinances.domain.exceptions.http.UnauthorizedException
import com.ynixt.sharedfinances.domain.models.exports.ActiveRecurrenceExportRow
import com.ynixt.sharedfinances.domain.models.exports.TransactionExportCursor
import com.ynixt.sharedfinances.domain.models.exports.TransactionExportFilter
import com.ynixt.sharedfinances.domain.models.exports.TransactionExportRow
import com.ynixt.sharedfinances.domain.repositories.TransactionExportRepository
import com.ynixt.sharedfinances.domain.services.groups.GroupPermissionService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

@Repository
open class TransactionExportDatabaseClientRepository(
    private val dbClient: DatabaseClient,
    private val groupPermissionService: GroupPermissionService,
) : TransactionExportRepository {
    override suspend fun countLines(
        userId: UUID,
        filter: TransactionExportFilter,
    ): Long {
        assertGroupAccess(userId, filter)
        val query = filteredQuery(userId, filter)
        return bind(dbClient.sql("SELECT COUNT(*) AS qty ${query.sql}"), query.bindings)
            .map { row, _ -> row.get("qty", java.lang.Long::class.java)!!.toLong() }
            .one()
            .awaitSingle()
    }

    override suspend fun findRows(
        userId: UUID,
        filter: TransactionExportFilter,
        pageSize: Int,
    ): Flow<TransactionExportRow> {
        require(pageSize in 1..2_000) { "Export page size must be between 1 and 2000" }
        assertGroupAccess(userId, filter)
        return flow {
            var cursor: TransactionExportCursor? = null
            while (true) {
                var pageCount = 0
                var lastCursor: TransactionExportCursor? = null
                findPage(userId, filter, cursor, pageSize).collect { row ->
                    emit(row)
                    lastCursor = row.cursor
                    pageCount++
                }
                if (pageCount < pageSize) break
                cursor = requireNotNull(lastCursor)
            }
        }
    }

    internal open fun findPage(
        userId: UUID,
        filter: TransactionExportFilter,
        cursor: TransactionExportCursor?,
        pageSize: Int,
    ): Flow<TransactionExportRow> {
        val query = filteredQuery(userId, filter, cursor)
        val select =
            """
            SELECT
                entry.id AS entry_id,
                event.id AS event_id,
                entry.wallet_item_id AS origin_id,
                item.name AS origin_name,
                event.date,
                event.name AS description,
                entry.value,
                item.currency,
                event.category_id AS category_id,
                category.name AS category_name,
                category.concept_id AS category_concept_id,
                event.group_id AS group_id,
                grp.name AS group_name,
                CASE
                    WHEN event.installment IS NOT NULL AND series.qty_total IS NOT NULL
                    THEN event.installment::text || '/' || series.qty_total::text
                END AS installment,
                (
                    SELECT string_agg(beneficiary.email || ':' || eb.benefit_percent::text, '|' ORDER BY beneficiary.email)
                    FROM wallet_event_beneficiary eb
                    JOIN "users" beneficiary ON beneficiary.id = eb.beneficiary_user_id
                    WHERE eb.wallet_event_id = event.id
                ) AS beneficiaries,
                bill.bill_date AS bill,
                event.tags,
                event.observations,
                event.confirmed,
                COALESCE(event.external_transaction_id, event.id::text) AS transaction_id,
                CASE WHEN event.type = 'TRANSFER' THEN event.id::text END AS transfer_id,
                recurrence.series_id::text AS series_id
            ${query.sql}
            ORDER BY event.date ASC, event.id ASC, entry.id ASC
            LIMIT :pageSize
            """.trimIndent()
        return bind(dbClient.sql(select), query.bindings)
            .bind("pageSize", pageSize)
            .map { row, _ ->
                val eventId = row.get("event_id", UUID::class.java)!!
                val entryId = row.get("entry_id", UUID::class.java)!!
                val date = row.get("date", LocalDate::class.java)!!
                TransactionExportRow(
                    origin = row.get("origin_id", UUID::class.java)!!.toString(),
                    originName = row.get("origin_name", String::class.java)!!,
                    date = date,
                    description = row.get("description", String::class.java),
                    value = row.get("value", BigDecimal::class.java)!!,
                    currency = row.get("currency", String::class.java)!!,
                    category = row.get("category_id", UUID::class.java)?.toString(),
                    categoryName = row.get("category_name", String::class.java),
                    categoryConceptId = row.get("category_concept_id", UUID::class.java)?.toString(),
                    group = row.get("group_id", UUID::class.java)?.toString(),
                    groupName = row.get("group_name", String::class.java),
                    installment = row.get("installment", String::class.java),
                    beneficiaries = row.get("beneficiaries", String::class.java),
                    bill = row.get("bill", LocalDate::class.java),
                    tags = row.get("tags", Array<String>::class.java)?.toList().orEmpty(),
                    observations = row.get("observations", String::class.java),
                    confirmed = row.get("confirmed", java.lang.Boolean::class.java)!!.booleanValue(),
                    transactionId = row.get("transaction_id", String::class.java)!!,
                    transferId = row.get("transfer_id", String::class.java),
                    seriesId = row.get("series_id", String::class.java),
                    cursor = TransactionExportCursor(date, eventId, entryId),
                )
            }.all()
            .asFlow()
    }

    override suspend fun findActiveRecurrences(
        userId: UUID,
        filter: TransactionExportFilter,
    ): Flow<ActiveRecurrenceExportRow> {
        assertGroupAccess(userId, filter)
        val conditions = mutableListOf("recurrence.initial_balance = FALSE", "recurrence.next_execution IS NOT NULL")
        val bindings = linkedMapOf<String, Any>()
        addScope(userId, filter, conditions, bindings, "recurrence")
        filter.dateFrom?.let { from ->
            bindings["dateFrom"] = from
            conditions += recurrenceDateCondition(filter.billDateMode, lowerBound = true)
        }
        filter.dateTo?.let { to ->
            bindings["dateTo"] = to
            conditions += recurrenceDateCondition(filter.billDateMode, lowerBound = false)
        }
        if (filter.walletItemIds.isNotEmpty()) {
            conditions +=
                "EXISTS (SELECT 1 FROM recurrence_entry recurrence_entry_filter " +
                "WHERE recurrence_entry_filter.wallet_event_id = recurrence.id " +
                "AND recurrence_entry_filter.wallet_item_id = ANY(CAST(:walletItemIds AS UUID[])))"
            bindings["walletItemIds"] = filter.walletItemIds.toTypedArray()
        }
        if (filter.categoryIds.isNotEmpty()) {
            conditions += "recurrence.category_id = ANY(CAST(:categoryIds AS UUID[]))"
            conditions +=
                "(category.user_id = :userId OR category.group_id IN " +
                "(SELECT group_id FROM group_user WHERE user_id = :userId))"
            bindings["categoryIds"] = filter.categoryIds.toTypedArray()
        }
        if (filter.entryTypes.isNotEmpty()) {
            conditions += "recurrence.type = ANY(CAST(:entryTypes AS TEXT[]))"
            bindings["entryTypes"] = filter.entryTypes.map { it.name }.toTypedArray()
        }
        if (filter.tags.isNotEmpty()) {
            conditions += "recurrence.tags && CAST(:tags AS TEXT[])"
            bindings["tags"] = filter.tags.toTypedArray()
        }
        val sql =
            """
            SELECT DISTINCT ON (recurrence.series_id)
                recurrence.name AS description,
                recurrence.payment_type,
                recurrence.next_execution,
                category.name AS category_name,
                grp.name AS group_name,
                recurrence.series_id
            FROM recurrence_event recurrence
            LEFT JOIN wallet_entry_category category ON category.id = recurrence.category_id
            LEFT JOIN "group" grp ON grp.id = recurrence.group_id
            WHERE ${conditions.joinToString(" AND ")}
            ORDER BY recurrence.series_id, recurrence.next_execution ASC
            """.trimIndent()
        return bind(dbClient.sql(sql), bindings)
            .map { row, _ ->
                ActiveRecurrenceExportRow(
                    description = row.get("description", String::class.java),
                    paymentType = enumValueOf(row.get("payment_type", String::class.java)!!),
                    nextExecution = row.get("next_execution", LocalDate::class.java),
                    category = row.get("category_name", String::class.java),
                    group = row.get("group_name", String::class.java),
                    seriesId = row.get("series_id", UUID::class.java)!!,
                )
            }.all()
            .asFlow()
    }

    private suspend fun assertGroupAccess(
        userId: UUID,
        filter: TransactionExportFilter,
    ) {
        val groupId = filter.groupId ?: return
        if (!groupPermissionService.hasPermission(userId, groupId)) throw UnauthorizedException()
    }

    internal fun filteredQuery(
        userId: UUID,
        filter: TransactionExportFilter,
        cursor: TransactionExportCursor? = null,
    ): QueryParts {
        val conditions = mutableListOf("event.initial_balance = FALSE")
        val bindings = linkedMapOf<String, Any>()
        addScope(userId, filter, conditions, bindings, "event")
        filter.dateFrom?.let { from ->
            bindings["dateFrom"] = from
            if (filter.billDateMode) {
                conditions +=
                    "((entry.bill_id IS NOT NULL AND bill.bill_date >= date_trunc('month', CAST(:dateFrom AS DATE))::date) OR " +
                    "(entry.bill_id IS NULL AND event.date >= :dateFrom))"
            } else {
                conditions += "event.date >= :dateFrom"
            }
        }
        filter.dateTo?.let { to ->
            bindings["dateTo"] = to
            if (filter.billDateMode) {
                conditions +=
                    "((entry.bill_id IS NOT NULL AND bill.bill_date <= date_trunc('month', CAST(:dateTo AS DATE))::date) OR " +
                    "(entry.bill_id IS NULL AND event.date <= :dateTo))"
            } else {
                conditions += "event.date <= :dateTo"
            }
        }
        if (filter.walletItemIds.isNotEmpty()) {
            conditions += "entry.wallet_item_id = ANY(CAST(:walletItemIds AS UUID[]))"
            bindings["walletItemIds"] = filter.walletItemIds.toTypedArray()
        }
        if (filter.categoryIds.isNotEmpty()) {
            conditions += "event.category_id = ANY(CAST(:categoryIds AS UUID[]))"
            conditions +=
                "(category.user_id = :userId OR category.group_id IN " +
                "(SELECT group_id FROM group_user WHERE user_id = :userId))"
            bindings["categoryIds"] = filter.categoryIds.toTypedArray()
        }
        if (filter.entryTypes.isNotEmpty()) {
            conditions += "event.type = ANY(CAST(:entryTypes AS TEXT[]))"
            bindings["entryTypes"] = filter.entryTypes.map { it.name }.toTypedArray()
        }
        if (filter.tags.isNotEmpty()) {
            conditions += "event.tags && CAST(:tags AS TEXT[])"
            bindings["tags"] = filter.tags.toTypedArray()
        }
        filter.confirmed?.let {
            conditions += "event.confirmed = :confirmed"
            bindings["confirmed"] = it
        }
        cursor?.let {
            conditions += "(event.date, event.id, entry.id) > (:cursorDate, :cursorEventId, :cursorEntryId)"
            bindings["cursorDate"] = it.date
            bindings["cursorEventId"] = it.eventId
            bindings["cursorEntryId"] = it.entryId
        }
        val sql =
            """
            FROM wallet_entry entry
            JOIN wallet_event event ON event.id = entry.wallet_event_id
            JOIN wallet_item item ON item.id = entry.wallet_item_id
            LEFT JOIN credit_card_bill bill ON bill.id = entry.bill_id
            LEFT JOIN wallet_entry_category category ON category.id = event.category_id
            LEFT JOIN "group" grp ON grp.id = event.group_id
            LEFT JOIN recurrence_event recurrence ON recurrence.id = event.recurrence_event_id
            LEFT JOIN recurrence_series series ON series.id = recurrence.series_id
            WHERE ${conditions.joinToString(" AND ")}
            """.trimIndent()
        return QueryParts(sql, bindings)
    }

    private fun addScope(
        userId: UUID,
        filter: TransactionExportFilter,
        conditions: MutableList<String>,
        bindings: MutableMap<String, Any>,
        alias: String,
    ) {
        bindings["userId"] = userId
        if (filter.groupId == null) {
            conditions += "$alias.created_by_user_id = :userId"
        } else {
            conditions += "$alias.group_id = :groupId"
            bindings["groupId"] = filter.groupId
        }
    }

    private fun bind(
        initial: DatabaseClient.GenericExecuteSpec,
        bindings: Map<String, Any>,
    ): DatabaseClient.GenericExecuteSpec = bindings.entries.fold(initial) { spec, (name, value) -> spec.bind(name, value) }

    private fun recurrenceDateCondition(
        billDateMode: Boolean,
        lowerBound: Boolean,
    ): String {
        val operator = if (lowerBound) ">=" else "<="
        val parameter = if (lowerBound) "dateFrom" else "dateTo"
        if (!billDateMode) return "recurrence.next_execution $operator :$parameter"
        return "EXISTS (SELECT 1 FROM recurrence_entry recurrence_entry_date " +
            "WHERE recurrence_entry_date.wallet_event_id = recurrence.id AND " +
            "((recurrence_entry_date.next_bill_date IS NOT NULL AND recurrence_entry_date.next_bill_date $operator " +
            "date_trunc('month', CAST(:$parameter AS DATE))::date) OR " +
            "(recurrence_entry_date.next_bill_date IS NULL AND recurrence.next_execution $operator :$parameter)))"
    }

    internal data class QueryParts(
        val sql: String,
        val bindings: Map<String, Any>,
    )
}

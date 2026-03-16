package com.ynixt.sharedfinances.resources.repositories.r2dbc.databaseclient

import com.ynixt.sharedfinances.domain.entities.wallet.entries.RecurrenceEventEntity
import com.ynixt.sharedfinances.resources.repositories.r2dbc.mapping.RecurrenceEntryR2DBCMapping
import com.ynixt.sharedfinances.resources.repositories.r2dbc.mapping.RecurrenceEventR2DBCMapping
import com.ynixt.sharedfinances.resources.repositories.r2dbc.mapping.WalletItemR2DBCMapping
import org.springframework.data.domain.Sort
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import java.time.LocalDate
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

    fun findAll(
        minimumEndExecution: LocalDate?,
        maximumNextExecution: LocalDate?,
        billDate: LocalDate?,
        walletItemId: UUID?,
        userId: UUID?,
        groupId: UUID?,
        sort: Sort = Sort.unsorted(),
    ): Flux<RecurrenceEventEntity> {
        // Query 1: find all event IDs
        var eventIdsSql = """
        SELECT re.id
        FROM recurrence_event re
    """

        if (minimumEndExecution != null) {
            eventIdsSql += " AND (re.end_execution >= :minimumEndExecution OR re.end_execution IS NULL)"
        }

        if (maximumNextExecution != null) {
            eventIdsSql += " AND (re.next_execution <= :maximumNextExecution)"
        }

        if (walletItemId != null) {
            eventIdsSql +=
                " AND EXISTS (SELECT 1 FROM recurrence_entry ren WHERE ren.wallet_event_id = re.id AND ren.wallet_item_id = :walletItemId"
            if (billDate != null) {
                eventIdsSql += " AND ren.next_bill_date <= :billDate AND (ren.last_bill_date >= :billDate OR ren.last_bill_date IS NULL)"
            }
            eventIdsSql += ")"
        }

        if (userId != null) eventIdsSql += " AND re.user_id = :userId"
        if (groupId != null) eventIdsSql += " AND re.group_id = :groupId"

        eventIdsSql = eventIdsSql.replaceFirst(" AND ", " WHERE ")

        val orderClause =
            if (sort.isSorted) {
                sort
                    .stream()
                    .map { order ->
                        val column = validSortColumns[order.property]
                        if (column != null) {
                            val direction = if (order.isAscending) "ASC" else "DESC"
                            "$column $direction"
                        } else {
                            null
                        }
                    }.filter { it != null }
                    .toList()
                    .joinToString(", ")
            } else {
                ""
            }

        if (orderClause.isNotEmpty()) {
            eventIdsSql += " ORDER BY $orderClause"
        }

        var spec = dbClient.sql(eventIdsSql)

        if (minimumEndExecution != null) spec = spec.bind("minimumEndExecution", minimumEndExecution)
        if (maximumNextExecution != null) spec = spec.bind("maximumNextExecution", maximumNextExecution)
        if (walletItemId != null) spec = spec.bind("walletItemId", walletItemId)
        if (billDate != null && walletItemId != null) spec = spec.bind("billDate", billDate)
        if (userId != null) spec = spec.bind("userId", userId)
        if (groupId != null) spec = spec.bind("groupId", groupId)

        return spec
            .map { row, _ -> row.get("id", UUID::class.java)!! }
            .all()
            .collectList()
            .flatMapMany { eventIds ->
                if (eventIds.isEmpty()) return@flatMapMany Flux.empty<RecurrenceEventEntity>()

                // Query 2
                var fullSql = """
                SELECT
                    re.*,
                    ren.id AS entry_id,
                    ${RecurrenceEntryR2DBCMapping.createSelectForRecurrenceEntry("ren", "entry_")},
                    ${WalletItemR2DBCMapping.createSelectForWalletItem("wi", "entry_wi_")}
                FROM recurrence_event re
                JOIN recurrence_entry ren ON ren.wallet_event_id = re.id
                JOIN wallet_item wi ON wi.id = ren.wallet_item_id
                WHERE re.id = ANY(:eventIds)
            """

                if (orderClause.isNotEmpty()) {
                    fullSql += " ORDER BY $orderClause, ren.value ASC"
                } else {
                    fullSql += " ORDER BY re.id, ren.value ASC"
                }

                dbClient
                    .sql(fullSql)
                    .bind("eventIds", eventIds.toTypedArray())
                    .map { row, _ ->
                        val event = RecurrenceEventR2DBCMapping.recurrenceEventFromRow(row, "")
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
}

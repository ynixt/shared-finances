package com.ynixt.sharedfinances.resources.repositories.r2dbc

import com.ynixt.sharedfinances.domain.entities.wallet.entries.WalletEventEntity
import com.ynixt.sharedfinances.domain.repositories.WalletEventCursorFindAll
import com.ynixt.sharedfinances.resources.repositories.r2dbc.mapping.WalletEntryR2DBCMapping
import com.ynixt.sharedfinances.resources.repositories.r2dbc.mapping.WalletEventR2DBCMapping
import com.ynixt.sharedfinances.resources.repositories.r2dbc.mapping.WalletItemR2DBCMapping
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import java.time.LocalDate
import java.util.UUID

@Repository
class WalletEventR2DBCRepository(
    private val dbClient: DatabaseClient,
) : R2BDCGenericRepository() {
    fun findAll(
        userId: UUID?,
        groupId: UUID?,
        limit: Int,
        walletItemId: UUID?,
        minimumDate: LocalDate?,
        maximumDate: LocalDate?,
        billId: UUID?,
        cursor: WalletEventCursorFindAll?,
    ): Flux<WalletEventEntity> {
        require(userId != null || groupId != null) { "Either userId or groupId must be provided" }

        // Query 1: find event ids, paginated
        var eventIdsSql = """
        SELECT we.id
        FROM wallet_event we
        WHERE
    """

        if (userId != null) eventIdsSql += " we.user_id = :userId"
        if (groupId != null) eventIdsSql += " we.group_id = :groupId"

        if (walletItemId !=
            null
        ) {
            eventIdsSql +=
                " AND EXISTS (SELECT 1 FROM wallet_entry wen WHERE wen.wallet_event_id = we.id AND wen.wallet_item_id = :walletItemId)"
        }
        if (billId !=
            null
        ) {
            eventIdsSql += " AND EXISTS (SELECT 1 FROM wallet_entry wen WHERE wen.wallet_event_id = we.id AND wen.bill_id = :billId)"
        }
        if (minimumDate != null) eventIdsSql += " AND we.date >= :minimumDate"
        if (maximumDate != null) eventIdsSql += " AND we.date <= :maximumDate"
        if (cursor != null) eventIdsSql += " AND (we.date, we.id) < (:cursorDate, :cursorId)"

        eventIdsSql += """
        ORDER BY we.date DESC, we.id DESC
        LIMIT :limit
    """

        var spec = dbClient.sql(eventIdsSql)

        if (userId != null) spec = spec.bind("userId", userId)
        if (groupId != null) spec = spec.bind("groupId", groupId)
        if (walletItemId != null) spec = spec.bind("walletItemId", walletItemId)
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
                        event.entries = pairs.map { it.second }
                        event
                    }
            }
    }
}

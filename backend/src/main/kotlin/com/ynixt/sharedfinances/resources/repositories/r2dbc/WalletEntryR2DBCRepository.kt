package com.ynixt.sharedfinances.resources.repositories.r2dbc

import com.ynixt.sharedfinances.domain.entities.wallet.entries.WalletEntryEntity
import com.ynixt.sharedfinances.domain.repositories.WalletEntryCursorFindAll
import com.ynixt.sharedfinances.resources.repositories.r2dbc.mapping.WalletEntryR2DBCMapping
import com.ynixt.sharedfinances.resources.repositories.r2dbc.mapping.WalletItemR2DBCMapping
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import java.time.LocalDate
import java.util.UUID

@Repository
class WalletEntryR2DBCRepository(
    private val dbClient: DatabaseClient,
) : R2BDCGenericRepository() {
    fun findAll(
        userId: UUID?,
        groupId: UUID?,
        limit: Int,
        walletItemIs: List<UUID>?,
        minimumDate: LocalDate?,
        maximumDate: LocalDate?,
        cursor: WalletEntryCursorFindAll?,
    ): Flux<WalletEntryEntity> {
        require(userId != null || groupId != null) { "Either userId or groupId must be provided" }

        var sql = """
            select 
                we.*,
                ${WalletItemR2DBCMapping.createSelectForWalletItem("origin", "origin_")},
                ${WalletItemR2DBCMapping.createSelectForWalletItem("target", "target_")}
            from 
                wallet_entry we
            join wallet_item origin 
                on origin.id = we.origin_id
            left join wallet_item target 
                on target.id = we.target_id
            where
        """

        if (userId != null) sql += " we.user_id = :userId"
        if (groupId != null) sql += " we.group_id = :groupId"

        if (walletItemIs != null) sql += " and (we.origin_id in (:walletItemIs) or we.target_id in (:walletItemIs))"
        if (minimumDate != null) sql += " and we.date >= :minimumDate"
        if (maximumDate != null) sql += " and we.date <= :maximumDate"

        if (cursor != null) sql += " and (we.date, we.id) < (:cursorDate, :cursorId)"

        sql +=
            """
            ORDER BY we.date DESC, we.id DESC 
            LIMIT :limit
            """

        var spec =
            dbClient
                .sql(sql)

        if (userId != null) spec = spec.bind("userId", userId)
        if (groupId != null) spec = spec.bind("groupId", groupId)
        if (walletItemIs != null) spec = spec.bind("walletItemIs", walletItemIs)
        if (minimumDate != null) spec = spec.bind("minimumDate", minimumDate)
        if (maximumDate != null) spec = spec.bind("maximumDate", maximumDate)
        if (cursor != null) spec = spec.bind("cursorDate", cursor.maximumDate).bind("cursorId", cursor.maximumId)

        spec = spec.bind("limit", limit)

        return spec
            .map { row, _ ->
                WalletEntryR2DBCMapping.walletEntryFromRow(row, "").also {
                    it.origin = WalletItemR2DBCMapping.walletItemFromRow(row, "origin_")
                    it.target = WalletItemR2DBCMapping.walletItemFromRow(row, "target_")
                }
            }.all()
    }
}

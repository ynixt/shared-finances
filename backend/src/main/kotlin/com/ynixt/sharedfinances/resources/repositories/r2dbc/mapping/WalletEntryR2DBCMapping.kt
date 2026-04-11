package com.ynixt.sharedfinances.resources.repositories.r2dbc.mapping

import com.ynixt.sharedfinances.domain.entities.wallet.entries.WalletEntryEntity
import io.r2dbc.spi.Row
import java.math.BigDecimal
import java.util.UUID

class WalletEntryR2DBCMapping {
    companion object {
        fun createSelectForWalletEntry(
            tableAlias: String = "wey",
            columnPrefix: String = "wey_",
        ): String =
            """
            $tableAlias.id                              AS ${columnPrefix}id,
            $tableAlias.created_at                      AS ${columnPrefix}created_at,
            $tableAlias.updated_at                      AS ${columnPrefix}updated_at,
            $tableAlias.value                           AS ${columnPrefix}value,
            $tableAlias.wallet_event_id                 AS ${columnPrefix}wallet_event_id,
            $tableAlias.wallet_item_id                  AS ${columnPrefix}wallet_item_id,
            $tableAlias.bill_id                         AS ${columnPrefix}bill_id,
            $tableAlias.contribution_percent            AS ${columnPrefix}contribution_percent
            """.trimIndent()

        fun walletEntryFromRow(
            row: Row,
            columnPrefix: String = "wt_",
        ): WalletEntryEntity =
            WalletEntryEntity(
                value = row.get("${columnPrefix}value", BigDecimal::class.java)!!,
                walletEventId = row.get("${columnPrefix}wallet_event_id", UUID::class.java)!!,
                walletItemId = row.get("${columnPrefix}wallet_item_id", UUID::class.java)!!,
                billId = row.get("${columnPrefix}bill_id", UUID::class.java),
                contributionPercent = row.get("${columnPrefix}contribution_percent", BigDecimal::class.java),
            ).also { gu ->
                gu.id = row.get("${columnPrefix}id", UUID::class.java)
            }
    }
}

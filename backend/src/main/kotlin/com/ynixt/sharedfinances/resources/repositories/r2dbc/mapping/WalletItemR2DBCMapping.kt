package com.ynixt.sharedfinances.resources.repositories.r2dbc.mapping

import com.ynixt.sharedfinances.domain.entities.wallet.WalletItemEntity
import com.ynixt.sharedfinances.domain.enums.WalletItemType
import io.r2dbc.spi.Row
import java.math.BigDecimal
import java.util.UUID

class WalletItemR2DBCMapping {
    companion object {
        fun createSelectForWalletItem(
            tableAlias: String = "wi",
            columnPrefix: String = "wi_",
        ): String =
            """
            $tableAlias.id                              AS ${columnPrefix}id,
            $tableAlias.created_at                      AS ${columnPrefix}created_at,
            $tableAlias.updated_at                      AS ${columnPrefix}updated_at,
            $tableAlias.name                            AS ${columnPrefix}name,
            $tableAlias.type                            AS ${columnPrefix}type,
            $tableAlias.enabled                         AS ${columnPrefix}enabled,
            $tableAlias.user_id                         AS ${columnPrefix}user_id,
            $tableAlias.currency                        AS ${columnPrefix}currency,
            $tableAlias.total_limit                     AS ${columnPrefix}total_limit,
            $tableAlias.balance                         AS ${columnPrefix}balance,
            $tableAlias.due_day                         AS ${columnPrefix}due_day,
            $tableAlias.days_between_due_and_closing    AS ${columnPrefix}days_between_due_and_closing,
            $tableAlias.due_on_next_business_day        AS ${columnPrefix}due_on_next_business_day
            """.trimIndent()

        fun walletItemFromRow(
            row: Row,
            columnPrefix: String = "wi_",
        ): WalletItemEntity? =
            row.get("${columnPrefix}name", String::class.java).let { name ->
                if (name == null) {
                    null
                } else {
                    WalletItemEntity(
                        name = name,
                        type = WalletItemType.valueOf(row.get("${columnPrefix}type", String::class.java)!!),
                        enabled = row.get("${columnPrefix}enabled", Boolean::class.java)!!,
                        userId = row.get("${columnPrefix}user_id", UUID::class.java)!!,
                        currency = row.get("${columnPrefix}currency", String::class.java)!!,
                        totalLimit = row.get("${columnPrefix}total_limit", BigDecimal::class.java),
                        balance = row.get("${columnPrefix}balance", BigDecimal::class.java)!!,
                        dueDay = row.get("${columnPrefix}due_day", Int::class.javaObjectType),
                        daysBetweenDueAndClosing = row.get("${columnPrefix}days_between_due_and_closing", Int::class.javaObjectType),
                        dueOnNextBusinessDay = row.get("${columnPrefix}due_on_next_business_day", Boolean::class.javaObjectType),
                    ).also { gu ->
                        gu.id = row.get("${columnPrefix}id", UUID::class.java)
                    }
                }
            }
    }
}

package com.ynixt.sharedfinances.resources.repositories.r2dbc.mapping

import com.ynixt.sharedfinances.domain.entities.wallet.WalletItemEntity
import com.ynixt.sharedfinances.domain.enums.WalletItemType
import io.r2dbc.spi.Row
import java.math.BigDecimal
import java.util.UUID

class WalletItemR2DBCMapping {
    companion object {
        fun walletItemFromRow(
            row: Row,
            columnPrefix: String = "wt_",
        ): WalletItemEntity =
            WalletItemEntity(
                name = row.get("${columnPrefix}name", String::class.java)!!,
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

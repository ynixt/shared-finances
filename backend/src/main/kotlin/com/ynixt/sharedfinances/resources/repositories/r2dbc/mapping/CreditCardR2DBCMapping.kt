package com.ynixt.sharedfinances.resources.repositories.r2dbc.mapping

import com.ynixt.sharedfinances.domain.entities.wallet.CreditCard
import io.r2dbc.spi.Row
import java.math.BigDecimal
import java.util.UUID

class CreditCardR2DBCMapping {
    companion object {
        fun creditCardFromRow(
            row: Row,
            columnPrefix: String = "ca_",
        ): CreditCard =
            CreditCard(
                name = row.get("${columnPrefix}name", String::class.java)!!,
                enabled = row.get("${columnPrefix}enabled", Boolean::class.java)!!,
                userId = row.get("${columnPrefix}user_id", UUID::class.java)!!,
                currency = row.get("${columnPrefix}currency", String::class.java)!!,
                totalLimit = row.get("${columnPrefix}total_limit", BigDecimal::class.java)!!,
                availableLimit = row.get("${columnPrefix}available_limit", BigDecimal::class.java)!!,
                dueDay = row.get("${columnPrefix}due_day", Int::class.java)!!,
                daysBetweenDueAndClosing = row.get("${columnPrefix}days_between_due_and_closing", Int::class.java)!!,
                dueOnNextBusinessDay = row.get("${columnPrefix}due_on_next_business_day", Boolean::class.java)!!,
            ).also { gu ->
                gu.id = row.get("${columnPrefix}id", UUID::class.java)
            }
    }
}

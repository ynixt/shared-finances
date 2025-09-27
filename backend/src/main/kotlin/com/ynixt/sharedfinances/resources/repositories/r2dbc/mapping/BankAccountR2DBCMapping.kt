package com.ynixt.sharedfinances.resources.repositories.r2dbc.mapping

import com.ynixt.sharedfinances.domain.entities.wallet.BankAccount
import io.r2dbc.spi.Row
import java.math.BigDecimal
import java.util.UUID

class BankAccountR2DBCMapping {
    companion object {
        fun bankAccountFromRow(
            row: Row,
            columnPrefix: String = "ba_",
        ): BankAccount =
            BankAccount(
                name = row.get("${columnPrefix}name", String::class.java)!!,
                enabled = row.get("${columnPrefix}enabled", Boolean::class.java)!!,
                userId = row.get("${columnPrefix}user_id", UUID::class.java)!!,
                currency = row.get("${columnPrefix}currency", String::class.java)!!,
                balance = row.get("${columnPrefix}balance", BigDecimal::class.java)!!,
            ).also { gu ->
                gu.id = row.get("${columnPrefix}id", UUID::class.java)
            }
    }
}

package com.ynixt.sharedfinances.resources.repositories.r2dbc.mapping

import com.ynixt.sharedfinances.domain.entities.wallet.entries.WalletEntryEntity
import com.ynixt.sharedfinances.domain.enums.WalletEntryType
import io.r2dbc.spi.Row
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

class WalletEntryR2DBCMapping {
    companion object {
        fun walletEntryFromRow(
            row: Row,
            columnPrefix: String = "wt_",
        ): WalletEntryEntity =
            WalletEntryEntity(
                name = row.get("${columnPrefix}name", String::class.java),
                type = WalletEntryType.valueOf(row.get("${columnPrefix}type", String::class.java)!!),
                value = row.get("${columnPrefix}value", BigDecimal::class.java)!!,
                categoryId = row.get("${columnPrefix}category_id", UUID::class.java),
                userId = row.get("${columnPrefix}user_id", UUID::class.java),
                groupId = row.get("${columnPrefix}group_id", UUID::class.java),
                tags = row.get("${columnPrefix}tags", Array::class.java)?.map { it as String },
                observations = row.get("${columnPrefix}observations", String::class.java),
                originId = row.get("${columnPrefix}origin_id", UUID::class.java)!!,
                targetId = row.get("${columnPrefix}target_id", UUID::class.java),
                date = row.get("${columnPrefix}date", LocalDate::class.java)!!,
                confirmed = row.get("${columnPrefix}confirmed", Boolean::class.java)!!,
                installment = row.get("${columnPrefix}installment", Int::class.javaObjectType),
                recurrenceConfigId = row.get("${columnPrefix}recurrence_config_id", UUID::class.java),
                originBillId = row.get("${columnPrefix}origin_bill_id", UUID::class.java),
                targetBillId = row.get("${columnPrefix}target_bill_id", UUID::class.java),
            ).also { gu ->
                gu.id = row.get("${columnPrefix}id", UUID::class.java)
            }
    }
}

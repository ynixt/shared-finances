package com.ynixt.sharedfinances.resources.repositories.r2dbc.mapping

import com.ynixt.sharedfinances.domain.entities.wallet.entries.EntryRecurrenceConfigEntity
import com.ynixt.sharedfinances.domain.enums.PaymentType
import com.ynixt.sharedfinances.domain.enums.RecurrenceType
import com.ynixt.sharedfinances.domain.enums.WalletEntryType
import io.r2dbc.spi.Row
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

class EntryRecurrenceConfigR2DBCMapping {
    companion object {
        fun entryRecurrenceConfigFromRow(
            row: Row,
            columnPrefix: String = "entry_config_",
        ): EntryRecurrenceConfigEntity =
            EntryRecurrenceConfigEntity(
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
                lastExecution = row.get("${columnPrefix}last_execution", LocalDate::class.java),
                nextExecution = row.get("${columnPrefix}next_execution", LocalDate::class.java),
                endExecution = row.get("${columnPrefix}end_execution", LocalDate::class.java),
                qtyLimit = row.get("${columnPrefix}qty_limit", Int::class.javaObjectType),
                qtyExecuted = row.get("${columnPrefix}qty_executed", Int::class.javaObjectType)!!,
                paymentType = PaymentType.valueOf(row.get("${columnPrefix}payment_type", String::class.java)!!),
                periodicity = RecurrenceType.valueOf(row.get("${columnPrefix}periodicity", String::class.java)!!),
            ).also { gu ->
                gu.id = row.get("${columnPrefix}id", UUID::class.java)
            }
    }
}

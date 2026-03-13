package com.ynixt.sharedfinances.resources.repositories.r2dbc.mapping

import com.ynixt.sharedfinances.domain.entities.wallet.entries.RecurrenceEntryEntity
import io.r2dbc.spi.Row
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

class RecurrenceEntryR2DBCMapping {
    companion object {
        fun createSelectForRecurrenceEntry(
            tableAlias: String = "rey",
            columnPrefix: String = "rey_",
        ): String =
            """
            $tableAlias.id                              AS ${columnPrefix}id,
            $tableAlias.created_at                      AS ${columnPrefix}created_at,
            $tableAlias.updated_at                      AS ${columnPrefix}updated_at,
            $tableAlias.value                           AS ${columnPrefix}value,
            $tableAlias.wallet_event_id                 AS ${columnPrefix}wallet_event_id,
            $tableAlias.wallet_item_id                  AS ${columnPrefix}wallet_item_id,
            $tableAlias.next_bill_date                  AS ${columnPrefix}next_bill_date,
            $tableAlias.last_bill_date                  AS ${columnPrefix}last_bill_date
            """.trimIndent()

        fun recurrenceEntryFromRow(
            row: Row,
            columnPrefix: String = "recurrence_entry_",
        ): RecurrenceEntryEntity =
            RecurrenceEntryEntity(
                value = row.get("${columnPrefix}value", BigDecimal::class.java)!!,
                walletEventId = row.get("${columnPrefix}wallet_event_id", UUID::class.java)!!,
                walletItemId = row.get("${columnPrefix}wallet_item_id", UUID::class.java)!!,
                nextBillDate = row.get("${columnPrefix}next_bill_date", LocalDate::class.java),
                lastBillDate = row.get("${columnPrefix}last_bill_date", LocalDate::class.java),
            ).also { gu ->
                gu.id = row.get("${columnPrefix}id", UUID::class.java)
            }
    }
}

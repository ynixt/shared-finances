package com.ynixt.sharedfinances.resources.repositories.r2dbc.mapping

import com.ynixt.sharedfinances.domain.entities.wallet.entries.WalletEventEntity
import com.ynixt.sharedfinances.domain.enums.PaymentType
import com.ynixt.sharedfinances.domain.enums.TransferPurpose
import com.ynixt.sharedfinances.domain.enums.WalletEntryType
import io.r2dbc.spi.Row
import java.time.LocalDate
import java.util.UUID

class WalletEventR2DBCMapping {
    companion object {
        fun walletEventFromRow(
            row: Row,
            columnPrefix: String = "wt_",
        ): WalletEventEntity =
            WalletEventEntity(
                name = row.get("${columnPrefix}name", String::class.java),
                type = WalletEntryType.valueOf(row.get("${columnPrefix}type", String::class.java)!!),
                categoryId = row.get("${columnPrefix}category_id", UUID::class.java),
                createdByUserId = row.get("${columnPrefix}created_by_user_id", UUID::class.java)!!,
                groupId = row.get("${columnPrefix}group_id", UUID::class.java),
                tags = row.get("${columnPrefix}tags", Array::class.java)?.map { it as String },
                observations = row.get("${columnPrefix}observations", String::class.java),
                date = row.get("${columnPrefix}date", LocalDate::class.java)!!,
                confirmed = row.get("${columnPrefix}confirmed", Boolean::class.java)!!,
                installment = row.get("${columnPrefix}installment", Int::class.javaObjectType),
                recurrenceEventId = row.get("${columnPrefix}recurrence_event_id", UUID::class.java),
                paymentType = PaymentType.valueOf(row.get("${columnPrefix}payment_type", String::class.java)!!),
                transferPurpose =
                    TransferPurpose.valueOf(
                        row.get("${columnPrefix}transfer_purpose", String::class.java) ?: TransferPurpose.GENERAL.name,
                    ),
                initialBalance = row.get("${columnPrefix}initial_balance", Boolean::class.java) ?: false,
            ).also { gu ->
                gu.id = row.get("${columnPrefix}id", UUID::class.java)
            }
    }
}

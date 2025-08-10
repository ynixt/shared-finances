package com.ynixt.shared_finances.domain.entities.wallet.entries

import com.ynixt.shared_finances.domain.entities.AuditedEntity
import org.springframework.data.relational.core.mapping.Table
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

@Table("credit_card_bill")
class CreditCardBill(
    val creditCardId: UUID,
    val dueDate: LocalDate,
    val closingDate: LocalDate,
    val payed: Boolean,
    val value: BigDecimal,
): AuditedEntity() {
}
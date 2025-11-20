package com.ynixt.sharedfinances.domain.entities.wallet.entries

import com.ynixt.sharedfinances.domain.entities.AuditedEntity
import org.springframework.data.relational.core.mapping.Table
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

@Table("credit_card_bill")
class CreditCardBillEntity(
    val creditCardId: UUID,
    val billDate: LocalDate,
    val dueDate: LocalDate,
    val closingDate: LocalDate,
    val payed: Boolean,
    val value: BigDecimal,
) : AuditedEntity()

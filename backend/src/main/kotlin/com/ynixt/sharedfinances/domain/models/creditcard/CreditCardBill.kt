package com.ynixt.sharedfinances.domain.models.creditcard

import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

data class CreditCardBill(
    val id: UUID?,
    val creditCardId: UUID,
    val billDate: LocalDate?,
    val dueDate: LocalDate?,
    val closingDate: LocalDate?,
    val payed: Boolean,
    val value: BigDecimal,
)

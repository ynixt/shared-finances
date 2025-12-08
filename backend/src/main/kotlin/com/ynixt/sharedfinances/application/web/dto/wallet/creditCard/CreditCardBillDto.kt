package com.ynixt.sharedfinances.application.web.dto.wallet.creditCard

import com.ynixt.sharedfinances.domain.enums.CreditCardBillStatus
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

data class CreditCardBillDto(
    val id: UUID?,
    val creditCardId: UUID,
    val billDate: LocalDate,
    val dueDate: LocalDate,
    val closingDate: LocalDate,
    val startDate: LocalDate?,
    val paid: Boolean,
    val value: BigDecimal,
    val status: CreditCardBillStatus?,
)

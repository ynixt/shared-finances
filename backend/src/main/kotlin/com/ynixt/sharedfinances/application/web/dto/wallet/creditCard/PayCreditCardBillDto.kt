package com.ynixt.sharedfinances.application.web.dto.wallet.creditCard

import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

data class PayCreditCardBillDto(
    val bankAccountId: UUID,
    val date: LocalDate,
    val amount: BigDecimal,
    val observations: String? = null,
)

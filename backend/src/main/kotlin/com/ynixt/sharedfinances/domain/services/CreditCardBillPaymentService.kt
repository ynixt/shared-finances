package com.ynixt.sharedfinances.domain.services

import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

interface CreditCardBillPaymentService {
    suspend fun payBill(
        userId: UUID,
        billId: UUID,
        bankAccountId: UUID,
        date: LocalDate,
        amount: BigDecimal,
        observations: String?,
    )
}

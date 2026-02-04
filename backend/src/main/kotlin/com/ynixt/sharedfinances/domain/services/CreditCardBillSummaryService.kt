package com.ynixt.sharedfinances.domain.services

import com.ynixt.sharedfinances.domain.models.creditcard.CreditCardBill
import java.util.UUID

interface CreditCardBillSummaryService {
    suspend fun getBillForMonth(
        userId: UUID,
        creditCardId: UUID,
        month: Int,
        year: Int,
    ): CreditCardBill
}

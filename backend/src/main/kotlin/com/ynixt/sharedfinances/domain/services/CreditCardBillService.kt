package com.ynixt.sharedfinances.domain.services

import com.ynixt.sharedfinances.domain.entities.wallet.entries.CreditCardBillEntity
import reactor.core.publisher.Mono
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

interface CreditCardBillService {
    /**
     * Try to get a bill using credit card id and bill date (bill date is dueDate at start of month).
     * If not found, creates a new bill.
     */
    fun getOrCreateBill(
        creditCardId: UUID,
        dueDate: LocalDate,
        closingDate: LocalDate,
        startValue: BigDecimal = BigDecimal.ZERO,
    ): Mono<CreditCardBillEntity>
}

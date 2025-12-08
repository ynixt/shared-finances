package com.ynixt.sharedfinances.domain.models.creditcard

import com.ynixt.sharedfinances.domain.enums.CreditCardBillStatus
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

data class CreditCardBill(
    val id: UUID?,
    val creditCardId: UUID,
    val billDate: LocalDate,
    val dueDate: LocalDate,
    val closingDate: LocalDate,
    val paid: Boolean,
    val value: BigDecimal,
) {
    var startDate: LocalDate? = null
    val status: CreditCardBillStatus?
        get() {
            if (startDate == null) return null

            val today = LocalDate.now()

            if (today.isBefore(startDate)) return CreditCardBillStatus.FUTURE

            if ((today.isAfter(startDate) || today.isEqual(startDate)) &&
                today.isBefore(closingDate.plusDays(1))
            ) {
                return CreditCardBillStatus.OPEN
            }

            if (paid || value >= BigDecimal.ZERO) return CreditCardBillStatus.PAID

            if (today.isBefore(dueDate.plusDays(1))) return CreditCardBillStatus.CLOSED

            return CreditCardBillStatus.OVERDUE
        }
}

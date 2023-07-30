package com.ynixt.sharedfinances.model.dto.creditcard

import com.ynixt.sharedfinances.model.dto.creditcardbilldate.CreditCardBillDateDto
import java.math.BigDecimal
import java.time.LocalDate

data class CreditCardDto(
    val id: Long? = null,
    val userId: Long,
    val name: String,
    val closingDay: Int,
    val paymentDay: Int,
    val limit: BigDecimal,
    val availableLimit: BigDecimal?,
    val enabled: Boolean,
    val displayOnGroup: Boolean,
    val billDates: List<CreditCardBillDateDto>
) {
    val billDatesValue: List<LocalDate> = billDates.map { it.billDate }
}

package com.ynixt.sharedfinances.model.dto.creditcardbilldate

import java.time.LocalDate

data class CreditCardBillDateDto(
    val id: Long?, val billDate: LocalDate, val creditCardId: Long?
)

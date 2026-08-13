package com.ynixt.sharedfinances.domain.services.exchangerate

import java.math.BigDecimal
import java.math.MathContext

object ExchangeRateDerivation {
    fun derive(
        rateFrom: BigDecimal,
        rateTo: BigDecimal,
    ): BigDecimal = rateTo.divide(rateFrom, MathContext.DECIMAL128)
}

package com.ynixt.sharedfinances.domain.enums

import java.math.BigDecimal

enum class WalletEntryType {
    REVENUE,
    EXPENSE,
    TRANSFER,
    ;

    fun fixValue(value: BigDecimal): BigDecimal =
        if (this == TRANSFER || this == EXPENSE) {
            value.unaryMinus()
        } else {
            value.abs()
        }
}
